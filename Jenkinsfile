pipeline {
    agent any  // Run on any available agent

    environment {
        REPO_URL = 'https://github.com/uriya66/DevOps1.git'
        BRANCH = 'main'
    }

    stages {
        stage('Checkout') {
            steps {
                // Clone the Git repository
                git branch: "${BRANCH}", url: "${REPO_URL}"
            }
        }

        stage('Build') {
            steps {
                // Setup Python virtual environment and install dependencies
                sh '''
                    echo "🔧 Setting up virtual environment..."
                    if [ ! -d "venv" ]; then
                        python3 -m venv venv  # Create virtual environment if not exists
                    fi
                    . venv/bin/activate  # Activate virtual environment
                    venv/bin/python -m pip install --upgrade pip --break-system-packages  # Upgrade pip
                    venv/bin/python -m pip install flask requests pytest --break-system-packages  # Install dependencies
                '''
            }
        }

        stage('Start Server') {
            steps {
                sh '''
                    echo "🛑 Stopping existing Flask server..."
                    sudo -n systemctl stop gunicorn || true  # Stop Gunicorn if running

                    echo "🚀 Starting Gunicorn service..."
                    sudo -n systemctl start gunicorn  # Start Gunicorn service

                    sleep 5  # Wait for the server to start

                    echo "🔍 Verifying Gunicorn status..."
                    if ! systemctl is-active --quiet gunicorn; then
                        echo "❌ Gunicorn service failed to start!"
                        exit 1
                    fi
                '''
            }
        }

        stage('Test') {
            steps {
                // Run unit tests using pytest
                sh '''
                    echo "🧪 Running Tests..."
                    . venv/bin/activate  # Activate virtual environment
                    venv/bin/python -m pytest test_app.py  # Run pytest
                '''
            }
        }

        stage('Deploy') {
            steps {
                // Execute deployment script
                sh '''
                    chmod +x deploy.sh  # Ensure deploy.sh is executable
                    . venv/bin/activate  # Activate virtual environment
                    ./deploy.sh  # Run deployment script
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

                    // Construct Slack message with build details
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)

                    // Send Slack notification
                    slack.sendSlackNotification(message, "good")
                } catch (Exception e) {
                    echo "⚠️ Error sending Slack notification: ${e.message}"
                }
            }
        }
    }
}

