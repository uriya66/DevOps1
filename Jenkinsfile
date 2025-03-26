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
                    env.GIT_BRANCH = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()  // Get triggering branch
                    env.BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Define new feature branch
                    echo "Detected branch: ${env.GIT_BRANCH}"  // Log branch
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out: ${env.GIT_BRANCH}"  // Log checkout
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.GIT_BRANCH}"]],
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
                    echo "Creating new branch: ${env.BRANCH_NAME}"  // Log branch creation
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${env.BRANCH_NAME}  # Create new branch
                            git push origin ${env.BRANCH_NAME}  # Push to remote
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Stop if any command fails
                    echo "Preparing virtual environment"
                    python3 -m venv venv  # Create venv
                    . venv/bin/activate  # Activate venv
                    venv/bin/pip install --upgrade pip  # Upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn  # Install dependencies
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Stop if any command fails
                    echo "Running unit tests"
                    . venv/bin/activate  # Activate venv
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start Gunicorn
                    sleep 3  # Wait for app
                    venv/bin/python -m pytest test_app.py  # Run tests
                    pkill gunicorn || true  # Kill Gunicorn
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack script
                    try {
                        sh "bash deploy.sh"  // Run deploy script
                        slack.sendSlackNotification(slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL), "good")  // Notify success
                    } catch (err) {
                        slack.sendSlackNotification("*❌ Deployment failed for ${env.BRANCH_NAME}*\nError: ${err.getMessage()}", "danger")  // Notify failure
                        error("Deployment failed!")  // Stop pipeline
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                allOf {
                    expression { env.GIT_BRANCH.startsWith("feature-") }  // Only for feature branches
                    expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }  // Only if successful
                }
            }
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack
                    try {
                        echo "Merging ${env.BRANCH_NAME} to main"  // Log
                        sh """
                            git checkout main  # Switch to main
                            git pull origin main  # Pull latest main
                            git merge --no-ff ${env.BRANCH_NAME}  # Merge feature branch
                            git push origin main  # Push main
                        """
                        slack.sendSlackNotification("*✅ Merge completed successfully for ${env.BRANCH_NAME}*", "good")  // Notify success
                    } catch (err) {
                        slack.sendSlackNotification("*❌ Merge failed for ${env.BRANCH_NAME}*\nError: ${err.getMessage()}", "danger")  // Notify failure
                        error("Merge failed!")  // Stop pipeline
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack script
                try {
                    def msg = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build Slack message
                    slack.sendSlackNotification(msg, "good")  // Send Slack message
                } catch (e) {
                    echo "Slack summary failed: ${e.message}"  // Log if Slack fails
                }
            }
        }
    }  // Close post block
}  // Close pipeline block
