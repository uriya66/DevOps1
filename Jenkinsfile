pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple concurrent builds
        skipDefaultCheckout(true)  // Skip the default automatic checkout by Jenkins
    }

    triggers {
        pollSCM('* * * * *')  // Poll SCM every minute
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub repository SSH URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Name for dynamically created feature branch
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Use Jenkins stored SSH credentials
                    script {
                        echo "Authenticating GitHub with SSH"  // Debug message for SSH auth
                        sh "ssh-add -l"  // List loaded SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection OK"
                            else
                                echo "SSH connection failed!"
                                exit 1
                            fi
                        '''  // Test SSH connection
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Cloning from triggering branch: ${env.BRANCH_NAME}"  // Log the source branch
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.BRANCH_NAME}"]],  // Dynamically use the current branch from which the build was triggered
                        userRemoteConfigs: [[
                            url: REPO_URL,
                            credentialsId: 'Jenkins-GitHub-SSH'  // Credential for SSH
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating new branch ${BRANCH_NAME}"  // Logging new branch name
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create new local branch
                            git push origin ${BRANCH_NAME}  # Push to GitHub
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Set env var for later usage
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Stop the script on first error
                    echo "Preparing environment"
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if it doesn't exist
                    . venv/bin/activate  # Activate virtual environment
                    venv/bin/pip install --upgrade pip  # Upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn  # Install dependencies
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Exit if any command fails
                    echo "Running unit tests"
                    . venv/bin/activate  # Activate virtual environment
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start the Flask app in background
                    sleep 3  # Wait for app to initialize
                    venv/bin/python -m pytest test_app.py  # Run pytest
                    pkill gunicorn  # Stop the app
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith("feature-")  // Only merge if current branch is feature-*
                }
            }
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack functions
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Merging ${env.GIT_BRANCH} to main"  // Log
                        sh """
                            git checkout main  # Switch to main branch
                            git pull origin main  # Update local main
                            git merge --no-ff ${env.GIT_BRANCH}  # Merge feature branch
                            git push origin main  # Push updated main
                        """
                        slack.sendSlackNotification("Merge completed successfully for ${env.GIT_BRANCH}", "good")  // Notify success
                    } else {
                        echo "Merge skipped due to test failure"  // Log skipped merge
                        slack.sendSlackNotification("Merge failed for ${env.GIT_BRANCH}", "danger")  // Notify failure
                        error("Stopping after failed merge")  // Stop the pipeline
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return currentBuild.result == null || currentBuild.result == 'SUCCESS'  // Proceed only if success
                }
            }
            steps {
                sh "bash deploy.sh"  // Execute deployment script
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack functions
                    slack.sendSlackNotification("Deployment completed for ${env.GIT_BRANCH}", "good")  // Notify success
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack module
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build Slack message
                    slack.sendSlackNotification(message, "good")  // Send message
                } catch (Exception e) {
                    echo "Slack final notification failed: ${e.message}"  // Catch and log error
                }
            }
        }
    }
}
