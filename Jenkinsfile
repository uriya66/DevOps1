pipeline {
    agent any  // Runs the pipeline on any available Jenkins agent

    environment {
        REPO_URL = 'https://github.com/uriya66/DevOps1.git'  // Use HTTPS instead of SSH
        CREDENTIALS_ID = 'Jenkins-GitHub-Token'  // Ensure you are using a valid PAT credential
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Load SSH credentials
                    script {
                        echo "Starting SSH Agent and verifying authentication."

                        // Print SSH_AUTH_SOCK to confirm it's correctly set
                        sh "echo 'SSH_AUTH_SOCK is: $SSH_AUTH_SOCK'"

                        // Ensure the SSH key is loaded in the agent
                        sh "ssh-add -l"

                        // Test SSH connection to GitHub
                        sh "ssh -T git@github.com || echo 'SSH Connection failed!'"
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
                            url: env.REPO_URL,  // Use HTTPS instead of SSH
                            credentialsId: env.CREDENTIALS_ID  // Use GitHub PAT token instead of SSH
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    def newBranch = "feature-${env.BUILD_NUMBER}"  // Create a feature branch
                    echo "Creating a new feature branch: ${newBranch}"

                    // Ensure the correct SSH authentication socket is used
                    withEnv(["SSH_AUTH_SOCK=${env.HOME}/.ssh/ssh-agent.sock"]) {
                        sh """
                            git checkout -b ${newBranch}  // Create a new branch
                            git push https://github.com/uriya66/DevOps1.git ${newBranch}  // Push the new branch using HTTPS
                        """
                    }
                    env.GIT_BRANCH = newBranch  // Store the branch name
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e  // Exit immediately if a command exits with a non-zero status
                    echo "Setting up Python virtual environment."

                    // Check if virtual environment directory exists, if not, create one
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi

                    // Activate the virtual environment
                    . venv/bin/activate

                    // Upgrade pip and install necessary dependencies
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Test') {
            steps {
                sh """
                    set -e  // Exit immediately if a command exits with a non-zero status
                    echo "Running API tests."

                    // Activate the virtual environment before running tests
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
                        git checkout main  // Switch to the main branch
                        git pull https://github.com/uriya66/DevOps1.git main  // Fetch latest changes using HTTPS
                        git merge --no-ff ${env.GIT_BRANCH}  // Merge feature branch into main
                        git push https://github.com/uriya66/DevOps1.git main  // Push updated main branch
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

