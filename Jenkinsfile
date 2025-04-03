pipeline {
    agent any

    environment {
        DEPLOY_SUCCESS = 'false'  // Track deployment status
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                // Ensure GitHub SSH connection is available
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'  // Confirm SSH access (non-blocking)
                }
            }
        }

        stage('Clone feature-test branch (clean)') {
            steps {
                // Checkout the clean latest state from remote feature-test branch
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/feature-test']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:uriya66/DevOps1.git',
                        credentialsId: 'Jenkins-GitHub-SSH'
                    ]]
                ])
            }
        }

        stage('Create & Push feature-${BUILD_NUMBER}') {
            steps {
                // Create and push new isolated branch from feature-test
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh """
                        git checkout -b feature-${BUILD_NUMBER}         # Create a feature branch from feature-test
                        git push origin feature-${BUILD_NUMBER}         # Push the feature branch to GitHub
                    """
                }
            }
        }

        stage('Build') {
            steps {
                echo "[DEBUG] Installing Python dependencies"
                sh '''
                    set -e
                    [ ! -d venv ] && python3 -m venv venv             # Create virtual environment if missing
                    . venv/bin/activate                               # Activate virtual environment
                    pip install --upgrade pip flask pytest gunicorn requests  # Install Python dependencies
                '''
            }
        }

        stage('Test') {
            steps {
                echo "[DEBUG] Running pytest and Flask health"
                sh '''
                    set -e
                    . venv/bin/activate                               # Activate Python environment
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &         # Start Flask app for health checks
                    sleep 5                                           # Wait for app to load
                    pytest test_app.py                                # Run tests
                    pkill -f gunicorn || true                         # Kill Gunicorn after test
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        echo "[DEBUG] Running deploy.sh script"
                        sh '''
                            chmod +x deploy.sh                        # Make deploy script executable
                            ./deploy.sh                               # Run deployment script
                        '''
                        DEPLOY_SUCCESS = 'true'                       // Set deployment success flag
                        echo "[DEBUG] DEPLOY_SUCCESS=true"
                        currentBuild.description = "DEPLOY_SUCCESS=true"
                    } catch (err) {
                        echo "[ERROR] Deployment failed: ${err.message}"
                        DEPLOY_SUCCESS = 'false'
                        echo "[DEBUG] DEPLOY_SUCCESS=false"
                        currentBuild.description = "DEPLOY_SUCCESS=false"
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack helper script

                def deployStatus = DEPLOY_SUCCESS == 'true'
                def color = deployStatus ? 'good' : 'danger'

                echo "[DEBUG] Sending Slack notification with DEPLOY_SUCCESS=${deployStatus}"

                def message = slack.constructSlackMessage(
                    env.BUILD_NUMBER,
                    env.BUILD_URL,
                    null,  // No merge success value used anymore
                    deployStatus
                )

                slack.sendSlackNotification(message, color)  // Send formatted Slack message
            }
        }
    }  // Close post block
}  // Close pipeline block

