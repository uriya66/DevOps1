
pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent concurrent runs
        skipDefaultCheckout(true)  // Skip default SCM checkout
    }

    triggers {
        pollSCM('* * * * *')  // Poll Git every minute
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH repository URL
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Use SSH credentials
                    script {
                        echo "Authenticating GitHub with SSH"  // Log for debug
                        sh "ssh-add -l"  // List loaded SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection OK"
                            else
                                echo "SSH connection failed!"
                                exit 1
                            fi
                        '''  // Verify SSH authentication with GitHub
                    }
                }
            }
        }

        stage('Detect Branch') {
            steps {
                script {
                    env.GIT_BRANCH = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()  // Get triggering branch
                    env.BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Define new feature branch
                    echo "Detected triggering branch: ${env.GIT_BRANCH}"  // Log branch
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Cloning from branch: ${env.GIT_BRANCH}"  // Log current branch
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.GIT_BRANCH}"]],
                        userRemoteConfigs: [[
                            url: REPO_URL,
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])  // Checkout the triggering branch
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating new branch: ${env.BRANCH_NAME}"  // Log
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${env.BRANCH_NAME}
                            git push origin ${env.BRANCH_NAME}
                        """  // Create and push the feature branch
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit on error
                    echo "Preparing virtual environment"
                    python3 -m venv venv  # Create virtualenv
                    . venv/bin/activate  # Activate venv
                    venv/bin/pip install --upgrade pip  # Upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn  # Install dependencies
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Stop if any command fails
                    echo "Running unit tests"  # Log test execution
                    . venv/bin/activate  # Activate venv
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start Gunicorn app
                    sleep 3  # Wait for app to initialize
                    venv/bin/python -m pytest test_app.py  # Run tests
                    pkill gunicorn  # Kill Gunicorn process
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack library
                    try {
                        sh "bash deploy.sh"  // Deploy application
                        slack.sendSlackNotification(slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL), "good")  // Notify success
                    } catch (err) {
                        slack.sendSlackNotification("*❌ Deployment Failed*\n> ${err.getMessage()}", "danger")  // Notify error
                        error("Deployment failed!")  // Stop pipeline
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                allOf {
                    expression { env.GIT_BRANCH.startsWith("feature-") }  // Merge only feature branches
                    expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }  // Only on successful build
                }
            }
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack
                    try {
                        echo "Merging ${env.BRANCH_NAME} to main"  // Log merge
                        sh '''
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${BRANCH_NAME}
                            git push origin main
                        '''  // Perform merge
                        slack.sendSlackNotification(slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL), "good")  // Notify success
                    } catch (err) {
                        slack.sendSlackNotification("*❌ Merge Failed*\n> ${err.getMessage()}", "danger")  // Notify error
                        error("Merge to main failed!")  // Stop pipeline
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Create message
                    slack.sendSlackNotification(message, "good")  // Send final notification
                } catch (Exception e) {
                    echo "Slack summary failed: ${e.message}"  // Log error
                }
            }
        }
    }  // Close post block
}  // Close pipeline block
