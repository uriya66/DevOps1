pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple concurrent builds
        skipDefaultCheckout(true)  // Skip default automatic Git checkout
    }

    triggers {
        pollSCM('* * * * *')  // Poll SCM every minute
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH GitHub repository
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Use SSH credentials from Jenkins
                    script {
                        echo "Authenticating GitHub with SSH"  // Debug SSH auth
                        sh "ssh-add -l"  // List loaded SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection OK"
                            else
                                echo "SSH connection failed!"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Detect Branch') {
            steps {
                script {
                    // Detect which branch triggered the build
                    env.GIT_BRANCH = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                    env.BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Define new feature branch
                    echo "Detected triggering branch: ${env.GIT_BRANCH}"  // Log current branch
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Cloning from branch: ${env.GIT_BRANCH}"  // Log clone source
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.GIT_BRANCH}"]],  // Checkout correct branch
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
                    echo "Creating new branch: ${env.BRANCH_NAME}"  // Log creation
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${env.BRANCH_NAME}
                            git push origin ${env.BRANCH_NAME}
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e
                    echo "Preparing virtual environment"
                    python3 -m venv venv  # Create new venv
                    . venv/bin/activate
                    venv/bin/pip install --upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Running unit tests"
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start app for test
                    sleep 3
                    venv/bin/python -m pytest test_app.py
                    pkill gunicorn  # Stop app
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.GIT_BRANCH.startsWith("feature-") }  // Only merge feature branches
            }
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack functions
                    try {
                        echo "Merging ${env.BRANCH_NAME} to main"  // Log
                        sh """
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${env.BRANCH_NAME}
                            git push origin main
                        """
                        slack.sendSlackNotification("Merge completed successfully for ${env.BRANCH_NAME}", "good")  // Notify success
                    } catch (err) {
                        slack.sendSlackNotification("Merge failed for ${env.BRANCH_NAME}", "danger")  // Notify failure
                        error("Merge failed!")  // Stop pipeline
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }  // Only if passed
            }
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack library
                    try {
                        sh "bash deploy.sh"  // Run deploy script
                        slack.sendSlackNotification("Deployment completed for ${env.BRANCH_NAME}", "good")  // Notify success
                    } catch (err) {
                        slack.sendSlackNotification("Deployment failed for ${env.BRANCH_NAME}", "danger")  // Notify failure
                        error("Deployment failed!")  // Stop pipeline
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack lib
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build message
                    slack.sendSlackNotification(message, "good")  // Send summary
                } catch (Exception e) {
                    echo "Slack summary failed: ${e.message}"  // Log error
                }
            }
        }
    }
}

