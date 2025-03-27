pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds running together
        skipDefaultCheckout(true)  // We perform manual checkout to control branch
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub repo via SSH
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Use Jenkins SSH credentials
                    script {
                        echo "Authenticating GitHub with SSH"  // Debug message
                        sh "ssh-add -l"  // List SSH keys loaded
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

        stage('Checkout') {
            steps {
                script {
                    // Get the branch from env var provided by Jenkins
                    env.GIT_BRANCH = sh(script: "git symbolic-ref --short HEAD || git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                    env.BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Name for new feature branch
                    echo "Checking out from branch: ${env.GIT_BRANCH}"  // Log source branch

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.GIT_BRANCH}"]],  // Dynamic checkout
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
                    echo "Creating branch ${env.BRANCH_NAME}"  // Log branch creation
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
                sh '''
                    set -e  # Stop on failure
                    echo "Installing dependencies"
                    python3 -m venv venv  # Create Python virtual env
                    . venv/bin/activate  # Activate it
                    venv/bin/pip install --upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Stop on error
                    echo "Running tests"
                    . venv/bin/activate  # Activate venv
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Run app in background
                    sleep 3  # Wait for app to start
                    venv/bin/python -m pytest test_app.py  # Run tests
                    pkill gunicorn  # Kill server
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper
                    try {
                        sh 'bash deploy.sh'  // Run deployment
                        slack.sendSlackNotification(slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL), 'good')  // Send formatted message
                    } catch (err) {
                        slack.sendSlackNotification("*❌ Deployment failed for ${env.BRANCH_NAME}*", 'danger')  // Notify failure
                        error("Deployment failed!")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.BRANCH_NAME.startsWith("feature-") && currentBuild.result == null }  // Only merge if success
            }
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper
                    try {
                        echo "Merging ${env.BRANCH_NAME} to main"  // Log merge
                        sh """
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${env.BRANCH_NAME}
                            git push origin main
                        """
                        slack.sendSlackNotification("*✅ Merge completed successfully for ${env.BRANCH_NAME}*", 'good')  // Notify success
                    } catch (err) {
                        slack.sendSlackNotification("*❌ Merge failed for ${env.BRANCH_NAME}*", 'danger')  // Notify failure
                        error("Merge failed!")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack
                try {
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build summary
                    slack.sendSlackNotification(message, 'good')  // Send success message
                } catch (e) {
                    echo "Slack summary error: ${e.message}"  // Log if failed
                }
            }
        }
    }  // Close post block
}  // Close pipeline block
