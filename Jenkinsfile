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
                        '''  // Test SSH connection
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
                            credentialsId: 'Jenkins-GitHub-SSH'  // Use Jenkins stored SSH key
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
                            git checkout -b ${env.BRANCH_NAME}  # Create new feature branch
                            git push origin ${env.BRANCH_NAME}  # Push to remote
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Stop on first error
                    echo "Preparing virtual environment"  # Log start
                    python3 -m venv venv  # Create new venv
                    . venv/bin/activate  # Activate venv
                    venv/bin/pip install --upgrade pip  # Upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn  # Install dependencies
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Stop on error
                    echo "Running unit tests"  # Log test phase
                    . venv/bin/activate  # Activate environment
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start app in background
                    sleep 3  # Wait for app to start
                    venv/bin/python -m pytest test_app.py  # Run tests
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
                        echo "Merging ${env.BRANCH_NAME} to main"  // Log merge
                        sh """
                            git checkout main  # Switch to main
                            git pull origin main  # Sync main
                            git merge --no-ff ${env.BRANCH_NAME}  # Merge feature
                            git push origin main  # Push updated main
                        """
                        slack.sendSlackNotification("*✅ Merge completed successfully for `${env.BRANCH_NAME}`*", "good")  // Notify success
                    } catch (err) {
                        slack.sendSlackNotification("*❌ Merge failed for `${env.BRANCH_NAME}`: ${err.message}*", "danger")  // Notify failure
                        error("Merge failed!")  // Fail pipeline
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }  // Run only if passed
            }
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack logic
                    try {
                        sh "bash deploy.sh"  // Run deploy script
                        slack.sendSlackNotification("*🚀 Deployment completed for `${env.BRANCH_NAME}`*", "good")  // Notify success
                    } catch (err) {
                        slack.sendSlackNotification("*🔥 Deployment failed for `${env.BRANCH_NAME}`: ${err.message}*", "danger")  // Notify failure
                        error("Deployment failed!")  // Fail pipeline
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack again
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build final message
                    slack.sendSlackNotification(message, "good")  // Send summary
                } catch (Exception e) {
                    echo "Slack summary failed: ${e.message}"  // Print Slack error
                }
            }
        }
    }
}

