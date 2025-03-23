pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds() // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Define GitHub repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Ensure branch name is globally available
        GIT_BRANCH = "feature-${env.BUILD_NUMBER}"  // Force GIT_BRANCH to always be a new feature branch, regardless of source branch
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l"
                        sh """
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH Connection successful."
                            else
                                echo "ERROR: SSH Connection failed!"
                                exit 1
                            fi
                        """
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
                        branches: [[name: '*/main']],  // Fetch the main branch
                        userRemoteConfigs: [[
                            url: REPO_URL,  // Use SSH URL for authentication
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}   # Create new feature branch from main
                            git push origin ${BRANCH_NAME}   # Push the new branch to GitHub
                        """
                    }

                    // Update Jenkins environment variable to track current feature branch
                    env.GIT_BRANCH = BRANCH_NAME  // Explicitly set GIT_BRANCH to the new branch name
                }

                // Stop further stages if we just created the branch
                // This forces Jenkins to exit this pipeline and re-trigger from webhook on new branch
                script {
                    currentBuild.result = 'SUCCESS'   // Mark build as successful
                    echo "Feature branch created. Exiting current pipeline to re-trigger from new branch..."
                    return                          // Exit pipeline here to allow new build to trigger on new branch
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e
                    echo "Setting up Python virtual environment."
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Test') {
            steps {
                sh """
                    set -e
                    echo "Running API tests."
                    . venv/bin/activate
                    venv/bin/python -m pytest test_app.py
                """
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith("feature-") ?: false // Merge only if it’s a feature branch
                }
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."

                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Tests passed, merging ${env.GIT_BRANCH} back to main..."

                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh """
                                git config user.name "jenkins"   # Set git username for merging
                                git config user.email "jenkins@example.com"  # Set git email
                                git checkout main                # Switch to main branch
                                git pull origin main             # Ensure main is up-to-date
                                git merge --no-ff ${env.GIT_BRANCH}   # Merge feature branch
                                git push origin main             # Push merge result to GitHub
                            """
                        }
                    } else {
                        echo "Tests failed, skipping merge!"
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                echo "Build & Tests passed. Merging branch automatically."
            }
        }
        failure {
            script {
                echo "Build or Tests failed. NOT merging to main."
            }
        }
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'   // Load external Slack script for notifications
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL) // Create message body
                    slack.sendSlackNotification(message, "good")    // Send success/failure notification
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}" // Handle Slack errors silently
                }
            }
        }
    }
}

