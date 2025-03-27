pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent parallel builds
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Git repository (SSH)
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // New feature branch name per build
        GIT_BRANCH = ""  // Real source branch name (will be set dynamically)
        DEPLOY_SUCCESS = 'false'  // Will hold deployment result
    }

    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm  // Checkout the Jenkinsfile commit
            }
        }

        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def msg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    if (msg.startsWith("Merge remote-tracking branch")) {
                        echo "Skipping merge commit build"
                        currentBuild.result = 'SUCCESS'
                        error("Build skipped: Merge commit detected")
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Authenticating SSH connection to GitHub"
                        sh 'ssh-add -l'
                        sh '''
                            # Verify GitHub SSH access
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
                    branches: [[name: '*/main']],
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
                    // Detect the real source branch (based on GitHub push)
                    def sourceBranch = sh(script: "git rev-parse --abbrev-ref origin/HEAD | sed 's|origin/||'", returnStdout: true).trim()
                    if (!sourceBranch) {
                        sourceBranch = sh(script: "git branch --show-current", returnStdout: true).trim()
                    }
                    echo "DEBUG: Source branch is ${sourceBranch}"
                    env.GIT_BRANCH = BRANCH_NAME

                    echo "Creating branch ${BRANCH_NAME} from ${sourceBranch}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout ${sourceBranch}
                            git pull origin ${sourceBranch}
                            git checkout -b ${BRANCH_NAME}
                            git push origin ${BRANCH_NAME}
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    # Set up Python virtual environment
                    set -e
                    echo "Setting up Python virtual environment"
                    if [ ! -d venv ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    # Run Flask app and execute tests
                    set -e
                    echo "Starting Flask app for testing..."
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    echo "Running tests..."
                    venv/bin/python -m pytest test_app.py
                    pkill gunicorn || true
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        echo "Deploying app..."
                        sh '''
                            # Run deploy script
                            set -e
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        echo "DEBUG: DEPLOY_SUCCESS = true"
                        env.DEPLOY_SUCCESS = 'true'
                    } catch (Exception e) {
                        echo "Deployment failed: ${e.message}"
                        env.DEPLOY_SUCCESS = 'false'
                        error("Deployment failed")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith("feature-") && env.DEPLOY_SUCCESS == 'true'
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
                def slack = load 'slack_notifications.groovy'

                // Determine if merge was successful based on logs
                def logs = currentBuild.rawBuild.getLog()
                def merged = logs.any { it.contains("git push origin main") }

                def msg = slack.constructSlackMessage(
                    env.BUILD_NUMBER,
                    env.BUILD_URL,
                    merged,
                    env.DEPLOY_SUCCESS == 'true'
                )

                slack.sendSlackNotification(
                    msg,
                    (merged && env.DEPLOY_SUCCESS == 'true') ? "good" : "danger"
                )
            }
        }
    }  // Close post block
}  // Close pipeline block
