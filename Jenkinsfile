pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds() // Prevent multiple builds running at the same time
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Create a unique feature branch per build
    }

    stages {
        stage('Start SSH Agent') { // Start SSH agent for authentication
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l" // List SSH keys
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

        stage('Checkout') { // Checkout the main branch
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

        stage('Create Feature Branch') { // Create and push a feature branch
            when {
                expression {
                    return env.GIT_BRANCH == null || !env.GIT_BRANCH.startsWith("feature-") // Only create a feature branch if not already running on one
                }
            }
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  // Create a new feature branch
                            git push origin ${BRANCH_NAME}  // Push it to GitHub
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME
                }
            }
        }

        stage('Build') { // Install dependencies
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

        stage('Test') { // Run tests
            steps {
                sh """
                    set -e
                    echo "Running API tests."
                    . venv/bin/activate
                    venv/bin/python -m pytest test_app.py
                """
            }
        }

        stage('Merge to Main') { // Merge the feature branch if tests pass
            when {
                expression {
                    return env.GIT_BRANCH && env.GIT_BRANCH.startsWith("feature-") // Only merge if it is a feature branch
                }
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."
                    
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Tests passed, merging ${env.GIT_BRANCH} back to main..."
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh """
                                git checkout main  // Switch to main
                                git pull origin main  // Fetch latest changes
                                git merge --no-ff ${env.GIT_BRANCH}  // Merge the feature branch
                                git push origin main  // Push to GitHub
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
        success { // Actions after successful pipeline
            script {
                echo "Build & Tests passed. Merging branch automatically."
            }
        }
        failure { // Actions if pipeline fails
            script {
                echo "Build or Tests failed. NOT merging to main."
            }
        }
        always { // Actions that always run
            script {
                try {
                    def slack = load 'slack_notifications.groovy' // Load Slack notification script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL) // Construct Slack message
                    slack.sendSlackNotification(message, "good") // Send Slack notification
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}" // Handle Slack errors
                }
            }
        }
    }
}

