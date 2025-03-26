pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repo URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Dynamic feature branch name based on build number
        GIT_BRANCH = ""  // Initialize the GIT_BRANCH variable globally to avoid null issues later
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
                                echo "SSH Connection successful."  // Confirm SSH connection
                            else
                                echo "ERROR: SSH Connection failed!"  // Report SSH failure
                                exit 1  // Exit pipeline if SSH fails
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
            when {
                expression { env.BRANCH_NAME != "feature-test" }  // Skip branch creation for static test branch
            }
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"  // Print new branch name

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {  // Provide SSH agent to shell
                        sh '''
                            git checkout -b ${BRANCH_NAME}  // Create local feature branch
                            git push origin ${BRANCH_NAME}  // Push feature branch to GitHub
                        '''
                    }

                    env.GIT_BRANCH = BRANCH_NAME  // Save branch name in environment variable for later use
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  // Exit if any command fails
                    echo "Setting up Python virtual environment."  // Log setup
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  // Create virtual environment if it does not exist
                    . venv/bin/activate  // Activate the virtual environment
                    venv/bin/python -m pip install --upgrade pip  // Upgrade pip to latest version
                    venv/bin/python -m pip install flask requests pytest gunicorn  // Install required Python packages
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  // Exit on error
                    echo "Starting Flask app for testing..."  // Log Flask app start
                    . venv/bin/activate  // Activate virtual environment
                    sleep 3  // Wait for server to start
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  // Run Flask app in background
                    echo "Running API tests."  // Log test start
                    venv/bin/python -m pytest test_app.py  // Run Python tests using pytest
                '''
            }
        }
    }

    post {
        success {
            script {
                echo "Build and tests passed successfully."  // Log success message

                // Use fallback to BRANCH_NAME if GIT_BRANCH not set explicitly
                def branch = env.GIT_BRANCH?.trim() ? env.GIT_BRANCH : env.BRANCH_NAME

                def mergeSuccess = false  // Initialize merge status
                def deploySuccess = false  // Initialize deploy status
                def slack = load 'slack_notifications.groovy'  // Load external Slack helper script

                if (branch.startsWith("feature-")) {  // Proceed only if it's a feature branch
                    echo "Merging ${branch} into main..."  // Log start of merge

                    try {
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {  // Provide SSH agent to shell
                            sh '''
                                git config user.name "jenkins"  // Set Git username for commit
                                git config user.email "jenkins@example.com"  // Set Git email
                                git checkout main  // Switch to main branch
                                git pull origin main  // Pull latest changes
                                git merge --no-ff ${GIT_BRANCH}  // Merge feature branch into main
                                git push origin main  // Push merged changes to GitHub
                            '''
                        }
                        echo "Merge completed successfully."  // Log successful merge
                        mergeSuccess = true  // Mark merge as successful
                    } catch (Exception mergeError) {
                        echo "Merge failed: ${mergeError.message}"  // Log merge failure
                    }

                    if (mergeSuccess) {  // Proceed to deploy only if merge succeeded
                        try {
                            echo "Starting deployment after merge..."  // Log deploy start
                            sh '''
                                chmod +x deploy.sh  // Make deploy script executable
                                ./deploy.sh  // Run the deploy script
                            '''
                            echo "Deployment script executed successfully."  // Log deploy success
                            deploySuccess = true  // Mark deployment as successful
                        } catch (Exception deployError) {
                            echo "Deployment failed: ${deployError.message}"  // Log deploy failure
                        }
                    }
                }

                // Send Slack message summarizing result
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, mergeSuccess, deploySuccess)
                def statusColor = (mergeSuccess && deploySuccess) ? "good" : "danger"
                slack.sendSlackNotification(message, statusColor)
            }
        }

        failure {
            script {
                echo "Build or tests failed."  // Log general build/test failure

                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)  // Send failure message (no merge, no deploy)
                    slack.sendSlackNotification(message, "danger")  // Slack color red
                } catch (Exception e) {
                    echo "Error sending Slack failure message: ${e.message}"  // Log Slack error
                }
            }
        }

        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build message without merge/deploy status
                    slack.sendSlackNotification(message, "good")  // Send Slack message
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"  // Log Slack error
                }
            }
        }
    }  // Close post block
}  // Close pipeline block

