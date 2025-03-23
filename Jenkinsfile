pipeline {
    agent any  // Run on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent simultaneous builds
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repo URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Dynamic branch name
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l"  // List loaded SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH Connection successful."
                            else
                                echo "ERROR: SSH Connection failed!"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository."
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],  // Checkout main branch
                        userRemoteConfigs: [[
                            url: REPO_URL,
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            when {
                expression {
                    return env.BRANCH_NAME?.startsWith("feature-")  // Only for feature branches
                }
            }
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            git checkout -b ${BRANCH_NAME}  // Create new feature branch
                            git push origin ${BRANCH_NAME}  // Push to GitHub
                        '''
                    }

                    env.GIT_BRANCH = BRANCH_NAME  // Save branch name for later stages
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    echo "Setting up Python virtual environment."
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate  // Activate venv
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Starting Flask app for testing..."
                    . venv/bin/activate
                    sleep 3
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  // Start the app
                    echo "Running API tests."
                    venv/bin/python -m pytest test_app.py
                '''
            }
        }

        stage('Merge and Deploy') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith("feature-")  // Only continue for feature branches
                }
            }
            steps {
                script {
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Tests passed, merging ${env.GIT_BRANCH} into main and deploying."

                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh '''
                                git config user.name "jenkins"  // Set Git user
                                git config user.email "jenkins@example.com"  // Set Git email
                                git checkout main  // Checkout main branch
                                git pull origin main  // Update local main
                                git merge --no-ff ${GIT_BRANCH}  // Merge the feature branch
                                git push origin main  // Push merged changes

                                echo "Starting deployment..."
                                chmod +x deploy.sh
                                ./deploy.sh  // Run deployment script
                            '''
                        }
                    } else {
                        echo "Tests failed, skipping merge and deploy."
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                echo "Build and tests passed successfully."
            }
        }
        failure {
            script {
                echo "Build or tests failed."
            }
        }
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)
                    slack.sendSlackNotification(message, "good")
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"
                }
            }
        }
    }
}
