pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent concurrent builds for the same pipeline
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH Git repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Feature branch name based on current build number
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Use configured SSH credentials for GitHub
                    script {
                        echo "Starting SSH Agent and verifying authentication."  // Debug message
                        sh "ssh-add -l"  // Show available SSH keys loaded
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH Connection successful."  // Confirm GitHub SSH connection
                            else
                                echo "ERROR: SSH Connection failed!"  // SSH connection failed
                                exit 1  // Terminate pipeline
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository."  // Log checkout step
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],  // Always checkout main
                        userRemoteConfigs: [[
                            url: REPO_URL,  // Use SSH URL
                            credentialsId: 'Jenkins-GitHub-SSH'  // Use SSH credentials
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"  // Log branch creation
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            git checkout -b ${BRANCH_NAME}  // Create new local feature branch
                            git push origin ${BRANCH_NAME}  // Push feature branch to GitHub
                        '''
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Save current feature branch to env variable
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  // Fail immediately on error
                    echo "Setting up Python virtual environment."  // Log environment setup
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  // Create venv if not exists
                    . venv/bin/activate  // Activate venv
                    venv/bin/python -m pip install --upgrade pip  // Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn  // Install packages
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  // Exit on error
                    echo "Starting Flask app for testing..."  // Log app startup
                    . venv/bin/activate  // Activate virtual environment
                    sleep 3  // Let Flask settle
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  // Start Flask with Gunicorn in background
                    echo "Running API tests."  // Log test start
                    venv/bin/python -m pytest test_app.py  // Execute tests
                '''
            }
        }
    }

    post {
        success {
            script {
                echo "Build and tests passed successfully."  // Success message
                if (env.GIT_BRANCH?.startsWith("feature-")) {  // Ensure we only merge feature branches
                    def mergeSuccess = true  // Track merge status
                    def deploySuccess = true  // Track deploy status
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper script

                    try {
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh '''
                                git config user.name "jenkins"  // Set Git username
                                git config user.email "jenkins@example.com"  // Set Git email
                                git checkout main  // Checkout main branch
                                git pull origin main  // Update local main
                                git merge --no-ff ${GIT_BRANCH}  // Merge feature branch
                                git push origin main  // Push merge to GitHub
                            '''
                        }
                        echo "Merge completed successfully."  // Log merge result
                    } catch (Exception mergeError) {
                        echo "Merge failed: ${mergeError.message}"  // Log error
                        mergeSuccess = false
                    }

                    if (mergeSuccess) {
                        try {
                            echo "Starting deployment after merge..."  // Log deployment step
                            sh '''
                                chmod +x deploy.sh  // Make script executable
                                ./deploy.sh  // Run deploy script
                            '''
                            echo "Deployment script executed successfully."  // Log success
                        } catch (Exception deployError) {
                            echo "Deployment failed: ${deployError.message}"  // Log error
                            deploySuccess = false
                        }
                    }

                    try {
                        def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, mergeSuccess, deploySuccess)  // Compose Slack message
                        def statusColor = (mergeSuccess && deploySuccess) ? "good" : "danger"  // Set color for Slack
                        slack.sendSlackNotification(message, statusColor)  // Send Slack message
                    } catch (Exception e) {
                        echo "Slack notification failed: ${e.message}"  // Log Slack error
                    }
                }
            }
        }

        failure {
            script {
                echo "Build or tests failed."  // General failure message
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)  // Compose Slack message with failure context
                    slack.sendSlackNotification(message, "danger")  // Send Slack alert
                } catch (Exception e) {
                    echo "Error sending Slack failure message: ${e.message}"  // Handle Slack failure
                }
            }
        }
    }
}

