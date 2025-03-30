pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repository URL
        BASE_BRANCH = ''  // Will hold the original triggering branch (main or feature-test)
        GIT_BRANCH = ''   // Will hold the generated feature-${BUILD_NUMBER} branch name
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication"  // Start SSH agent and verify
                        sh 'ssh-add -l'  // List loaded keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection successful"  # Confirm SSH connection
                            else
                                echo "ERROR: SSH connection failed!"  # Report SSH failure
                                exit 1  # Fail the build
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Detecting triggering branch using Git log"  // Log start of detection
                    BASE_BRANCH = sh(script: "git log -1 --pretty=format:%D | grep -oE 'origin/(main|feature-test)' | cut -d/ -f2", returnStdout: true).trim()  // Detect source branch
                    if (!BASE_BRANCH) {
                        error("❌ Could not detect triggering branch")  // Fail if detection fails
                    }
                    echo "Trigger branch is: ${BASE_BRANCH}"  // Log detected branch

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${BASE_BRANCH}"]],
                        userRemoteConfigs: [[
                            url: REPO_URL,
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    def branch = "feature-${env.BUILD_NUMBER}"  // Create local variable for new branch
                    env.GIT_BRANCH = branch  // Save to global env
                    echo "Creating feature branch: ${env.GIT_BRANCH} from ${BASE_BRANCH}"  // Log branch creation
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${env.GIT_BRANCH}  # Create new branch
                            git push origin ${env.GIT_BRANCH}  # Push to GitHub
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    echo "Setting up Python virtual environment..."  # Log venv creation
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    venv/bin/pip install --upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Running tests using Gunicorn..."  # Log testing
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    echo "Executing API tests..."
                    venv/bin/pytest test_app.py
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    sh '''
                        set -e
                        echo "Running deployment script..."  # Log deploy
                        chmod +x deploy.sh
                        ./deploy.sh
                    '''
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith('feature-')  // Merge only if branch is feature-*
                }
            }
            steps {
                script {
                    def branchForMerge = env.GIT_BRANCH
                    echo "Current branch for merge verification: ${branchForMerge}"  // Debug current feature branch
                    echo "Attempting to merge ${branchForMerge} into main..."  // Debug attempt
                    try {
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh """
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main
                                git pull origin main
                                git merge --no-ff ${branchForMerge}
                                git push origin main
                            """
                            echo "✅ Merge to main completed successfully."
                        }
                    } catch (err) {
                        echo "❌ Merge to main failed: ${err.getMessage()}"
                        error("Merge failed")
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                def slack = load 'slack_notifications.groovy'
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, true, true)
                slack.sendSlackNotification(message, "good")
            }
        }
        failure {
            script {
                def slack = load 'slack_notifications.groovy'
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)
                slack.sendSlackNotification(message, "danger")
            }
        }
    }
}

