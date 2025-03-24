pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repo URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Dynamic feature branch name based on build number
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Use SSH credentials for GitHub
                    script {
                        echo "Starting SSH Agent and verifying authentication."  // Print start message
                        sh "ssh-add -l"  // List loaded SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH Connection successful."  // Confirm SSH connection
                            else
                                echo "ERROR: SSH Connection failed!"  // Report SSH failure
                                exit 1  // Exit pipeline if SSH fails
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository."  // Log checkout start
                    checkout([
                        $class: 'GitSCM',  // Use Git plugin
                        branches: [[name: '*/main']],  // Checkout main branch
                        userRemoteConfigs: [[
                            url: REPO_URL,  // Use the GitHub SSH repo URL
                            credentialsId: 'Jenkins-GitHub-SSH'  // Use Jenkins credentials ID
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"  // Print new branch name
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {  // Provide SSH agent to shell
                        sh '''
                            git checkout -b ${BRANCH_NAME}  # Create new feature branch
                            git push origin ${BRANCH_NAME}  # Push it to remote GitHub
                        '''
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Save the branch name for later use
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
    }

    post {
        success {
            script {
                echo "Build and tests passed successfully."

                // Only feature branches should be merged
                if (env.GIT_BRANCH?.startsWith("feature-")) {
                    def mergeSuccess = true
                    def deploySuccess = true
                    def slack = load 'slack_notifications.groovy'

                    try {
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh '''
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main
                                git pull origin main
                                git merge --no-ff ${GIT_BRANCH}
                                git push origin main
                            '''
                        }
                        echo "Merge completed successfully."
                    } catch (Exception mergeError) {
                        echo "Merge failed: ${mergeError.message}"
                        mergeSuccess = false
                    }

                    if (mergeSuccess) {
                        try {
                            echo "Starting deployment after merge..."
                            sh '''
                                chmod +x deploy.sh
                                ./deploy.sh
                            '''
                            echo "Deployment script executed successfully."
                        } catch (Exception deployError) {
                            echo "Deployment failed: ${deployError.message}"
                            deploySuccess = false
                        }
                    }

                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, mergeSuccess, deploySuccess)
                    def statusColor = (mergeSuccess && deploySuccess) ? "good" : "danger"
                    slack.sendSlackNotification(message, statusColor)
                }
            }
        }

        failure {
            script {
                echo "Build or tests failed."
                try {
                    def slack = load 'slack_notifications.groovy'
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)
                    slack.sendSlackNotification(message, "danger")
                } catch (Exception e) {
                    echo "Error sending Slack failure message: ${e.message}"
                }
            }
        }
    }
}

