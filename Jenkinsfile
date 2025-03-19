pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Define the GitHub repository URL
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                // Start SSH Agent using Jenkins credentials (Ensure 'Jenkins-GitHub-SSH' exists in Jenkins)
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."

                        // Print SSH_AUTH_SOCK to verify it's correctly set
                        sh "echo 'SSH_AUTH_SOCK is: $SSH_AUTH_SOCK'"

                        // Ensure the SSH key is loaded in the agent
                        sh "ssh-add -l"

                        // Test SSH connection to GitHub
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
                    // Checkout source code using SSH authentication
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],  // Fetch the main branch
                        userRemoteConfigs: [[
                            url: REPO_URL,  // Use SSH URL for authentication
                            credentialsId: 'Jenkins-GitHub-SSH'  // Ensure the correct credentials are used
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    // Generate a feature branch name based on the build number
                    def newBranch = "feature-${env.BUILD_NUMBER}"
                    echo "Creating a new feature branch: ${newBranch}"

                    // Use SSH_AUTH_SOCK for secure authentication
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${newBranch}  # Create a new feature branch
                            git push origin ${newBranch}  # Push the new branch to GitHub
                        """
                    }
                    env.GIT_BRANCH = newBranch  // Store the branch name for later use
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Exit immediately if a command fails
                    echo "Setting up Python virtual environment."

                    # Check if virtual environment directory exists, if not, create it
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi

                    # Activate the virtual environment
                    . venv/bin/activate

                    # Upgrade pip and install dependencies
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Test') {
            steps {
                sh """
                    set -e  # Exit immediately if a command fails
                    echo "Running API tests."

                    # Activate the virtual environment before running tests
                    . venv/bin/activate
                    venv/bin/python -m pytest test_app.py
                """
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.GIT_BRANCH.startsWith("feature-") }  // Only merge if it's a feature branch
            }
            steps {
                script {
                    echo "Merging ${env.GIT_BRANCH} back to main."

                    sh """
                        git checkout main  # Switch to the main branch
                        git pull origin main  # Fetch the latest changes
                        git merge --no-ff ${env.GIT_BRANCH}  # Merge feature branch into main
                        git push origin main  # Push the updated main branch
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    // Send Slack notification after every build
                    def slack = load 'slack_notifications.groovy'
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)
                    slack.sendSlackNotification(message, "good")
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"
                }
            }
        }
    }
}

