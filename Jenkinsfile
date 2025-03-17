pipeline {
    agent any // Run on any available Jenkins agent

    environment {
        REPO_URL = 'https://github.com/uriya66/DevOps1.git' // GitHub repository URL
        BRANCH = '*/feature-*'  // Run on any feature branch
    }

    stages {
        stage('Checkout') {
            steps {
                // Clone the Git repository from the specified branch
                git branch: "${BRANCH}", url: "${REPO_URL}"
            }
        }

        stage('Create Feature Branch') {
            when {
                branch 'main'  // This stage runs only if we're on main branch
            }
            steps {
                script {
                    def newBranch = "feature-${env.BUILD_NUMBER}" // Generate a unique feature branch name
                    echo "Creating new feature branch: ${newBranch}"
                    
                    sh """
                        git checkout -b ${newBranch}
                        git push origin ${newBranch}
                    """
                }
            }
        }

        stage('Build') {
            steps {
                // Set up a Python virtual environment and install dependencies
                sh """
                    set -e  # Stop script on error
                    echo "Setting up virtual environment..."
                    if [ ! -d "venv" ]; then
                        python3 -m venv venv
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
                        sudo -n systemctl stop gunicorn
                    fi

                    echo "Starting Gunicorn service..."
                    sudo -n systemctl start gunicorn

                    sleep 5  # Wait for the server to start

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
                // Run the API health check script
                sh """
                    set -e
                    chmod +x api_health_check.sh  # Ensure script is executable
                    ./api_health_check.sh  # Run the health check script
                """
            }
        }

        stage('Test') {
            steps {
                // Run unit tests using pytest
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
                branch 'feature-*' // Run only on feature branches
            }
            steps {
                sh """
                    git checkout main
                    git merge --no-ff \$(git rev-parse --abbrev-ref HEAD)
                    git push origin main
                """
            }
        }
    }

    post {
        always {
            script {
                try {
                    // Load external Slack notification script
                    def slack = load 'slack_notifications.groovy'

                    // Construct Slack message with build details
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
