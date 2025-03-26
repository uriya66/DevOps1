pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple concurrent builds
        skipDefaultCheckout(true)  // Skip default Git checkout
    }

    triggers {
        pollSCM('* * * * *')  // Check Git every minute
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH URL to repo
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Dynamic feature branch
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Authenticating GitHub with SSH"  // Message
                        sh "ssh-add -l"  // Show SSH key
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection OK"
                            else
                                echo "SSH connection failed!"
                                exit 1
                            fi
                        '''  // Verify GitHub connection
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Cloning from main branch"  // Log clone
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "${env.GIT_BRANCH}"]],  // Pull from the last push
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
                    echo "Creating new branch ${BRANCH_NAME}"  // Log new branch
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create branch
                            git push origin ${BRANCH_NAME}  # Push to GitHub
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Save current branch
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Stop on error
                    echo "Preparing environment"
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if needed
                    . venv/bin/activate  # Activate venv
                    venv/bin/pip install --upgrade pip  # Upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn  # Install dependencies
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Exit on error
                    echo "Running unit tests"
                    . venv/bin/activate  # Activate environment
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start app
                    sleep 3  # Wait for it to start
                    venv/bin/python -m pytest test_app.py  # Run tests
                    pkill gunicorn  # Stop app
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH.startsWith("feature-")  // Merge only feature branches
                }
            }
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack lib
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Merging ${env.GIT_BRANCH} to main"  // Log merge
                        sh """
                            git checkout main  # Switch
                            git pull origin main  # Update
                            git merge --no-ff ${env.GIT_BRANCH}  # Merge
                            git push origin main  # Push
                        """
                        slack.sendSlackNotification("Merge completed successfully for ${env.GIT_BRANCH}", "good")  // Slack merge OK
                    } else {
                        echo "Merge skipped due to test failure"  // Log skip
                        slack.sendSlackNotification(" Merge failed for ${env.GIT_BRANCH}", "danger")  // Slack merge fail
                        error("Stopping after failed merge")  // Stop pipeline
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return currentBuild.result == null || currentBuild.result == 'SUCCESS'  // Only if passed
                }
            }
            steps {
                sh "bash deploy.sh"  // Run deploy script
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack lib
                    slack.sendSlackNotification("✅ Deployment completed for ${env.GIT_BRANCH}", "good")  // Notify
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
                    slack.sendSlackNotification(message, "good")  // Send final message
                } catch (Exception e) {
                    echo "Slack final notification failed: ${e.message}"  // Log error
                }
            }
        }
    }
}

