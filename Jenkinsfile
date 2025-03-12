pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                // Clone the Git repository from GitHub, ensuring the correct branch is checked out
                git branch: 'main', url: 'https://github.com/uriya66/DevOps1.git'
            }
        }

        stage('Build') {
            steps {
                // Ensure Python virtual environment exists and install dependencies
                sh '''
                    echo "Setting up virtual environment..."
                    if [ ! -d "venv" ]; then
                        python3 -m venv venv  # Create a virtual environment if not exists
                    fi
                    bash -c "source venv/bin/activate && pip install --upgrade pip && pip install flask requests pytest"
                '''
            }
        }

        stage('Start Server') {
            steps {
                sh '''
                    echo "Stopping any existing Flask server..."
                    pkill -9 -f "python3 app.py" || true  # Ignore error if process is not running

                    echo "Ensuring port 5000 is free..."
                    sudo -n fuser -k 5000/tcp || true  # Forcefully kill any process using port 5000

                    echo "Starting Flask server..."
                    nohup bash -c "source venv/bin/activate && exec python3 app.py" > flask.log 2>&1 &

                    sleep 5  # Give it time to initialize

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
                // Run the test suite using pytest
                sh '''
                    echo "Running Tests..."
                    bash -c "source venv/bin/activate && pytest test_app.py"
                '''
            }
        }

        stage('Deploy') {
            steps {
                // Ensure the deploy script is executable and run it
                sh '''
                    chmod +x deploy.sh  # Grant execute permissions to deploy script
                    ./deploy.sh  # Execute deployment script
                '''
            }
        }
    }
    
    post {
        failure {
            // 📢 Send an alert to the Slack channel if the build fails
            slackSend channel: '#devops-alerts', message: "❌ Jenkins Build Failed! Check pipeline: ${env.BUILD_URL}"
        }
    }
}
