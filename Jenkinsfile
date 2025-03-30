pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"
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
            when {
                expression {
                    return !(env.GIT_BRANCH?.startsWith("feature-") ?: false)
                }
            }
            steps {
                script {
                    echo "Creating feature branch ${env.BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${env.BRANCH_NAME}
                            git push origin ${env.BRANCH_NAME}
                        """
                    }
                    env.GIT_BRANCH = env.BRANCH_NAME
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    echo "Setting up virtualenv"
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Running Flask app for testing"
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    echo "Running pytest"
                    venv/bin/python -m pytest test_app.py
                    pkill gunicorn
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        sh '''
                            set -e
                            echo "Running deployment script"
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        // Set success flag using currentBuild object
                        currentBuild.description = "deploy=success"
                        currentBuild.displayName = "#${env.BUILD_NUMBER}"
                        currentBuild.deploySuccess = true
                    } catch (Exception e) {
                        currentBuild.deploySuccess = false
                        error("Deployment failed: ${e.message}")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return (env.GIT_BRANCH?.startsWith("feature-") && currentBuild.deploySuccess == true)
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
                        currentBuild.mergeSuccess = true
                    } catch (Exception e) {
                        currentBuild.mergeSuccess = false
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
                    def message = slack.constructSlackMessage(
                        env.BUILD_NUMBER,
                        env.BUILD_URL,
                        currentBuild.mergeSuccess == true,
                        currentBuild.deploySuccess == true
                    )
                    def color = (currentBuild.mergeSuccess == true && currentBuild.deploySuccess == true) ? "good" : "danger"
                    slack.sendSlackNotification(message, color)
                } catch (Exception e) {
                    echo "Slack notification error: ${e.message}"
                }
            }
        }
    }  // Close post block
}  // Close pipeline block

