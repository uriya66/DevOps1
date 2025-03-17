pipeline {
    agent any // Run on any available Jenkins agent

    environment {
        REPO_URL = 'https://github.com/uriya66/DevOps1.git' // Define the GitHub repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Create a unique feature branch per build
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository..." // Print message indicating checkout process
                    git branch: "main", url: "${REPO_URL}" // Checkout the main branch from GitHub

                    // Get the current branch name and store it in an environment variable
                    def currentBranch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                    env.GIT_BRANCH = currentBranch
                    echo "Current branch: ${env.GIT_BRANCH}" // Print the current branch name
                }
            }
        }

        stage('Create Feature Branch') {
            when {
                expression { env.GIT_BRANCH == 'main' }  // Only create a new branch if running on main
            }
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"  // Log the branch creation

                    // Create new branch and push it
                    sh """
                        git checkout -b ${BRANCH_NAME}  # Create a new feature branch
                        git push origin ${BRANCH_NAME}  # Push the branch to the remote repository
                    """

                    env.GIT_BRANCH = BRANCH_NAME  // Update the environment variable with the new branch name
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Stop execution if any command fails
                    echo "Setting up the Python virtual environment..."
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create virtual environment if not exists
                    . venv/bin/activate  # Activate the virtual environment
                    venv/bin/python -m pip install --upgrade pip --break-system-packages # Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn --break-system-packages # Install dependencies
                """
            }
        }

        stage('Start Gunicorn') {
            steps {
                sh """
                    set -e  # Stop execution if any command fails
                    echo "Stopping the existing Gunicorn service..."
                    if systemctl is-active --quiet gunicorn; then
                        sudo -n systemctl stop gunicorn  # Stop Gunicorn if it is already running
                    fi

                    echo "Starting the Gunicorn service..."
                    sudo -n systemctl start gunicorn  # Start the Gunicorn service

                    sleep 5  # Wait for Gunicorn to fully start

                    echo "Verifying Gunicorn status..."
                    if ! systemctl is-active --quiet gunicorn; then
                        echo "ERROR: Gunicorn service failed to start!"
                        exit 1  # Exit with an error if Gunicorn is not running
                    fi
                """
            }
        }

        stage('API Health Check') {
            steps {
                // Run the API health check script to ensure the Flask application is running correctly
                sh """
                    set -e  # Stop execution if any command fails
                    chmod +x api_health_check.sh  # Ensure the script is executable
                    ./api_health_check.sh  # Execute the health check script
                """
            }
        }

        stage('Test') {
            steps {
                // Run unit tests using pytest to validate API functionality
                sh """
                    set -e  # Stop execution if any command fails
                    echo "Running API tests..."
                    . venv/bin/activate  # Activate the virtual environment
                    venv/bin/python -m pytest test_app.py  # Run pytest
                """
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.GIT_BRANCH.startsWith("feature-") } // Only merge feature branches back to main
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Tests passed, merging ${env.GIT_BRANCH} back to main..."

                        // Merge only if all previous stages were successful
                        sh """
                            git checkout main  # Switch to the main branch
                            git pull origin main  # Ensure we have the latest main branch before merging
                            git merge --no-ff ${env.GIT_BRANCH}  # Merge the feature branch into main (no fast-forward)
                            git push origin main  # Push merged changes to the remote repository
                        """
                    } else {
                        echo "Tests failed, skipping merge!"
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                echo "Build & Tests passed. Merging branch automatically."
            }
        }
        failure {
            script {
                echo "Build or Tests failed. NOT merging to main."
            }
        }
        always {
            script {
                try {
                    // Load external Slack notification script to notify about the pipeline result
                    def slack = load 'slack_notifications.groovy'

                    // Construct Slack message containing build details
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)

                     // Send Slack notification with the build status
                    slack.sendSlackNotification(message, "good")
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"  // Print error message in case of failure
                }
            }
        }
    }
}

