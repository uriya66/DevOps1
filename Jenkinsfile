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
                // Set up a Python virtual environment and install dependencies
                sh '''
                    echo "Setting up virtual environment..."
                    python3 -m venv venv  # Create a virtual environment
                    bash -c "source venv/bin/activate && pip install --upgrade pip && pip install flask requests pytest"
                '''
            }
        }

stage('Start Server') {
    steps {
        sh '''
            echo "Stopping any existing Flask server..."
            pkill -f "python3 app.py" || true  # Ignore error if not running

            echo "Starting Flask server..."
            bash -c "source venv/bin/activate && nohup python3 app.py &"
            sleep 5  # Give it time to initialize
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
}
