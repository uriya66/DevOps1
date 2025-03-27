pipeline {
    agent any  // Use any available Jenkins agent for the build

    options {
        disableConcurrentBuilds()  // Prevent concurrent builds
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Git repository over SSH
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Branch name for each build
        GIT_BRANCH = ''  // Will hold the real branch name
        DEPLOY_SUCCESS = 'false'  // Deployment status
        MERGE_SUCCESS = 'false'   // Merge status
    }

    stages {
        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    if (lastCommitMessage.startsWith("Merge remote-tracking branch")) {
                        echo "Skipping build: This is a merge commit."
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
                        echo "Starting SSH Agent and verifying authentication."
                        sh 'ssh-add -l'
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection successful"
                            else
                                echo "ERROR: SSH authentication failed"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
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
            steps {
                script {
                    echo "Creating feature branch ${BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}
                            git push origin ${BRANCH_NAME}
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e
                    echo "Setting up virtualenv"
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Running Flask app for testing..."
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    echo "Running pytest"
                    venv/bin/python -m pytest test_app.py
                    pkill gunicorn || true
                '''
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return currentBuild.result == null || currentBuild.result == 'SUCCESS'
                }
            }
            steps {
                script {
                    try {
                        echo "Running deployment script"
                        sh '''
                            set -e
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        env.DEPLOY_SUCCESS = 'true'
                    } catch (Exception e) {
                        env.DEPLOY_SUCCESS = 'false'
                        error("Deployment failed: ${e.message}")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    echo "DEBUG: GIT_BRANCH = ${env.GIT_BRANCH}"
                    echo "DEBUG: DEPLOY_SUCCESS = ${env.DEPLOY_SUCCESS}"
                    return env.GIT_BRANCH?.startsWith("feature-") && env.DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                script {
                    try {
                        echo "Merging ${env.GIT_BRANCH} into main"
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
                    } catch (Exception e) {
                        env.MERGE_SUCCESS = 'false'
                        error("Merge to main failed: ${e.message}")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'
                    def msg = slack.constructSlackMessage(
                        env.BUILD_NUMBER,
                        env.BUILD_URL,
                        env.MERGE_SUCCESS == 'true',
                        env.DEPLOY_SUCCESS == 'true'
                    )
                    slack.sendSlackNotification(
                        msg,
                        (env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true') ? "good" : "danger"
                    )
                } catch (e) {
                    echo "Slack notification failed: ${e.message}"
                }
            }
        }
    }  // Close post block
}  // Close pipeline block
