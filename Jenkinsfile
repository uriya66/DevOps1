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
                        python3 -m venv venv
                    fi
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest
                '''
            }
        }

        stage('Start Server') {
            steps {
                // Ensuring the server is stopped, then starting the Flask application
                sh '''
                    echo "Stopping any existing Flask server..."
                    sudo -n pkill -9 -f "gunicorn" || true

                    echo "Ensuring port 5000 is free..."
                    sudo -n fuser -k 5000/tcp || true

                    echo "Starting Flask server..."
                    mkdir -p logs
                    venv/bin/gunicorn -w 4 -b 0.0.0.0:5000 app:app > logs/flask.log 2>&1 &

                    sleep 5  # Give the server time to initialize

                    echo "Checking if Flask server is running..."
                    if ! curl -s http://127.0.0.1:5000/health; then
                        echo "❌ Flask server failed to start!"
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
                    venv/bin/python -m pytest test_app.py
                '''
            }
        }

        stage('Deploy') {
            steps {
                // Running deployment script
                sh '''
                    chmod +x deploy.sh
                    ./deploy.sh
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
                    echo "⚠️ Error sending Slack notification: ${e.message}"
                }
            }
        }
    }
}
