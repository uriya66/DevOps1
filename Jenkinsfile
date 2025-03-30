
pipeline {
    agent any  // Use any available Jenkins agent

    environment {
        DEPLOY_SUCCESS = 'false' // Flag to track deployment status
        MERGE_SUCCESS  = 'false' // Flag to track merge status
    }

    stages {

        stage('Checkout SCM') {
            steps {
                checkout scm  // Checkout source code from GitHub
            }
        }

        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def lastCommit = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()  // Get last commit message
                    echo "[DEBUG] Last commit message: ${lastCommit}"  // Print commit message
                    if (lastCommit.contains('Merge branch')) {
                        echo "[INFO] Skipping build due to merge commit."  // Skip build if it's a merge commit
                        currentBuild.result = 'SUCCESS'
                        return
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'  // Validate SSH connectivity to GitHub
                }
            }
        }

        stage('Checkout Main') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:uriya66/DevOps1.git',
                        credentialsId: 'Jenkins-GitHub-SSH'
                    ]]
                ])  // Checkout the main branch from GitHub
            }
        }

        stage('Create Feature Branch') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh """
                        git checkout -b feature-${BUILD_NUMBER}  # Create a new feature branch
                        git push origin feature-${BUILD_NUMBER}  # Push feature branch to GitHub
                    """
                }
            }
        }

        stage('Build') {
            steps {
                echo "[DEBUG] Installing dependencies"  // Log start of build stage
                sh '''
                    set -e
                    [ ! -d venv ] && python3 -m venv venv  # Create virtual environment if missing
                    . venv/bin/activate  # Activate venv
                    pip install --upgrade pip flask pytest gunicorn requests  # Install dependencies
                '''
            }
        }

        stage('Test') {
            steps {
                echo "[DEBUG] Running tests with Gunicorn"  // Log start of test stage
                sh '''
                    set -e
                    . venv/bin/activate  # Activate virtual environment
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start Flask app with Gunicorn
                    sleep 5  # Wait for server to boot
                    pytest test_app.py  # Run tests
                    pkill -f gunicorn || true  # Stop Gunicorn server
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        echo "[DEBUG] Running deployment script"  // Log deployment start
                        sh '''
                            set -e
                            chmod +x deploy.sh  # Make deployment script executable
                            ./deploy.sh  # Execute deployment script
                        '''
                        echo "[INFO] Deployment succeeded"
                        env.DEPLOY_SUCCESS = 'true'  // Mark deployment as successful
                    } catch (e) {
                        echo "[ERROR] Deployment failed"
                        env.DEPLOY_SUCCESS = 'false'  // Mark deployment as failed
                        currentBuild.result = 'FAILURE'
                        error("Stopping pipeline due to deployment failure.")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    def status = (env.DEPLOY_SUCCESS == 'true')  // Check if deployment succeeded
                    echo "[DEBUG] DEPLOY_SUCCESS flag from description: ${env.DEPLOY_SUCCESS}"  // Debug log
                    return status
                }
            }
            steps {
                echo "[INFO] Starting merge to main branch"  // Log merge start
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh """
                        git config user.name 'jenkins'  # Set Git username
                        git config user.email 'jenkins@example.com'  # Set Git email
                        git checkout main  # Switch to main branch
                        git pull origin main  # Sync main branch
                        git merge feature-${BUILD_NUMBER}  # Merge feature branch
                        git push origin main  # Push changes to main
                    """
                }
                script {
                    env.MERGE_SUCCESS = 'true'  // Mark merge as successful
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack notification script

                    def message = slack.constructSlackMessage(
                        env.BUILD_NUMBER,  // Jenkins build number
                        env.BUILD_URL,  // Jenkins build URL
                        env.MERGE_SUCCESS == 'true',  // Merge result
                        env.DEPLOY_SUCCESS == 'true'  // Deploy result
                    )

                    def color = (env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true') ? "good" : "danger"  // Set Slack message color

                    slack.sendSlackNotification(message, color)  // Send Slack notification
                } catch (Exception e) {
                    echo "Slack notification error: ${e.message}"  // Log Slack error
                }
            }
        }
    }  // Close post block
}  // Close pipeline block

