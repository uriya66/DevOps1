pipeline {
    agent any

    environment {
        DEPLOY_SUCCESS = 'false'  // Tracks if deployment was successful
        MERGE_SUCCESS  = 'false'  // Tracks if merge was successful
    }

    stages {

        stage('Init Git from Trigger Source') {
            steps {
                checkout scm  // Default checkout from GitHub push trigger
            }
        }

        stage('Skip Auto-Merge Loop on main') {
            steps {
                script {
                    def currentBranch = env.GIT_BRANCH?.replace('origin/', '') ?: 'UNKNOWN'
                    echo "[DEBUG] GIT_BRANCH from env: ${env.GIT_BRANCH}"
                    echo "[DEBUG] Cleaned currentBranch: ${currentBranch}"

                    if (currentBranch == 'main') {
                        def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                        echo "[DEBUG] Last commit on main: ${lastCommitMessage}"

                        if (lastCommitMessage.startsWith("JENKINS AUTO MERGE -")) {
                            echo "[INFO] Auto-merge commit detected. Skipping redundant build."
                            currentBuild.result = 'SUCCESS'
                            error("Stopping pipeline: redundant auto-merge build on main.")
                        }
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'  // Validate SSH access
                }
            }
        }

        stage('Clone feature-test branch (clean)') {
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

        stage('Create & Push feature-${BUILD_NUMBER}') {
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
                        echo "[DEBUG] Running deploy script"
                        sh '''
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        DEPLOY_SUCCESS = 'true'
                        echo "[DEBUG] DEPLOY_SUCCESS=true"
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
                    echo "[DEBUG] Checking if deploy succeeded for merge stage: ${DEPLOY_SUCCESS}"
                    return DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                script {
                    try {
                        echo "[INFO] Merging feature-${BUILD_NUMBER} to main"
                        sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                            def lastCommit = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                            echo "[DEBUG] Commit before merge: ${lastCommit}"

                            if (!lastCommit.startsWith("JENKINS AUTO MERGE -")) {
                                sh """
                                    git config user.name 'jenkins'
                                    git config user.email 'jenkins@example.com'
                                    git checkout main
                                    git pull origin main
                                    git merge --no-ff feature-${BUILD_NUMBER} -m 'JENKINS AUTO MERGE - CI: Auto-merge feature branch to main after successful pipeline'
                                    git push origin main
                                """
                                MERGE_SUCCESS = 'true'
                                echo "[DEBUG] MERGE_SUCCESS=true"
                            } else {
                                echo "[INFO] Merge skipped - already auto-merged"
                            }
                        }
                    } catch (e) {
                        echo "[ERROR] Merge failed: ${e.message}"
                        MERGE_SUCCESS = 'false'
                        echo "[DEBUG] MERGE_SUCCESS=false"
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

                echo "[DEBUG] Sending Slack notification with DEPLOY_SUCCESS=${deployStatus}, MERGE_SUCCESS=${mergeStatus}"

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
