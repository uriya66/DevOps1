pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent concurrent builds of the same pipeline
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH URL of GitHub repo
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Name of temporary feature branch for this build
    }

    stages {

        stage('Detect Real Branch') {
            steps {
                script {
                    // Get the original Git branch from the Git plugin
                    env.GIT_BRANCH = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                    echo "DEBUG: Real GIT_BRANCH = ${env.GIT_BRANCH}"
                }
            }
        }

        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    // Prevent infinite build loops from merge commits
                    def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    if (lastCommitMessage.startsWith("Merge remote-tracking branch")) {
                        echo "Skipping build: Merge commit detected"
                        currentBuild.result = 'SUCCESS'
                        error("Stopping Pipeline")
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Authenticating SSH connection to GitHub"
                        sh 'ssh-add -l'   # List loaded SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection successful"
                            else
                                echo "ERROR: SSH authentication failed"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],  // Always base the build off main
                    userRemoteConfigs: [[
                        url: REPO_URL,
                        credentialsId: 'Jenkins-GitHub-SSH'
                    ]]
                ])
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating feature branch ${BRANCH_NAME} from main"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create the feature branch
                            git push origin ${BRANCH_NAME}  # Push to remote
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Update env var for later use
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Exit if any command fails
                    echo "Setting up Python virtual environment"
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if not exists
                    . venv/bin/activate  # Activate venv
                    venv/bin/python -m pip install --upgrade pip  # Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn  # Install dependencies
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Starting Flask app for testing..."
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3  # Wait for server to start
                    echo "Running tests..."
                    venv/bin/python -m pytest test_app.py
                    pkill gunicorn || true  # Clean up
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    echo "Deploying app..."
                    env.DEPLOY_SUCCESS = "false"  // Default to failure
                    try {
                        sh '''
                            set -e
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        env.DEPLOY_SUCCESS = "true"  // Only if no error thrown
                        echo "DEBUG: DEPLOY_SUCCESS = ${env.DEPLOY_SUCCESS}"
                    } catch (Exception e) {
                        echo "Deployment failed: ${e.message}"
                        error("Stopping pipeline due to deployment failure")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    // Merge only if this is a feature branch and deployment was successful
                    return env.GIT_BRANCH?.startsWith('feature-') && env.DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                script {
                    echo "Merging ${env.GIT_BRANCH} into main"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git config user.name "jenkins"
                            git config user.email "jenkins@example.com"
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${env.GIT_BRANCH}
                            git push origin main
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper script
                    def msg = slack.constructSlackMessage(
                        env.BUILD_NUMBER,
                        env.BUILD_URL,
                        env.GIT_BRANCH,
                        env.DEPLOY_SUCCESS == 'true'
                    )
                    slack.sendSlackNotification(msg, (env.DEPLOY_SUCCESS == 'true') ? "good" : "danger")
                } catch (Exception e) {
                    echo "Slack notification failed: ${e.message}"
                }
            }
        }
    }  // Close post block
}  // Close pipeline block
