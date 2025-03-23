pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repo URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Dynamic feature branch name based on build number
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Use SSH credentials for GitHub
                    script {
                        echo "Starting SSH Agent and verifying authentication."  // Print start message
                        sh "ssh-add -l"  // List loaded SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH Connection successful."  # Confirm SSH connection
                            else
                                echo "ERROR: SSH Connection failed!"  # Report SSH failure
                                exit 1  # Exit pipeline if SSH fails
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository."  // Log checkout start
                    checkout([
                        $class: 'GitSCM',  // Use Git plugin
                        branches: [[name: '*/main']],  // Checkout main branch
                        userRemoteConfigs: [[
                            url: REPO_URL,  // Use the GitHub SSH repo URL
                            credentialsId: 'Jenkins-GitHub-SSH'  // Use Jenkins credentials ID
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"  // Print new branch name

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {  // Provide SSH agent to shell
                        sh '''
                            git checkout -b ${BRANCH_NAME}  # Create local feature branch
                            git push origin ${BRANCH_NAME}  # Push feature branch to GitHub
                        '''
                    }

                    env.GIT_BRANCH = BRANCH_NAME  // Save branch name in environment variable for later use
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit if any command fails
                    echo "Setting up Python virtual environment."  # Log setup
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create virtual environment if it does not exist
                    . venv/bin/activate  # Activate the virtual environment
                    venv/bin/python -m pip install --upgrade pip  # Upgrade pip to latest version
                    venv/bin/python -m pip install flask requests pytest gunicorn  # Install required Python packages
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Exit on error
                    echo "Starting Flask app for testing..."  # Log Flask app start
                    . venv/bin/activate  # Activate virtual environment
                    sleep 3  # Wait for server to start
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Run Flask app in background
                    echo "Running API tests."  # Log test start
                    venv/bin/python -m pytest test_app.py  # Run Python tests using pytest
                '''
            }
        }
    }

    post {
        success {
            script {
                echo "Build and tests passed successfully."  // Log success message after successful stages

                if (env.GIT_BRANCH?.startsWith("feature-")) {  // Check if the current branch is a feature branch
                    echo "Merging ${env.GIT_BRANCH} into main..."  // Log start of merge step

                    def mergeSuccess = true  // Variable to store merge result
                    def deploySuccess = true  // Variable to store deploy result
                    def slack = load 'slack_notifications.groovy'  // Load shared Slack utility script

                    try {
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {  // Provide SSH agent environment to shell
                            sh '''
                                git config user.name "jenkins"  # Set Git username for merge
                                git config user.email "jenkins@example.com"  # Set Git email for merge
                                git checkout main  # Switch to main branch
                                git pull origin main  # Get latest code from origin
                                git merge --no-ff ${GIT_BRANCH}  # Merge feature branch without fast-forward
                                git push origin main  # Push merged changes to origin
                            '''
                        }
                        echo "Merge completed successfully."  // Log success of merge
                    } catch (Exception mergeError) {
                        echo "Merge failed: ${mergeError.message}"  // Log merge error
                        mergeSuccess = false  // Mark merge as failed
                    }

                    if (mergeSuccess) {  // Proceed to deploy only if merge succeeded
                        try {
                            echo "Starting deployment after merge..."  // Log start of deploy
                            sh '''
                                chmod +x deploy.sh  # Ensure the deploy script is executable
                                ./deploy.sh  # Execute the deployment script
                            '''
                            echo "Deployment script executed successfully."  // Log successful deployment
                        } catch (Exception deployError) {
                            echo "Deployment failed: ${deployError.message}"  // Log deployment error message
                            deploySuccess = false  // Mark deploy as failed
                        }
                    }

                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, mergeSuccess, deploySuccess)  // Build Slack message with status flags
                    def statusColor = (mergeSuccess && deploySuccess) ? "good" : "danger"  // Determine Slack message color
                    slack.sendSlackNotification(message, statusColor)  // Send detailed Slack message
                }
            }
        }
        failure {
            script {
                echo "Build or tests failed."  // Log general failure message
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack utility script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)  // Build failure message
                    slack.sendSlackNotification(message, "danger")  // Send Slack alert for failure
                } catch (Exception e) {
                    echo "Error sending Slack failure message: ${e.message}"  // Log Slack failure
                }
            }
        }
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack utility script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build generic Slack message (no merge/deploy info)
                    slack.sendSlackNotification(message, "good")  // Send message if not already sent
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"  // Log error during Slack send
                }
            }
        }
    }

