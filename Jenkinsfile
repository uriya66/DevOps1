pipeline {
    agent any

    environment {
        DEPLOY_SUCCESS = 'false'  // Track deployment status
        MERGE_SUCCESS = 'false'   // Track merge status
    }

    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm  // Checkout source triggered this pipeline
            }
        }

        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def lastCommit = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                    echo "[DEBUG] Last commit message: ${lastCommit}"
                    if (lastCommit.contains('Merge branch')) {
                        echo "[INFO] Skipping redundant build triggered by merge commit."
                        currentBuild.result = 'SUCCESS'
                        return
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'  // Confirm SSH access
                }
            }
        }

        stage('Checkout feature-test') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/feature-test']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:uriya66/DevOps1.git',
                        credentialsId: 'Jenkins-GitHub-SSH'
                    ]]
                ])
            }
        }

        stage('Create Feature Branch') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh """
                        git checkout -b feature-${BUILD_NUMBER}
                        git push origin feature-${BUILD_NUMBER}
                    """
                }
            }
        }

        stage('Build') {
            steps {
                echo "[DEBUG] Installing Python dependencies"
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
                echo "[DEBUG] Running pytest and Flask health"
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
                        echo "[DEBUG] Running deploy.sh script"
                        sh '''
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        DEPLOY_SUCCESS = 'true'
                        currentBuild.description = "DEPLOY_SUCCESS=true"
                    } catch (err) {
                        echo "[ERROR] Deployment failed: ${err.message}"
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
                    return currentBuild.description?.contains('DEPLOY_SUCCESS=true')
                }
            }
            steps {
                script {
                    try {
                        echo "[INFO] Merging feature-${BUILD_NUMBER} to main"
                        sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                            sh """
                                git config user.name 'jenkins'
                                git config user.email 'jenkins@example.com'
                                git checkout main
                                git pull origin main
                                git merge --no-ff feature-${BUILD_NUMBER} -m 'CI: Auto-merge feature branch to main after successful pipeline'
                                git push origin main
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
                def slack = load 'slack_notifications.groovy'

                def mergeStatus = MERGE_SUCCESS == 'true'
                def deployStatus = DEPLOY_SUCCESS == 'true'
                def color = (mergeStatus && deployStatus) ? 'good' : 'danger'

                def message = slack.constructSlackMessage(
                    env.BUILD_NUMBER,
                    env.BUILD_URL,
                    mergeStatus,
                    deployStatus
                )

                slack.sendSlackNotification(message, color)
            }
        }
    }  // Close post block
}  // Close pipeline block
