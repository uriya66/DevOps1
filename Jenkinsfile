pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds() // Prevent multiple builds at the same time
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
                        echo "Skipping build: Merge commit detected."
                        currentBuild.result = 'SUCCESS'
                        error("Stopping Pipeline: Merge commit.")
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh '''
                        # Check GitHub SSH authentication without failing on the expected response
                        ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"
                        if [ $? -eq 0 ]; then
                            echo "SSH authentication successful"
                        else
                            echo "SSH authentication failed"
                            exit 1
                        fi
                    '''
                }
            }
        }

        stage('Checkout & Create Feature Branch') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh '''
                        # Checkout to feature-test branch
                        git checkout feature-test

                        # Create new feature branch from feature-test
                        git checkout -b ${BRANCH_NAME}

                        # Push new feature branch to origin
                        git push origin ${BRANCH_NAME}
                    '''
                    script {
                        env.GIT_BRANCH = BRANCH_NAME
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    pip install --upgrade pip flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    GUNICORN_PID=$!
                    sleep 3
                    pytest test_app.py
                    kill $GUNICORN_PID
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        sh '''
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
                    return env.DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        try {
                            sh '''
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main
                                git pull origin main
                                git merge --no-ff ${BRANCH_NAME}
                                git push origin main
                            '''
                            env.MERGE_SUCCESS = 'true'
                        } catch (Exception e) {
                            env.MERGE_SUCCESS = 'false'
                            error("Merge failed: ${e.message}")
                        }
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
                    env.MERGE_SUCCESS,
                    env.DEPLOY_SUCCESS
                )
                def color = (env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true') ? "good" : "danger"
                slack.sendSlackNotification(message, color)
            }
        }
    }  // Close post block
}  // Close pipeline block
