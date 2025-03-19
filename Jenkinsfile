pipeline {
    agent any // Run on any available Jenkins agent

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git' // Use SSH URL for secure authentication
        # CREDENTIALS_ID = 'Jenkins-GitHub-Token' // The Jenkins credential ID for SSH authentication
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository..." // Log message
                    checkout scm // Checkout the source code from SCM

                    // Get the current branch name dynamically
                    def currentBranch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                    env.GIT_BRANCH = currentBranch // Store current branch in an environment variable
                    echo "Current branch: ${env.GIT_BRANCH}" // Log the current branch
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    // Generate a new feature branch name using Jenkins build number
                    def newBranch = "feature-${env.BUILD_NUMBER}"
                    echo "Creating a new feature branch: ${newBranch}" // Log message

                    // Create and push the new branch to the remote repository using SSH
                    sh """
                        git checkout -b ${newBranch}  # Create a new feature branch
                        git push git@github.com:uriya66/DevOps1.git ${newBranch}  # Push using SSH
                    """

                    env.GIT_BRANCH = newBranch // Update environment variable with the new branch name
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Stop execution if any command fails
                    echo "Setting up the Python virtual environment..." # Log message
                    
                    # Create virtual environment if it does not exist
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  

                    . venv/bin/activate  # Activate the virtual environment
                    
                    # Upgrade pip to the latest version
                    venv/bin/python -m pip install --upgrade pip --break-system-packages
                    
                    # Install necessary dependencies for the application
                    venv/bin/python -m pip install flask requests pytest gunicorn --break-system-packages
                """
            }
        }

        stage('Start Gunicorn') {
            steps {
                sh """
                    set -e  # Stop execution if any command fails
                    
                    echo "Stopping the existing Gunicorn service..." # Log message
                    
                    # Stop Gunicorn if it is currently running
                    if systemctl is-active --quiet gunicorn; then
                        sudo -n systemctl stop gunicorn
                    fi

                    echo "Starting the Gunicorn service..." # Log message
                    sudo -n systemctl start gunicorn  # Start Gunicorn service

                    sleep 5  # Wait for Gunicorn to fully start

                    echo "Verifying Gunicorn status..." # Log message
                    
                    # Check if Gunicorn is running; if not, exit with an error
                    if ! systemctl is-active --quiet gunicorn; then
                        echo "ERROR: Gunicorn service failed to start!"
                        exit 1
                    fi
                """
            }
        }

        stage('API Health Check') {
            steps {
                sh """
                    set -e  # Stop execution if any command fails
                    
                    # Ensure the API health check script is executable
                    chmod +x api_health_check.sh  
                    
                    echo "Running API Health Check..." # Log message
                    ./api_health_check.sh  # Execute the health check script
                """
            }
        }

        stage('Test') {
            steps {
                sh """
                    set -e  # Stop execution if any command fails
                    
                    echo "Running API tests..." # Log message
                    
                    . venv/bin/activate  # Activate the virtual environment
                    
                    # Run unit tests using pytest
                    venv/bin/python -m pytest test_app.py
                """
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.GIT_BRANCH.startsWith("feature-") } // Ensure only feature branches are merged
            }
            steps {
                script {
                    echo "Merging ${env.GIT_BRANCH} back to main..." // Log message

                    sh """
                        git checkout main  # Switch to the main branch
                        git pull git@github.com:uriya66/DevOps1.git main  # Ensure main is up to date before merging
                        git merge --no-ff ${env.GIT_BRANCH}  # Merge feature branch into main (no fast-forward)
                        git push git@github.com:uriya66/DevOps1.git main  # Push merged changes to the remote repository
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    // Load external Slack notification script
                    def slack = load 'slack_notifications.groovy'

                    // Construct a Slack message with build details
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)

                    // Send a Slack notification about the build status
                    slack.sendSlackNotification(message, "good")
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}" // Log error if Slack notification fails
                }
            }
        }
    }
}
