pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repo URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Dynamic feature branch name
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l"
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
                        branches: [[name: '*/main']],  // Always base on main
                        userRemoteConfigs: [[
                            url: REPO_URL,
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            git checkout -b ${BRANCH_NAME}  # Create local feature branch
                            git push origin ${BRANCH_NAME}  # Push to GitHub
                        '''
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Save it for later use
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    echo "Setting up Python virtual environment."
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
                    echo "Starting Flask app for testing..."
                    . venv/bin/activate
                    sleep 3
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    echo "Running API tests."
                    venv/bin/python -m pytest test_app.py
                '''
            }
        }

        stage('Merge & Deploy') {
            when {
                expression {
                    // Only run this step if the build is running on a feature branch
                    return env.GIT_BRANCH?.startsWith("feature-") ?: false
                }
            }
            steps {
                script {
                    echo "Merging ${env.GIT_BRANCH} into main and deploying."

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            # Set git identity for automated commit
                            git config user.name "jenkins"
                            git config user.email "jenkins@example.com"

                            # Ensure we're on main and up to date
                            git checkout main
                            git pull origin main

                            # Merge the feature branch into main
                            git merge --no-ff ${GIT_BRANCH} -m "Auto-merge ${GIT_BRANCH} into main by Jenkins"

                            # Push the changes
                            git push origin main

                            echo "Merge completed. Starting deploy..."

                            chmod +x deploy.sh
                            ./deploy.sh

                            if [ $? -eq 0 ]; then
                                echo "Deployment completed successfully."
                            else
                                echo "Deployment failed."
                                exit 1
                            fi
                        '''
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

