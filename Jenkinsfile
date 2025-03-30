pipeline {
    agent any

    environment {
        DEPLOY_SUCCESS = 'false'       // Global variable for deployment status
        MERGE_SUCCESS  = 'false'       // Global variable for merge status
    }

    stages {

        stage('Checkout SCM') {
            steps {
                checkout scm  // Pull latest code from trigger branch
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
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true' // Validate GitHub SSH
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
                ]) // Pull main branch to prepare for merge
            }
        }

        stage('Create Feature Branch') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh """
                        git checkout -b feature-${BUILD_NUMBER}  # Create new feature branch
                        git push origin feature-${BUILD_NUMBER}  # Push branch to remote
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
                        env.DEPLOY_SUCCESS = 'true'  // Mark success
                    } catch (e) {
                        echo "[ERROR] Deployment failed"
                        env.DEPLOY_SUCCESS = 'false' // Mark failure
                        currentBuild.result = 'FAILURE'
                        error("Stopping pipeline due to deployment failure.")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    echo "[DEBUG] DEPLOY_SUCCESS flag from env: ${env.DEPLOY_SUCCESS}"
                    return env.DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                echo "[INFO] Starting merge to main branch"
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        try {
                            sh """
                                git config user.name 'jenkins'
                                git config user.email 'jenkins@example.com'
                                git checkout main
                                git pull origin main
                                git merge feature-${BUILD_NUMBER}
                                git push origin main
                            """
                            env.MERGE_SUCCESS = 'true' // Mark merge success
                        } catch (err) {
                            echo "[ERROR] Merge failed"
                            env.MERGE_SUCCESS = 'false'
                            error("Merge to main failed")
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'  // Load shared Slack message script

                // Construct rich Slack message with all metadata
                def message = slack.constructSlackMessage(
                    env.BUILD_NUMBER,
                    env.BUILD_URL,
                    env.MERGE_SUCCESS == 'true',
                    env.DEPLOY_SUCCESS == 'true'
                )

                // Determine color based on pipeline outcome
                def color = (env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true') ? "good" : "danger"

                slack.sendSlackNotification(message, color)  // Send to Slack
            }
        }
    }  // Close post block
}  // Close pipeline block

