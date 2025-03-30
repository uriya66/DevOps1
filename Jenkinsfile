pipeline {
    agent any

    environment {
        DEPLOY_SUCCESS = 'false' // Custom flag to control merge stage
    }

    stages {

        stage('Checkout SCM') {
            steps {
                checkout scm // Checkout source code from the SCM
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
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true' // Verify SSH access to GitHub
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
                ]) // Checkout main branch explicitly via SSH
            }
        }

        stage('Create Feature Branch') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh """
                        git checkout -b feature-${BUILD_NUMBER} # Create new feature branch
                        git push origin feature-${BUILD_NUMBER} # Push to GitHub
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
                echo "[DEBUG] Running tests with Gunicorn"
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
                        echo "[ERROR] Deployment failed"
                        DEPLOY_SUCCESS = 'false'
                        currentBuild.description = "DEPLOY_SUCCESS=false"
                        currentBuild.result = 'FAILURE'
                        error("Stopping pipeline due to deployment failure.")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    def successFlag = currentBuild.description?.contains('DEPLOY_SUCCESS=true')
                    echo "[DEBUG] DEPLOY_SUCCESS flag from description: ${successFlag}"
                    return successFlag
                }
            }
            steps {
                echo "[INFO] Starting merge to main branch"
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh """
                        git config user.name 'jenkins'
                        git config user.email 'jenkins@example.com'
                        git checkout main
                        git pull origin main
                        git merge feature-${BUILD_NUMBER}
                        git push origin main
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                def commit = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                def message = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                def branch = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
                def ip = sh(script: 'curl -s http://checkip.amazonaws.com', returnStdout: true).trim()
                def color = currentBuild.currentResult == 'SUCCESS' ? 'good' : 'danger'

                def payload = [
                    branch: "feature-${BUILD_NUMBER}",
                    commit: commit.take(7),
                    message: message,
                    result: currentBuild.currentResult,
                    app_url: "http://${ip}:5000"
                ]

                slackSend (
                    channel: '#jenkis_alerts',
                    color: color,
                    tokenCredentialId: 'Jenkins-Slack-Token',
                    teamDomain: 'devops-c4a8276',
                    message: "[${payload.result}] Branch: ${payload.branch}, Commit: ${payload.commit}, Message: ${payload.message}, App: ${payload.app_url}"
                )
            }
        }
    }  // Close post block
}  // Close pipeline block

