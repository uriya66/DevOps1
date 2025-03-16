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
                    venv/bin/python -m pip install --upgrade pip  # Upgrade pip
                    venv/bin/python -m pip install flask requests pytest  # Install required dependencies
                '''
            }
        }

        stage('Start Server') {
            steps {
                // Ensuring the server is stopped, then starting the Flask application
                sh '''
                    echo "Activating virtual environment..."
                    . venv/bin/activate  # Activate virtual environment

                    echo "Stopping any existing Flask server..."
                    sudo pkill -f "gunicorn" || true  # Kill any running Gunicorn processes
                    sudo pkill -9 -f "gunicorn" || true  # Force kill if necessary

                    echo "Ensuring port 5000 is free..."
                    sudo fuser -k 5000/tcp || true  # Free port 5000 if occupied

                    echo "Starting Flask server..."
                    mkdir -p logs  # Ensure logs directory exists
                    venv/bin/gunicorn -w 4 -b 0.0.0.0:5000 app:app > logs/flask.log 2>&1 &  # Start Gunicorn in the background

                    sleep 5  # Allow the server time to initialize

                    echo "Checking if Flask server is running..."
                    if ! curl -s http://127.0.0.1:5000/health; then
                        echo "Flask server failed to start!"
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

