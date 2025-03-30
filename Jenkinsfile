pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Avoid concurrent builds
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Git repository URL
        FEATURE_BRANCH = "feature-${env.BUILD_NUMBER}"  // Dynamic feature branch per build
        DEPLOY_SUCCESS = 'false'
        MERGE_SUCCESS = 'false'
    }

    stages {
        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    if (lastCommitMessage.startsWith("Merge remote-tracking branch")) {
                        echo "Skipping redundant merge build."
                        currentBuild.result = 'SUCCESS'
                        error("Stopping: merge commit detected.")
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                sshagent(['Jenkins-GitHub-SSH']) {
                    sh '''
                        # Ensure all commits always go to feature-test
                        git checkout -B feature-test
                        git push -f origin feature-test
                    '''
                }

                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/feature-test']],
                    userRemoteConfigs: [[
                        url: REPO_URL,
                        credentialsId: 'Jenkins-GitHub-SSH'
                    ]]
                ])

                script {
                    sshagent(['Jenkins-GitHub-SSH']) {
                        sh """
                            # Create new isolated feature branch for current build
                            git checkout -b ${FEATURE_BRANCH}
                            git push origin ${FEATURE_BRANCH}
                        """
                        env.GIT_BRANCH = FEATURE_BRANCH  // Store active branch name
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    # Create Python venv and install dependencies
                    set -e
                    python3 -m venv venv
                    . venv/bin/activate
                    pip install -U pip
                    pip install -r requirements.txt
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    # Ensure Gunicorn not already running, then test
                    set -e
                    pkill gunicorn || true
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    python -m pytest test_app.py
                    pkill gunicorn || true
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        sh '''
                            # Run deploy script
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
                expression { env.DEPLOY_SUCCESS == 'true' }
            }
            steps {
                script {
                    try {
                        sshagent(['Jenkins-GitHub-SSH']) {
                            sh '''
                                # Merge successful feature branch into main
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main
                                git pull origin main
                                git merge --no-ff ${FEATURE_BRANCH}
                                git push origin main
                            '''
                        }
                        env.MERGE_SUCCESS = 'true'
                    } catch (Exception e) {
                        env.MERGE_SUCCESS = 'false'
                        error("Merge failed: ${e.message}")
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
