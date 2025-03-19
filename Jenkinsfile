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

                        // Test SSH connection (Ignoring "does not provide shell access" message)
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
                    // Checkout source code from GitHub repository using SSH authentication
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],  // Fetch the main branch
                        userRemoteConfigs: [[
                            url: 'git@github.com:uriya66/DevOps1.git',  // Use SSH URL for authentication
                            credentialsId: 'Jenkins-GitHub-SSH'  // Ensure the correct credentials are used
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    // Create a new branch based on the build number
                    def newBranch = "feature-${env.BUILD_NUMBER}"
                    echo "Creating a new feature branch: ${newBranch}"

                    // Use the correct SSH authentication socket to ensure secure Git operations
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${newBranch}  # Create a new feature branch
                            git push git@github.com:uriya66/DevOps1.git ${newBranch}  # Push the new branch to GitHub
                        """
                    }
                    env.GIT_BRANCH = newBranch  // Set the new branch as an environment variable
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  # Exit immediately if a command exits with a non-zero status
                    echo "Setting up Python virtual environment."

                    # Check if virtual environment directory exists, if not, create one
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi

                    # Activate the virtual environment
                    . venv/bin/activate

                    # Upgrade pip and install necessary dependencies
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Test') {
            steps {
                sh """
                    set -e  # Exit immediately if a command exits with a non-zero status
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
                        git pull git@github.com:uriya66/DevOps1.git main  # Get the latest changes
                        git merge --no-ff ${env.GIT_BRANCH}  # Merge the feature branch into main
                        git push git@github.com:uriya66/DevOps1.git main  # Push the updated main branch
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    // Load the Slack notification script and send a notification
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

