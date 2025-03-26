pipeline {
    agent any  // Use any available Jenkins agent for the build

    options {
        disableConcurrentBuilds()  // Prevent concurrent builds from running simultaneously
        skipDefaultCheckout(true)  // Disable automatic checkout to allow custom steps
    }

    triggers {
        pollSCM('* * * * *')  // Check for changes in SCM every minute
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH URL to GitHub repository
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Generate dynamic branch name based on build number
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying access to GitHub"
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
                    echo "Cloning main branch of the repository"
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],  // Target branch
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
                    echo "Creating and pushing new feature branch"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create new local branch
                            git push origin ${BRANCH_NAME}  # Push new branch to GitHub
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Store current working branch
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Exit on error
                    echo "Setting up Python environment"
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create virtual environment
                    . venv/bin/activate  # Activate virtual environment
                    venv/bin/pip install --upgrade pip  # Upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn  # Install dependencies
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Exit on any error
                    echo "Running unit tests against Flask app"
                    . venv/bin/activate  # Activate virtual environment
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start Gunicorn in background
                    sleep 3  # Wait for server to be available
                    venv/bin/python -m pytest test_app.py  # Run tests
                    pkill gunicorn  # Stop server after tests
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH.startsWith("feature-")  // Only merge feature branches
                }
            }
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack script
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Merging branch ${env.GIT_BRANCH} into main"
                        sh """
                            git checkout main  # Switch to main branch
                            git pull origin main  # Pull latest changes
                            git merge --no-ff ${env.GIT_BRANCH}  # Merge without fast-forward
                            git push origin main  # Push merged main branch
                        """
                        slack.sendSlackNotification("Merge completed successfully for ${env.GIT_BRANCH}", "good")  // Send success message
                    } else {
                        echo "Merge skipped due to failed tests"
                        slack.sendSlackNotification("Merge failed due to errors for ${env.GIT_BRANCH}", "danger")  // Send failure message
                        error("Stopping pipeline due to test failures")  // Fail the build
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return currentBuild.result == null || currentBuild.result == 'SUCCESS'  // Proceed only if previous steps succeeded
                }
            }
            steps {
                sh "bash deploy.sh"  // Run deployment script
                script {
                    def slack = load 'slack_notifications.groovy'  // Load Slack script
                    slack.sendSlackNotification("Deployment completed for ${env.GIT_BRANCH}", "good")  // Notify deployment
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Create final message
                    slack.sendSlackNotification(message, "good")  // Send final report
                } catch (Exception e) {
                    echo "Slack post error: ${e.message}"  // Log any Slack errors
                }
            }
        }
    }
}

