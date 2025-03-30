pipeline {
    agent any

    environment {
        DEPLOY_SUCCESS = 'false'  // Track deployment result
        MERGE_SUCCESS = 'false'   // Track merge result
    }

    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm  // Checkout source code from SCM
            }
        }

        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def lastCommit = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                    echo "[DEBUG] Last commit message: ${lastCommit}"
                    if (lastCommit.contains('Merge branch')) {
                        echo "[INFO] Skipping build due to merge commit."
                        currentBuild.result = 'SUCCESS'
                        return
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'  // Verify GitHub access
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
                ])  // Checkout main branch explicitly
            }
        }

        stage('Create Feature Branch') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh """
                        git checkout -b feature-${BUILD_NUMBER}  # Create new branch
                        git push origin feature-${BUILD_NUMBER}  # Push to GitHub
                    """
                }
            }
        }

        stage('Build') {
            steps {
                echo "[DEBUG] Installing dependencies"
                sh '''
                    set -e
                    [ ! -d venv ] && python3 -m venv venv
                    . venv/bin/activate
                    pip install --upgrade pip flask pytest gunicorn requests
                '''
            }
        }

        stage('Test') {
            steps {
                echo "[DEBUG] Running tests"
                sh '''
                    set -e
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 5
                    pytest test_app.py
                    pkill -f gunicorn || true
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        echo "[DEBUG] Running deployment script"
                        sh '''
                            set -e
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        echo "[INFO] Deployment succeeded"
                        DEPLOY_SUCCESS = 'true'
                        currentBuild.description = "DEPLOY_SUCCESS=true"
                    } catch (e) {
                        echo "[ERROR] Deployment failed: ${e.message}"
                        DEPLOY_SUCCESS = 'false'
                        currentBuild.description = "DEPLOY_SUCCESS=false"
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    def deployFlag = currentBuild.description?.contains('DEPLOY_SUCCESS=true')
                    echo "[DEBUG] DEPLOY_SUCCESS from description: ${deployFlag}"
                    return deployFlag
                }
            }
            steps {
                script {
                    try {
                        echo "[INFO] Starting merge to main"
                        sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                            sh """
                                git config user.name 'jenkins'
                                git config user.email 'jenkins@example.com'
                                git checkout main
                                git pull origin main
                                git merge --no-ff feature-${BUILD_NUMBER} || echo "[INFO] Nothing to merge"
                                git push origin main || echo "[INFO] Nothing was pushed to main"
                            """
                        }
                        MERGE_SUCCESS = 'true'
                    } catch (e) {
                        echo "[ERROR] Merge failed: ${e.message}"
                        MERGE_SUCCESS = 'false'
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack module

                def mergeStatus = MERGE_SUCCESS == 'true'
                def deployStatus = DEPLOY_SUCCESS == 'true'
                def color = (mergeStatus && deployStatus) ? 'good' : 'danger'

                def message = slack.constructSlackMessage(
                    env.BUILD_NUMBER,
                    env.BUILD_URL,
                    mergeStatus,
                    deployStatus
                )

                slack.sendSlackNotification(message, color)  // Send Slack notification
            }
        }
    }  // Close post block
}  // Close pipeline block

