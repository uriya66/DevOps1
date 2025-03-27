pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repo URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Dynamic feature branch name
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Start SSH agent with GitHub credentials
                    script {
                        echo "Starting SSH Agent and verifying authentication"  // Log SSH check
                        sh "ssh-add -l"  // List loaded keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection successful"  # Confirm SSH success
                            else
                                echo "SSH connection failed"  # Log failure
                                exit 1  # Exit on failure
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the triggering branch"  // Log checkout start
                    checkout([$class: 'GitSCM',
                        branches: [[name: "*/${env.BRANCH_NAME}"]],
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
                    echo "Creating new branch from source trigger: ${BRANCH_NAME}"  // Log branch creation
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            git checkout -b ${BRANCH_NAME}  # Create new feature branch
                            git push origin ${BRANCH_NAME}  # Push it to remote
                        '''
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Save branch name for later use
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit on error
                    echo "Preparing virtual environment"  # Log venv setup
                    python3 -m venv venv  # Create virtual env
                    . venv/bin/activate  # Activate it
                    venv/bin/pip install --upgrade pip  # Upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn  # Install deps
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Exit on failure
                    echo "Running unit tests"  # Log start
                    . venv/bin/activate  # Activate venv
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start app
                    sleep 3  # Wait for app to be ready
                    venv/bin/python -m pytest test_app.py  # Run tests
                    pkill gunicorn  # Stop app
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    echo "Running deploy.sh script..."  # Log deploy
                    chmod +x deploy.sh  # Make script executable
                    ./deploy.sh  # Run deploy
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith("feature-")  // Only merge if branch is feature-*
                }
            }
            steps {
                script {
                    echo "Merging ${env.GIT_BRANCH} to main"  // Log merge
                    try {
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh '''
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main  # Switch to main
                                git pull origin main  # Sync latest
                                git merge --no-ff ${GIT_BRANCH}  # Merge feature
                                git push origin main  # Push update
                            '''
                        }
                        echo "Merge completed successfully"  // Log success
                    } catch (Exception e) {
                        error("Merge failed: ${e.message}")  // Fail pipeline if merge fails
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack module
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, true, true)  // Build success message
                    slack.sendSlackNotification(message, "good")  // Send with green status
                } catch (Exception e) {
                    echo "Slack success notification failed: ${e.message}"  // Log failure
                }
            }
        }

        failure {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)
                    slack.sendSlackNotification(message, "danger")  // Send red Slack message
                } catch (Exception e) {
                    echo "Slack failure notification failed: ${e.message}"
                }
            }
        }
    }  // Close post block
}  // Close pipeline block
