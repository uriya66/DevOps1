pipeline {
    agent any // Use any available Jenkins agent

    options {
        disableConcurrentBuilds() // Avoid concurrent builds
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git' // Git repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Dynamic feature branch per build
        DEPLOY_SUCCESS = 'false' // Deployment status initialized as false
        MERGE_SUCCESS = 'false' // Merge status initialized as false
        GIT_BRANCH = '' // Placeholder for the actual git branch name
    }

    stages {

        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    if (lastCommitMessage.startsWith("Merge remote-tracking branch")) {
                        echo "Skipping redundant merge build."
                        currentBuild.result = 'SUCCESS'
                        error("Stopping pipeline - redundant merge commit detected.")
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
                                echo "SSH authentication successful"
                            else
                                echo "ERROR: SSH authentication failed"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout & Create Feature Branch') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Force pushing latest code to feature-test and creating feature-${env.BUILD_NUMBER}"
                        sh '''
                            git checkout -B feature-test
                            git push -f origin feature-test
                            git checkout -b ${BRANCH_NAME}
                            git push origin ${BRANCH_NAME}
                        '''
                        env.GIT_BRANCH = "${BRANCH_NAME}"
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    echo "Setting up virtualenv and installing dependencies"
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    pip install --upgrade pip
                    pip install -r requirements.txt
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Running tests with pytest"
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5001 app:app &  # Use port 5001 to avoid conflict
                    sleep 3
                    pytest test_app.py
                    pkill -f gunicorn
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        echo "Running deployment script"
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
                    return env.GIT_BRANCH.startsWith("feature-") && env.DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        try {
                            echo "Merging ${env.GIT_BRANCH} to main"
                            sh '''
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main
                                git pull origin main
                                git merge --no-ff ${GIT_BRANCH}
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
                    env.MERGE_SUCCESS == 'true',
                    env.DEPLOY_SUCCESS == 'true'
                )
                def color = (env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true') ? "good" : "danger"
                slack.sendSlackNotification(message, color)
            }
        }
    }  // Close post block
}  // Close pipeline block
