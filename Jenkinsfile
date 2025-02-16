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
                sh 'chmod +x deploy.sh && ./deploy.sh'
            }
        }

        stage('Test') {
            steps {
                sh 'echo "Running Tests..."'
            }
        }

        stage('Deploy') {
            steps {
                sh 'echo "Deploying Application..."'
            }
        }
    }
}
