pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/uriya66/DevOps1.git'
            }
        }

        stage('Build') {
            steps {
                sh '''
                    echo "Setting up virtual environment..."
                    if [ ! -d "venv" ]; then
                        python3 -m venv venv
                    fi
                    bash -c "source venv/bin/activate && pip install --upgrade pip && pip install flask requests pytest"
                '''
            }
        }

        stage('Start Server') {
            steps {
                sh '''
                    echo "Stopping any existing Flask server..."
                    sudo pkill -9 -f "gunicorn" || true

                    echo "Ensuring port 5000 is free..."
                    sudo -n fuser -k 5000/tcp || true

                    echo "Starting Flask server..."
                    nohup bash -c "source venv/bin/activate && exec gunicorn -w 4 -b 0.0.0.0:5000 app:app" > logs/flask.log 2>&1 &

                    sleep 5  # Give it time to initialize

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
                sh '''
                    echo "Running Tests..."
                    bash -c "source venv/bin/activate && pytest test_app.py"
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    chmod +x deploy.sh
                    ./deploy.sh
                '''
            }
        }
    }
    
    post {
        failure {
            slackSend channel: '#devops-alerts', tokenCredentialId: 'Jenkins-GitHub-Token', message: "❌ Jenkins Build Failed! Check pipeline: ${env.BUILD_URL}"
        }
    }
}

