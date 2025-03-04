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
                    python3 -m venv venv
                    source venv/bin/activate
                    pip install --upgrade pip
                    pip install flask requests pytest
                '''
            }
        }

        stage('Start Server') {
            steps {
                sh '''
                    echo "Starting Flask server..."
                    source venv/bin/activate
                    nohup python3 app.py & 
                    sleep 5
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    echo "Running Tests..."
                    source venv/bin/activate
                    pytest test_app.py
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
}
