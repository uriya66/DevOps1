
pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds() // Prevent concurrent builds
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'
        FEATURE_BRANCH = "feature-${env.BUILD_NUMBER}"
        DEPLOY_SUCCESS = 'false'
        MERGE_SUCCESS = 'false'
    }

    stages {
        stage('Skip Merge Commits') {
            steps {
                script {
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
                        # Force push current commit to feature-test
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

                sshagent(['Jenkins-GitHub-SSH']) {
                    sh '''
                        # Create a new branch feature-${BUILD_NUMBER} from feature-test
                        git checkout -b ${FEATURE_BRANCH}
                        git push origin ${FEATURE_BRANCH}
                    '''
                    env.GIT_BRANCH = FEATURE_BRANCH
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
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
                        sh 'chmod +x deploy.sh && ./deploy.sh'
                        DEPLOY_SUCCESS = 'true'
                    } catch (Exception e) {
                        DEPLOY_SUCCESS = 'false'
                        error("Deploy failed: ${e.message}")
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
