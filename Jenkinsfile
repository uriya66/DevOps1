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
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest
                '''
            }
        }

        stage('Start Server') {
            steps {
                sh '''
                    echo "Stopping any existing Flask server..."
                    sudo -n pkill -9 -f "gunicorn" || true

                    echo "Ensuring port 5000 is free..."
                    sudo -n fuser -k 5000/tcp || true

                    echo "Starting Flask server..."
                    mkdir -p logs
                    venv/bin/gunicorn -w 4 -b 0.0.0.0:5000 app:app > logs/flask.log 2>&1 &
                    
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
                    venv/bin/python -m pytest test_app.py
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
        always {
            script {
                def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                def pipelineUrl = "${env.BUILD_URL}"
                def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"
                def duration = "${currentBuild.durationString.replace(' and counting', '')}"

                def message = """
                ✅ *Jenkins Build Succeeded!* 🎉
                *Pipeline:* #${env.BUILD_NUMBER}
                *Branch:* ${branch}
                *Commit:* [${commitId}](${commitUrl})
                *Message:* ${commitMessage}
                *Duration:* ${duration}
                *Pipeline Link:* [View Pipeline](${pipelineUrl})
                """

                try {
                    slackSend(
                        channel: '#jenkins-alerts',
                        tokenCredentialId: 'Jenkins-Slack-Token',
                        message: message,
                        color: 'good'
                    )
                } catch (Exception e) {
                    echo "⚠️ Slack notification failed: ${e.message}"
                }
            }
        }
    }
}
