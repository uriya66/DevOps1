pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple concurrent builds
        skipDefaultCheckout(true)  // Skip default automatic Git checkout
    }

    triggers {
        pollSCM('* * * * *')  // Poll GitHub every minute
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repository
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Use SSH credentials
                    script {
                        echo "Authenticating GitHub with SSH"  // Log SSH auth
                        sh "ssh-add -l"  // List loaded keys
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
                    env.BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Define feature branch
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
                        branches: [[name: "*/${env.GIT_BRANCH}"]],  // Use triggering branch
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
                    echo "Creating branch: ${env.BRANCH_NAME}"  // Log branch creation
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
                    set -e  # Stop on error
                    echo "Creating venv and installing packages"
                    python3 -m venv venv  # Create venv
                    . venv/bin/activate  # Activate
                    venv/bin/pip install --upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Stop on error
                    echo "Running tests"
                    . venv/bin/activate  # Activate venv
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start app for tests
                    sleep 3  # Wait
                    venv/bin/python -m pytest test_app.py  # Run tests
                    pkill gunicorn  # Stop app
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    slack = load 'slack_notifications.groovy'  // Load once here
                    try {
                        sh "bash deploy.sh"  // Run deployment script
                        def deployMessage = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build Slack message
                        slack.sendSlackNotification("*🚀 Deployment Succeeded!*\n" + deployMessage, "good")  // Notify success
                    } catch (err) {
                        slack.sendSlackNotification("*❌ Deployment Failed:* ${err.message}", "danger")  // Notify failure
                        error("Deployment failed!")  // Stop pipeline
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.GIT_BRANCH.startsWith("feature-") }  // Only merge feature branches
            }
            steps {
                script {
                    try {
                        echo "Merging ${env.BRANCH_NAME} to main"  // Log merge
                        sh """
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${env.BRANCH_NAME}
                            git push origin main
                        """
                        def mergeMessage = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build Slack message
                        slack.sendSlackNotification("*🔀 Merge Completed!*\n" + mergeMessage, "good")  // Notify merge
                    } catch (err) {
                        slack.sendSlackNotification("*❌ Merge Failed:* ${err.message}", "danger")  // Notify failure
                        error("Merge failed!")  // Stop pipeline
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper
                    def finalMessage = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build summary
                    slack.sendSlackNotification("*✅ Jenkins Build Completed!*\n" + finalMessage, "good")  // Send summary
                } catch (e) {
                    echo "Final Slack notification failed: ${e.message}"  // Log error
                }
            }
        }
    }  // Close post block
}  // Close pipeline block
