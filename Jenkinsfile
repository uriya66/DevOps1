pipeline {
    agent any  // Use any available Jenkins agent for the build

    options {
        disableConcurrentBuilds() // Prevent concurrent builds of the same job
    }

    triggers {
        pollSCM('* * * * *') // Check for SCM changes every minute
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Git repository over SSH
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Create unique feature branch per build
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l" // List loaded SSH keys
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
                    echo "Cloning the repository"
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']], // Checkout from main branch
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
                    echo "Creating feature branch ${BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create local feature branch
                            git push origin ${BRANCH_NAME}  # Push feature branch to remote
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Save the new branch name to environment
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Stop on error
                    echo "Setting up virtualenv"
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if not exist
                    . venv/bin/activate  # Activate virtualenv
                    venv/bin/python -m pip install --upgrade pip  # Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn  # Install dependencies
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Stop the script on any error
                    echo "Running Flask app for testing..."

                    # Activate the virtual environment
                    . venv/bin/activate

                    # Start Gunicorn in the background on localhost
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &

                    # Give the server a moment to fully start
                    sleep 3

                    echo "Running Pytest tests..."
                    venv/bin/python -m pytest test_app.py

                    # Kill Gunicorn after tests complete
                    pkill gunicorn
                '''
            }
        }
        stage('Merge to Main') {
            when {
                expression {
                    def branchName = env.GIT_BRANCH.replace("origin/", "")  // Clean branch name
                    return branchName.startsWith("feature-")  // Only merge feature branches
                }
            }
            steps {
                script {
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Tests passed, merging branch"
                        sh """
                            git checkout main  # Switch to main
                            git pull origin main  # Update local main
                            git merge --no-ff ${env.GIT_BRANCH}  # Merge feature branch
                            git push origin main  # Push merged main
                        """
                        def slack = load 'slack_notifications.groovy'
                        slack.sendSlackNotification("Merge completed successfully for ${env.GIT_BRANCH}", "good") // Notify merge success
                    } else {
                        echo "Tests failed, merge skipped"
                        def slack = load 'slack_notifications.groovy'
                        slack.sendSlackNotification("Merge failed due to failed tests for ${env.GIT_BRANCH}", "danger") // Notify merge failure
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return currentBuild.result == null || currentBuild.result == 'SUCCESS'  // Only deploy on success
                }
            }
            steps {
                sh "bash deploy.sh" // Deploy application via shell script
                script {
                    def slack = load 'slack_notifications.groovy'
                    slack.sendSlackNotification("Deployment completed successfully for ${env.GIT_BRANCH}", "good") // Notify deploy success
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL) // Build detailed message
                    slack.sendSlackNotification(message, "good") // Send final build result
                } catch (Exception e) {
                    echo "Slack notification error: ${e.message}"  // Log Slack failure
                }
            }
        }
    }
}

