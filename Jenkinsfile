pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds() // Prevent multiple builds at the same time
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Git repository URL over SSH
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"      // Dynamic feature branch per build
        DEPLOY_SUCCESS = 'false'                         // Deployment status as string for Jenkins env
        MERGE_SUCCESS = 'false'                          // Merge status as string for Jenkins env
        GIT_BRANCH = ''                                  // Ensure global env variable for branch
    }

    stages {
        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    echo "[DEBUG] Checking if last commit was a merge commit"
                    def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    echo "[DEBUG] Last commit message: ${lastCommitMessage}"
                    if (lastCommitMessage.startsWith("Merge remote-tracking branch")) {
                        echo "[INFO] Skipping build: Merge commit detected"
                        currentBuild.result = 'SUCCESS'
                        error("Stopping Pipeline: Merge commit detected.")
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "[DEBUG] Starting SSH Agent and verifying authentication."
                        sh 'ssh-add -l'
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "[INFO] SSH connection successful"
                            else
                                echo "[ERROR] SSH authentication failed"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                echo "[DEBUG] Checking out main branch"
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: REPO_URL,
                        credentialsId: 'Jenkins-GitHub-SSH'
                    ]]
                ])
            }
        }

        stage('Create Feature Branch') {
            when {
                expression {
                    echo "[DEBUG] Evaluating branch condition for creating feature branch: ${env.GIT_BRANCH}"
                    return !(env.GIT_BRANCH?.startsWith("feature-") ?: false)
                }
            }
            steps {
                script {
                    echo "[INFO] Creating feature branch ${BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}
                            git push origin ${BRANCH_NAME}
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Define GIT_BRANCH globally
                    echo "[DEBUG] env.GIT_BRANCH set to ${env.GIT_BRANCH}"
                }
            }
        }

        stage('Build') {
            steps {
                echo "[DEBUG] Setting up virtual environment and dependencies"
                sh '''
                    set -e
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
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
                    sleep 3
                    venv/bin/python -m pytest test_app.py
                    pkill gunicorn || true
                '''
            }
        }

        stage('Deploy') {
            when {
                expression {
                    echo "[DEBUG] Checking if Build and Test stages succeeded"
                    return currentBuild.result == null || currentBuild.result == 'SUCCESS'
                }
            }
            steps {
                script {
                    try {
                        echo "[INFO] Running deployment script"
                        sh '''
                            set -e
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        env.DEPLOY_SUCCESS = 'true'  // Important: Set globally
                        echo "[INFO] Deployment successful"
                    } catch (Exception e) {
                        env.DEPLOY_SUCCESS = 'false'
                        echo "[ERROR] Deployment failed: ${e.message}"
                        error("Deployment failed: ${e.message}")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    echo "[DEBUG] Evaluating conditions for merge:"
                    echo "[DEBUG] env.GIT_BRANCH=${env.GIT_BRANCH}, DEPLOY_SUCCESS=${env.DEPLOY_SUCCESS}"
                    def condition = env.GIT_BRANCH?.startsWith("feature-") && env.DEPLOY_SUCCESS == 'true'
                    echo "[DEBUG] Merge condition evaluated to: ${condition}"
                    return condition
                }
            }
            steps {
                script {
                    try {
                        echo "[INFO] Merging branch ${env.GIT_BRANCH} into main"
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh """
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main
                                git pull origin main
                                git merge --no-ff ${env.GIT_BRANCH}
                                git push origin main
                            """
                        }
                        env.MERGE_SUCCESS = 'true'
                        echo "[INFO] Merge successful"
                    } catch (Exception e) {
                        env.MERGE_SUCCESS = 'false'
                        echo "[ERROR] Merge failed: ${e.message}"
                        error("Merge failed: ${e.message}")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "[DEBUG] Preparing Slack notification"
                try {
                    def slack = load 'slack_notifications.groovy'
                    def message = slack.constructSlackMessage(
                        env.BUILD_NUMBER,
                        env.BUILD_URL,
                        env.MERGE_SUCCESS == 'true',
                        env.DEPLOY_SUCCESS == 'true'
                    )
                    def color = (env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true') ? "good" : "danger"
                    slack.sendSlackNotification(message, color)
                    echo "[INFO] Slack notification sent"
                } catch (Exception e) {
                    echo "[ERROR] Slack notification failed: ${e.message}"
                }
            }
        }
    }  // Close post block
}  // Close pipeline block

