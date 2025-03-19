pipeline {
    agent any  // Run the pipeline on any available agent

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Define the Git repository URL
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository..."  // Log message
                    checkout scm  // Checkout the repository from SCM

                    // Use the Jenkins SSH Agent plugin to manage SSH keys
                    sshagent(['Jenkins-GitHub-Token']) {
                        sh """
                            echo "Verifying SSH authentication..."
                            ssh-add -l || echo "No SSH keys loaded in the agent!"
                            ssh -T git@github.com || echo "SSH Connection failed!"
                        """
                    }

                    // Get the current branch dynamically
                    def currentBranch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                    env.GIT_BRANCH = currentBranch  // Store the branch name in an environment variable
                    echo "Current branch: ${env.GIT_BRANCH}"  // Log the current branch name
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    def newBranch = "feature-${env.BUILD_NUMBER}"  // Define the feature branch name
                    echo "Creating a new feature branch: ${newBranch}"  // Log the branch creation

                    sshagent(['Jenkins-GitHub-Token']) {  // Use the SSH Agent plugin for authentication
                        sh """
                            git checkout -b ${newBranch}  // Create a new branch locally
                            git push git@github.com:uriya66/DevOps1.git ${newBranch}  // Push the branch to GitHub
                        """
                    }
                    env.GIT_BRANCH = newBranch  // Store the new branch name in an environment variable
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  // Stop execution if any command fails

                    echo "Setting up Python virtual environment..."  // Log message

                    # Create a virtual environment if it does not exist
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi

                    . venv/bin/activate  # Activate the virtual environment

                    # Upgrade pip and install necessary dependencies
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Start Gunicorn') {
            steps {
                sh """
                    set -e  // Stop execution if any command fails

                    echo "Stopping the existing Gunicorn service..."  // Log message

                    # Stop Gunicorn if it is currently running
                    if systemctl is-active --quiet gunicorn; then
                        sudo -n systemctl stop gunicorn
                    fi

                    echo "Starting the Gunicorn service..."  // Log message
                    sudo -n systemctl start gunicorn  # Start the Gunicorn service

                    sleep 5  # Wait for Gunicorn to fully start

                    echo "Verifying Gunicorn status..."  // Log message

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
                    set -e  // Stop execution if any command fails

                    chmod +x api_health_check.sh  # Ensure the health check script is executable

                    echo "Running API Health Check..."  // Log message
                    ./api_health_check.sh  # Execute the health check script
                """
            }
        }

        stage('Test') {
            steps {
                sh """
                    set -e  // Stop execution if any command fails

                    echo "Running API tests..."  // Log message

                    . venv/bin/activate  # Activate the virtual environment

                    # Run unit tests using pytest
                    venv/bin/python -m pytest test_app.py
                """
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.GIT_BRANCH.startsWith("feature-") }  // Ensure only feature branches are merged
            }
            steps {
                script {
                    echo "Merging ${env.GIT_BRANCH} back to main..."  // Log message

                    sshagent(['Jenkins-GitHub-Token']) {  // Use the SSH Agent plugin for authentication
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
                    echo "Error sending Slack notification: ${e.message}"  // Log error if Slack notification fails
                }
            }
        }
    }
}

