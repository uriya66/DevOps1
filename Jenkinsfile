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
                sh 'echo "Building Project..."'
                sh 'pip install flask requests pytest'
            }
        }

        stage('Start Server') {
            steps {
                sh 'nohup python3 app.py &'
                sh 'sleep 5'  // Waiting for the server to start working
            }
        }

        stage('Test') {
            steps {
                sh 'pytest test_app.py'
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying Application..."
            }
        }
    }
}
