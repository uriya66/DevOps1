pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds() // Prevents multiple builds from running simultaneously
        skipStagesAfterUnstable() // Stops the pipeline if any stage fails
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Create a unique feature branch for each build
    }

    stages {
        stage('Start SSH Agent') { // Start SSH authentication for GitHub access
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) { // Use stored SSH credentials
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l" // List currently added SSH keys
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

        stage('Checkout') { // Checkout the main branch from GitHub
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

        stage('Create Feature Branch') { // Create a new feature branch for this build
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) { // Ensure SSH authentication persists
                        sh """
                            git checkout -b ${BRANCH_NAME} // Create a new branch
                            git push origin ${BRANCH_NAME} // Push the new branch to GitHub
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME // Store the branch name in environment variables
                }
            }
        }

        stage('Build') { // Install dependencies and prepare the environment
            steps {
                sh """
                    set -e
                    echo "Setting up Python virtual environment."
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi // Create a virtual environment if it does not exist
                    . venv/bin/activate // Activate the virtual environment
                    venv/bin/python -m pip install --upgrade pip // Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn // Install dependencies
                """
            }
        }

        stage('Test') { // Run tests to ensure code stability
            steps {
                sh """
                    set -e
                    echo "Running API tests."
                    . venv/bin/activate // Activate virtual environment
                    venv/bin/python -m pytest test_app.py // Run all tests
                """
            }
        }

        stage('Merge to Main') { // Merge the feature branch into main if tests pass
            when {
                expression {
                    // Extract the branch name and check if it starts with 'feature-'
                    def branchName = env.GIT_BRANCH.replace("origin/", "")
                    echo "Current Branch after cleanup: ${branchName}"
                    return branchName.startsWith("feature-") // Only merge if it is a feature branch
                }
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."
                    
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') { // Ensure the build was successful
                        echo "Tests passed, merging ${env.GIT_BRANCH} back to main..."
                        sh """
                            git checkout main // Switch to the main branch
                            git pull origin main // Fetch the latest changes from main
                            git merge --no-ff ${env.GIT_BRANCH} // Merge the feature branch
                            git push origin main // Push the updated main branch to GitHub
                        """
                    } else {
                        echo "Tests failed, skipping merge!" // Do not merge if tests failed
                    }
                }
            }
        }
    }

    post {
        success { // Actions to perform if the pipeline is successful
            script {
                echo "Build & Tests passed. Merging branch automatically."
            }
        }
        failure { // Actions to perform if the pipeline fails
            script {
                echo "Build or Tests failed. NOT merging to main."
            }
        }
        always { // Actions that always run, regardless of success or failure
            script {
                try {
                    def slack = load 'slack_notifications.groovy' // Load Slack notification script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL) // Construct message
                    slack.sendSlackNotification(message, "good") // Send Slack notification
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}" // Handle Slack errors
                }
            }
        }
    }
}

