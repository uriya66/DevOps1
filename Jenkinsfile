pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                // Cloning the Git repository from the main branch
                git branch: 'main', url: 'https://github.com/uriya66/DevOps1.git'
            }
        }

        stage('Build') {
            steps {
                sh '''
                    echo "Setting up virtual environment..."
                    if [ ! -d "venv" ]; then
                        python3 -m venv venv  # Create a virtual environment if it doesn't exist
                    fi
                    venv/bin/python -m pip install --upgrade pip  # Upgrade pip
                    venv/bin/python -m pip install flask requests pytest  # Install required dependencies
                '''
            }
        }

        stage('Start Server') {
            steps {
                sh '''
                    echo "Stopping any existing Flask server..."
                    sudo -n pkill -9 -f "gunicorn" || true  # Kill any running Gunicorn process

                    echo "Ensuring port 5000 is free..."
                    sudo -n fuser -k 5000/tcp || true  # Kill any process using port 5000

                    echo "Starting Flask server..."
                    mkdir -p logs  # Create logs directory if not exists
                    venv/bin/gunicorn -w 4 -b 0.0.0.0:5000 app:app > logs/flask.log 2>&1 &  # Start Gunicorn in background
                    
                    sleep 5  # Give the server some time to start

                    echo "Checking if Flask server is running..."
                    if ! curl -s http://127.0.0.1:5000/health; then
                        echo "❌ Flask server failed to start!"
                        exit 1  # Exit with failure if the health check fails
                    fi
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    echo "Running Tests..."
                    venv/bin/python -m pytest test_app.py  # Run pytest for testing the application
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    chmod +x deploy.sh  # Ensure deploy script is executable
                    ./deploy.sh  # Run the deployment script
                '''
            }
        }
    }

    post {
        always {
            script {
                // Extracting build duration and Git commit details for better logging
                def buildDuration = currentBuild.durationString.replace(' and counting', '')
                def commitHash = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitHash}"
                
                // Build summary message
                def buildSummary = """
🔹 *Jenkins Build #${env.BUILD_NUMBER}*  
🔹 *Pipeline:* [#${env.BUILD_NUMBER}](${env.BUILD_URL})  
🔹 *Branch:* main  
🔹 *Commit:* <${commitUrl}|${commitHash.take(7)}>  
🔹 *Build Duration:* ${buildDuration}  
🔹 *Repository:* <https://github.com/uriya66/DevOps1.git|DevOps1>
"""

                // Notify success or failure in Slack
                try {
                    if (currentBuild.result == 'SUCCESS') {
                        slackSend(
                            channel: '#jenkis-alerts',
                            tokenCredentialId: 'Jenkins-Slack-Token',
                            message: "✅ *Jenkins Build Succeeded!* 🎉\n${buildSummary}",
                            color: 'good'
                        )
                    } else {
                        slackSend(
                            channel: '#jenkis-alerts',
                            tokenCredentialId: 'Jenkins-Slack-Token',
                            message: "❌ *Jenkins Build Failed!* 🚨\n${buildSummary}",
                            color: 'danger'
                        )
                    }
                } catch (Exception e) {
                    echo "⚠️ Slack notification failed: ${e.message}"
                }
            }
        }
    }
}
