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
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"  // Print new branch name

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {  // Provide SSH agent to shell
                        sh '''
                            git checkout -b ${BRANCH_NAME}  # Create local feature branch
                            git push origin ${BRANCH_NAME}  # Push feature branch to GitHub
                        '''
                    }

                    env.GIT_BRANCH = BRANCH_NAME  // Save branch name in env for later use
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit if any command fails
                    echo "Setting up Python virtual environment."  # Log setup
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if not exists
                    . venv/bin/activate  # Activate the virtual environment
                    venv/bin/python -m pip install --upgrade pip  # Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn  # Install required packages
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
                    venv/bin/python -m pytest test_app.py  # Run tests
                '''
            }
        }
    }

    post {
        success {
            script {
                echo "Build and tests passed successfully."  // Log success

                if (env.GIT_BRANCH?.startsWith("feature-")) {  // Check if it's a feature branch
                    echo "Merging ${env.GIT_BRANCH} into main..."  // Log merge step

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {  // Provide SSH environment
                        sh '''
                            git config user.name "jenkins"  # Set Git username
                            git config user.email "jenkins@example.com"  # Set Git email
                            git checkout main  # Switch to main branch
                            git pull origin main  # Pull latest changes
                            git merge --no-ff ${GIT_BRANCH}  # Merge feature branch
                            git push origin main  # Push merged changes
                        '''
                    }

                    echo "Starting deployment after merge..."  // Log deployment step
                    sh '''
                        chmod +x deploy.sh  # Make deploy script executable
                        ./deploy.sh  # Run deployment script

                        if [ $? -eq 0 ]; then
                            echo "Deployment script executed successfully."  # Success log
                        else
                            echo "Deployment script failed."  # Failure log
                            exit 1  # Exit with error
                        fi
                    '''

                    def slack = load 'slack_notifications.groovy'  // Load Slack notification utility
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Create full Slack message
                    slack.sendSlackNotification(message, "good")  // Send formatted Slack message
                }
            }
        }
        failure {
            script {
                echo "Build or tests failed."  // Log failure
            }
        }
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Create message
                    slack.sendSlackNotification(message, "good")  // Send message
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"  // Log Slack error
                }
            }
        }
    }
}
