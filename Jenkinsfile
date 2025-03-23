pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Define the GitHub repository via SSH
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Generate dynamic feature branch name using the build number
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Use Jenkins SSH credentials to access GitHub
                    script {
                        echo "Starting SSH Agent and verifying authentication."  // Log for SSH startup
                        sh "ssh-add -l"  // List the SSH keys that were loaded
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH Connection successful."  # Log on successful authentication
                            else
                                echo "ERROR: SSH Connection failed!"  # Log on authentication failure
                                exit 1  # Stop pipeline on SSH failure
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository."  // Log before checkout
                    checkout([
                        $class: 'GitSCM',  // Use Git as the SCM tool
                        branches: [[name: '*/main']],  // Checkout the main branch
                        userRemoteConfigs: [[
                            url: REPO_URL,  // Repository URL to checkout from
                            credentialsId: 'Jenkins-GitHub-SSH'  // Use the SSH credentials to authenticate
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"  // Log the branch name creation

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {  // Pass SSH agent socket for Git use
                        sh '''
                            git checkout -b ${BRANCH_NAME}  # Create the new feature branch
                            git push origin ${BRANCH_NAME}  # Push the new branch to GitHub
                        '''
                    }

                    env.GIT_BRANCH = BRANCH_NAME  // Store branch name in environment for later use
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit script on first error
                    echo "Setting up Python virtual environment."  # Log setup start
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if not present
                    . venv/bin/activate  # Activate the virtual environment
                    venv/bin/python -m pip install --upgrade pip  # Upgrade pip inside venv
                    venv/bin/python -m pip install flask requests pytest gunicorn  # Install required packages
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Exit on failure
                    echo "Starting Flask app for testing..."  # Log before running app
                    . venv/bin/activate  # Activate the Python environment
                    sleep 3  # Wait to ensure the app starts
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Run the app in background
                    echo "Running API tests."  # Log before test execution
                    venv/bin/python -m pytest test_app.py  # Run tests using pytest
                '''
            }
        }
    }

    post {
        success {
            script {
                echo "Build and tests passed successfully."  // General success log

                if (env.GIT_BRANCH?.startsWith("feature-")) {  // Only merge if branch is a feature branch
                    echo "Merging ${env.GIT_BRANCH} into main..."  // Log the merge initiation

                    def mergeSuccess = true  // Boolean to track merge success
                    def deploySuccess = true  // Boolean to track deployment success
                    def slack = load 'slack_notifications.groovy'  // Load external Slack handler script

                    try {
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {  // Use SSH agent for Git operations
                            sh '''
                                git config user.name "jenkins"  # Set Git user name
                                git config user.email "jenkins@example.com"  # Set Git user email
                                git checkout main  # Switch to main branch
                                git pull origin main  # Pull latest changes
                                git merge --no-ff ${GIT_BRANCH}  # Merge feature branch into main
                                git push origin main  # Push merged changes to GitHub
                            '''
                        }
                        echo "Merge completed successfully."  // Log successful merge
                    } catch (Exception mergeError) {
                        echo "Merge failed: ${mergeError.message}"  // Log merge failure message
                        mergeSuccess = false  // Update flag to false
                    }

                    if (mergeSuccess) {  // Only deploy if merge was successful
                        try {
                            echo "Starting deployment after merge..."  // Log before deployment
                            sh '''
                                chmod +x deploy.sh  # Make sure deploy script is executable
                                ./deploy.sh  # Run deployment script
                            '''
                            echo "Deployment script executed successfully."  // Log success
                        } catch (Exception deployError) {
                            echo "Deployment failed: ${deployError.message}"  // Log deployment error
                            deploySuccess = false  // Mark deploy failure
                        }
                    }

                    def message = slack.constructSlackResultMessage(env.BUILD_NUMBER, env.BUILD_URL, mergeSuccess, deploySuccess)  // Build full Slack message
                    def statusColor = (mergeSuccess && deploySuccess) ? "good" : "danger"  // Set color based on status
                    slack.sendSlackNotification(message, statusColor)  // Send the Slack notification
                }
            }
        }

        failure {
            script {
                echo "Build or tests failed."  // General failure log
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper script
                    def message = slack.constructSlackResultMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)  // Build failure message
                    slack.sendSlackNotification(message, "danger")  // Notify Slack on failure
                } catch (Exception e) {
                    echo "Error sending Slack failure message: ${e.message}"  // Handle Slack send error
                }
            }
        }

        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack notification utility
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build standard message
                    slack.sendSlackNotification(message, "good")  // Send notification regardless of result
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"  // Catch and log error
                }
            }
        }
    }
}

