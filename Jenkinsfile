pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"
        DEPLOY_SUCCESS = 'false'
        MERGE_SUCCESS = 'false'
    }

    stages {
        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    if (lastCommitMessage.startsWith("Merge remote-tracking branch")) {
                        echo "Skipping build: merge commit."
                        currentBuild.result = 'SUCCESS'
                        error("Stopping Pipeline: merge commit detected.")
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'
                }
            }
        }

        stage('Checkout Main') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[url: REPO_URL, credentialsId: 'Jenkins-GitHub-SSH']]
                ])
                script {
                    env.GIT_BRANCH = BRANCH_NAME
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh """
                        git checkout -b ${BRANCH_NAME}
                        git push origin ${BRANCH_NAME}
                    """
                }
                script {
                    env.GIT_BRANCH = BRANCH_NAME
                }
            }
        }

        stage('Build') {
            steps {
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
                        sh '''
                            set -e
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        env.DEPLOY_SUCCESS = 'true'
                    } catch(Exception e) {
                        env.DEPLOY_SUCCESS = 'false'
                        error "Deploy failed: ${e.message}"
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                script {
                    try {
                        sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                            sh """
                                git config user.name 'jenkins'
                                git config user.email 'jenkins@example.com'
                                git checkout main
                                git pull origin main
                                git merge ${env.GIT_BRANCH}
                                git push origin main
                            """
                        }
                        env.MERGE_SUCCESS = 'true'
                    } catch(Exception e) {
                        env.MERGE_SUCCESS = 'false'
                        error "Merge failed: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'
                def message = slack.constructSlackMessage(
                    env.BUILD_NUMBER,
                    env.BUILD_URL,
                    env.MERGE_SUCCESS == 'true',
                    env.DEPLOY_SUCCESS == 'true'
                )
                def color = (env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true') ? 'good' : 'danger'
                slack.sendSlackNotification(message, color)
            }
        }
    }  // Close post block
}  // Close pipeline block

