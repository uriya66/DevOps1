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
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l"  // List loaded SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH Connection successful."
                            else
                                echo "ERROR: SSH Connection failed!"
                                exit 1
                            fi
                        '''  // Verify GitHub SSH connection
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository."
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],  // Always checkout latest main
                        userRemoteConfigs: [[
                            url: REPO_URL,
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            when {
                expression {
                    return !(env.GIT_BRANCH?.startsWith("feature-") ?: false)  // Skip if already on feature branch
                }
            }
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            git checkout -b ${BRANCH_NAME}  # Create local feature branch
                            git push origin ${BRANCH_NAME}  # Push new branch to GitHub
                        '''
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Set the env var for later stages
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit immediately on error
                    echo "Setting up Python virtual environment."
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if not exist
                    . venv/bin/activate  # Activate venv
                    venv/bin/python -m pip install --upgrade pip  # Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn  # Install dependencies
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Exit on error
                    echo "Starting Flask app for testing..."
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start Flask app
                    sleep 3  # Wait for app to initialize
                    echo "Running API tests."
                    venv/bin/python -m pytest test_app.py  # Run tests
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith("feature-") ?: false  // Only merge if feature branch
                }
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Tests passed, merging ${env.GIT_BRANCH} into main."

                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh '''
                                git config user.name "jenkins"  # Configure Git user
                                git config user.email "jenkins@example.com"  # Configure Git email
                                git fetch origin main  # Fetch latest main
                                git checkout main  # Switch to main branch
                                git reset --hard origin/main  # Reset to origin/main to avoid local conflicts
                                git merge --no-ff ${GIT_BRANCH}  # Merge feature branch
                                git push origin main  # Push merged changes
                            '''
                        }

                        // Force update of GIT_BRANCH so Deploy stage will run
                        env.GIT_BRANCH = "main"  // Update GIT_BRANCH to 'main' after successful merge
                    } else {
                        echo "Tests failed, skipping merge!"
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return env.GIT_BRANCH == 'main'  // Only deploy if we are now on main branch
                }
            }
            steps {
                sh '''
                    echo "Starting deployment..."
                    chmod +x deploy.sh  # Ensure script is executable
                    ./deploy.sh  # Execute deployment
                    if [ $? -eq 0 ]; then
                        echo "Deployment script executed successfully."
                    else
                        echo "Deployment script failed."
                        exit 1
                    fi
                '''
            }
        }
    }

    post {
        success {
            script {
                echo "Build and tests passed successfully."  // Post success message
            }
        }
        failure {
            script {
                echo "Build or tests failed."  // Post failure message
            }
        }
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack notification script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build Slack message
                    slack.sendSlackNotification(message, "good")  // Send notification
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"  // Catch any errors
                }
            }
        }
    }
}

