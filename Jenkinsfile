pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                // Cloning the Git repository
                git branch: 'main', url: 'https://github.com/uriya66/DevOps1.git'
            }
        }

        stage('Build') {
            steps {
                // Setting up a virtual environment and installing dependencies
                sh '''
                    echo "Setting up virtual environment..."
                    if [ ! -d "venv" ]; then
                        python3 -m venv venv  # Create a virtual environment if it does not exist
                    fi
                    . venv/bin/activate  # Activate virtual environment
                    venv/bin/python -m pip install --upgrade pip --break-system-packages  # Upgrade pip with permission override
                    venv/bin/python -m pip install flask requests pytest --break-system-packages  # Install dependencies
                '''
            }
        }

        stage('Start Server') {
            steps {
                sh '''
                    echo "Stopping existing Flask server..."
                    sudo -n systemctl stop gunicorn || true  # Stop Gunicorn without password prompt

                    echo "Starting Gunicorn service..."
                    sudo -n systemctl start gunicorn  # Start Gunicorn without password prompt

                    sleep 5  # Allow time for server to start

                    echo "Checking if Gunicorn is running..."
                    if ! systemctl is-active --quiet gunicorn; then
                        echo "Gunicorn service failed to start!"
                        exit 1
                    fi
                '''
            }
        }

        stage('Test') {
            steps {
                // Running unit tests using pytest
                sh '''
                    echo "Running Tests..."
                    . venv/bin/activate  # Activate virtual environment
                    venv/bin/python -m pytest test_app.py  # Run tests
                '''
            }
        }

        stage('Deploy') {
            steps {
                // Running deployment script
                sh '''
                    chmod +x deploy.sh  # Ensure deploy.sh is executable
                    . venv/bin/activate  # Activate virtual environment
                    ./deploy.sh  # Execute deployment script
                '''
            }
        }
    }

    post {
        always {
            script {
                try {
                    // Load external Slack notification script
                    def slack = load 'slack_notifications.groovy'
                    
                    // Retrieve Git commit details and construct the Slack message
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

