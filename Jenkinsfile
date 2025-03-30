pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Avoid concurrent builds
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Git repository URL
        FEATURE_BRANCH = "feature-${env.BUILD_NUMBER}"  // Dynamic branch per build
        DEPLOY_SUCCESS = 'false'  // Track deploy success
        MERGE_SUCCESS = 'false'   // Track merge success
    }

    stages {
        stage('Skip Merge Commits') {
            steps {
                script {
                    // Skip builds triggered by merge commits to prevent loops
                    def commitMsg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    if (commitMsg.startsWith("Merge remote-tracking branch")) {
                        echo "Merge commit detected, skipping build."
                        currentBuild.result = 'SUCCESS'
                        error("Stopped: Merge commit")
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                sshagent(['Jenkins-GitHub-SSH']) {
                    sh '''
                        # Force push current commit to 'feature-test'
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
                            # Create a new isolated feature branch from feature-test
                            git checkout -b ${FEATURE_BRANCH}
                            git push origin ${FEATURE_BRANCH}
                        """
                        // Set GIT_BRANCH environment variable to use later in pipeline
                        env.GIT_BRANCH = FEATURE_BRANCH
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    # Setup Python virtual environment and install dependencies
                    set -e
                    python3 -m venv venv
                    . venv/bin/activate
                    pip install --upgrade pip
                    pip install -r requirements.txt
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    # Run Gunicorn and pytest to ensure application correctness
                    set -e
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    python -m pytest test_app.py
                    pkill gunicorn
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        sh '''
                            # Execute deployment script
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        DEPLOY_SUCCESS = 'true'
                    } catch (Exception e) {
                        DEPLOY_SUCCESS = 'false'
                        error("Deployment failed: ${e.message}")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression { DEPLOY_SUCCESS == 'true' }
            }
            steps {
                script {
                    try {
                        sshagent(['Jenkins-GitHub-SSH']) {
                            sh '''
                                # Merge current feature branch into main
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main
                                git pull origin main
                                git merge --no-ff ${FEATURE_BRANCH}
                                git push origin main
                            '''
                        }
                        MERGE_SUCCESS = 'true'
                    } catch (Exception e) {
                        MERGE_SUCCESS = 'false'
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
                    MERGE_SUCCESS == 'true',
                    DEPLOY_SUCCESS == 'true'
                )
                def color = (MERGE_SUCCESS == 'true' && DEPLOY_SUCCESS == 'true') ? "good" : "danger"
                slack.sendSlackNotification(message, color)
            }
        }
    }  // Close post block
}  // Close pipeline block
