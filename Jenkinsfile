pipeline {
    agent any  // Use any available Jenkins agent to run the pipeline

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running at the same time
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Define the SSH GitHub repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Define dynamic feature branch name using the build number
        GIT_BRANCH = ""  // Initialize GIT_BRANCH to avoid null issues later
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Start SSH agent with GitHub SSH credentials
                    script {
                        echo "Starting SSH Agent and verifying authentication."  // Output start message
                        sh "ssh-add -l"  // List currently loaded SSH keys
                        sh '''
                            # Test SSH connection to GitHub
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH Connection successful."  # Confirm SSH connection
                            else
                                echo "ERROR: SSH Connection failed!"  # Output error if SSH fails
                                exit 1  # Exit the pipeline if authentication fails
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository."  // Output repository checkout message
                    checkout([
                        $class: 'GitSCM',  // Use GitSCM plugin
                        branches: [[name: '*/main']],  // Checkout from the main branch
                        userRemoteConfigs: [[
                            url: REPO_URL,  // Set the GitHub repository URL
                            credentialsId: 'Jenkins-GitHub-SSH'  // Use SSH credentials ID
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            when {
                expression { env.BRANCH_NAME != "feature-test" }  // Only create a new branch if it's not 'feature-test'
            }
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"  // Output the new branch name

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {  // Pass SSH agent to shell
                        sh '''
                            # Initialize Git and setup origin manually
                            git init  # Initialize a new Git repository (fix for detached HEAD)
                            git remote add origin ${REPO_URL}  # Add remote origin
                            git fetch origin  # Fetch all remote branches
                            git checkout -b ${BRANCH_NAME} origin/main  # Create new branch from main
                            git push origin ${BRANCH_NAME}  # Push new branch to GitHub
                        '''
                    }

                    env.GIT_BRANCH = BRANCH_NAME  // Save the new branch name into environment variable
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit immediately if a command fails
                    echo "Setting up Python virtual environment."  # Log setup step
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create virtualenv if not exists
                    . venv/bin/activate  # Activate the virtual environment
                    venv/bin/python -m pip install --upgrade pip  # Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn  # Install Python dependencies
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Exit immediately if any command fails
                    echo "Starting Flask app for testing..."  # Output message for app start
                    . venv/bin/activate  # Activate Python virtual environment
                    sleep 3  # Wait to ensure app is ready
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start the Flask app using Gunicorn
                    echo "Running API tests."  # Output message for test start
                    venv/bin/python -m pytest test_app.py  # Run API tests using pytest
                '''
            }
        }
    }

    post {
        success {
            script {
                echo "Build and tests passed successfully."  // Log success message

                def branch = env.GIT_BRANCH?.trim() ? env.GIT_BRANCH : env.BRANCH_NAME  // Fallback to BRANCH_NAME if needed
                def mergeSuccess = false  // Track merge status
                def deploySuccess = false  // Track deploy status
                def slack = load 'slack_notifications.groovy'  // Load external Slack helper

                if (branch.startsWith("feature-")) {  // Only auto-merge feature branches
                    try {
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh '''
                                # Perform git merge
                                git config user.name "jenkins"  # Set Git username
                                git config user.email "jenkins@example.com"  # Set Git email
                                git checkout main  # Switch to main branch
                                git pull origin main  # Sync with remote
                                git merge --no-ff ${GIT_BRANCH}  # Merge feature branch
                                git push origin main  # Push to remote main
                            '''
                        }
                        echo "Merge completed successfully."  // Log merge result
                        mergeSuccess = true  // Mark success
                    } catch (Exception mergeError) {
                        echo "Merge failed: ${mergeError.message}"  // Log error
                    }

                    if (mergeSuccess) {
                        try {
                            echo "Starting deployment after merge..."  // Log deploy start
                            sh '''
                                chmod +x deploy.sh  # Make deploy script executable
                                ./deploy.sh  # Run deployment script
                            '''
                            echo "Deployment script executed successfully."  // Log deploy result
                            deploySuccess = true  // Mark success
                        } catch (Exception deployError) {
                            echo "Deployment failed: ${deployError.message}"  // Log error
                        }
                    }
                }

                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, mergeSuccess, deploySuccess)  // Construct Slack message
                def statusColor = (mergeSuccess && deploySuccess) ? "good" : "danger"  // Choose Slack color
                slack.sendSlackNotification(message, statusColor)  // Send the message
            }
        }

        failure {
            script {
                echo "Build or tests failed."  // Log failure message

                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)  // Create failure message
                    slack.sendSlackNotification(message, "danger")  // Send Slack message with red color
                } catch (Exception e) {
                    echo "Error sending Slack failure message: ${e.message}"  // Handle Slack error
                }
            }
        }

        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Create basic Slack message
                    slack.sendSlackNotification(message, "good")  // Send final notification
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"  // Handle notification error
                }
            }
        }
    }  // Close post block
}  // Close pipeline block

