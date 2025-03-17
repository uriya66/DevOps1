pipeline {
    agent any // Run on any available Jenkins agent

    environment {
        REPO_URL = 'https://github.com/uriya66/DevOps1.git' // GitHub repository URL
        BRANCH = "feature-${env.BUILD_NUMBER}" // Generate a unique feature branch per build
    }

    stages {
        stage('Checkout') {
            steps {
                // Checkout the repository using the current branch running the build
                git branch: "${env.GIT_BRANCH}", url: "${REPO_URL}"
            }
        }

        stage('Create Feature Branch') {
            when {
                expression { env.GIT_BRANCH == 'main' } // Only create a new feature branch if running on main
            }
            steps {
                script {
                    def newBranch = "feature-${env.BUILD_NUMBER}" // Generate a unique feature branch name
                    echo "Creating new feature branch: ${newBranch}"

                    sh """
                        git checkout -b ${newBranch}  # Create the new feature branch
                        git push origin ${newBranch}  # Push the new branch to remote repository
                    """
                }
            }
        }

        stage('Build') {
            steps {
                // Set up a Python virtual environment and install required dependencies
                sh """
                    set -e  # Stop script on error
                    echo "Setting up virtual environment..."
                    if [ ! -d "venv" ]; then
                        python3 -m venv venv  # Create virtual environment if it doesn't exist
                    fi
                    . venv/bin/activate
                    venv/bin/python -m pip install --upgrade pip --break-system-packages
                    venv/bin/python -m pip install flask requests pytest gunicorn --break-system-packages
                """
            }
        }

        stage('Start Gunicorn') {
            steps {
                sh """
                    set -e
                    echo "Stopping existing Gunicorn service..."
                    if systemctl is-active --quiet gunicorn; then
                        sudo -n systemctl stop gunicorn  # Stop Gunicorn if running
                    fi

                    echo "Starting Gunicorn service..."
                    sudo -n systemctl start gunicorn  # Start Gunicorn service

                    sleep 5  # Wait for Gunicorn to fully start

                    echo "Verifying Gunicorn status..."
                    if ! systemctl is-active --quiet gunicorn; then
                        echo "ERROR: Gunicorn service failed to start!"
                        exit 1
                    fi
                """
            }
        }

        stage('API Health Check') {
            steps {
                // Run the API health check script to ensure the Flask application is running correctly
                sh """
                    set -e
                    chmod +x api_health_check.sh  # Ensure script is executable
                    ./api_health_check.sh  # Execute the health check script
                """
            }
        }

        stage('Test') {
            steps {
                // Run unit tests using pytest to verify API functionality
                sh """
                    set -e
                    echo "Running Tests..."
                    . venv/bin/activate
                    venv/bin/python -m pytest test_app.py
                """
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.GIT_BRANCH.startsWith("feature-") } // Merge only feature branches
            }
            steps {
                script {
                    echo "Merging feature branch back to main..."
                    sh """
                        git checkout main  # Switch to the main branch
                        git merge --no-ff ${env.GIT_BRANCH}  # Merge feature branch changes
                        git push origin main  # Push merged changes to remote repository
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    // Load external Slack notification script to notify about the pipeline result
                    def slack = load 'slack_notifications.groovy'

                    // Construct Slack message containing build details
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)

                    // Send Slack notification
                    slack.sendSlackNotification(message, "good")
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"
                }
            }
        }
    }
}

