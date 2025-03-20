pipeline {
    agent any

    options {
        disableConcurrentBuilds() // Ensure no concurrent builds are running
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git' // GitHub repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Create a feature branch using the build number
    }

    stages {
        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    // Get the last commit message
                    def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()

                    // Check if the commit is a merge commit and skip the build if true
                    if (lastCommitMessage.startsWith("Merge remote-tracking branch")) {
                        echo "Skipping build: This is a merge commit."
                        currentBuild.result = 'SUCCESS'
                        error("Stopping Pipeline: Merge commit detected.")
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l" // List available SSH keys
                        sh """
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH Connection successful."
                            else
                                echo "ERROR: SSH Connection failed!"
                                exit 1
                            fi
                        """ // Verify SSH authentication with GitHub
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                echo "Checking out the repository."
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']], // Fetch the main branch from remote
                    userRemoteConfigs: [[
                        url: REPO_URL, // Use the defined repository URL
                        credentialsId: 'Jenkins-GitHub-SSH' // Use SSH credentials
                    ]]
                ])
            }
        }

        stage('Create Feature Branch') {
            when {
                expression {
                    return !(env.GIT_BRANCH?.startsWith("feature-") ?: false) // Ensure we are not already in a feature branch
                }
            }
            steps {
                script {
                    sh "git checkout -b ${BRANCH_NAME}" // Create a new feature branch
                    sh "git push origin ${BRANCH_NAME}" // Push the new branch to remote repository
                    env.GIT_BRANCH = BRANCH_NAME // Update the GIT_BRANCH variable
                    echo "DEBUG: Updated GIT_BRANCH = ${env.GIT_BRANCH}" // Log the updated branch name
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e // Exit immediately if a command exits with a non-zero status
                    echo "Setting up Python virtual environment."
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi // Create a virtual environment if it does not exist
                    . venv/bin/activate // Activate the virtual environment
                    venv/bin/python -m pip install --upgrade pip // Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn // Install required dependencies
                """
            }
        }

        stage('Test') {
            steps {
                sh """
                    set -e // Exit immediately if a command exits with a non-zero status
                    echo "Running API tests."
                    . venv/bin/activate // Activate the virtual environment
                    venv/bin/python -m pytest test_app.py // Run all tests
                """
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    def cleanBranch = env.GIT_BRANCH?.replace("origin/", "") ?: "" // Remove "origin/" prefix if present
                    echo "DEBUG: Checking merge condition: env.GIT_BRANCH=${env.GIT_BRANCH}" // Print branch name before merging
                    return cleanBranch.startsWith("feature-") // Only merge if it's a feature branch
                }
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."
                    def cleanBranch = env.GIT_BRANCH.replace("origin/", "") // Ensure correct branch format

                    echo "Tests passed, merging ${env.GIT_BRANCH} back to main..."
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout main // Switch to the main branch
                            git pull origin main // Pull the latest changes from main
                            git merge --no-ff ${cleanBranch} || echo "Nothing to merge" // Merge the feature branch
                            git push origin main // Push the merged changes to remote repository
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                echo "Build & Tests passed. Merging branch automatically." // Log success message
            }
        }
        failure {
            script {
                echo "Build or Tests failed. NOT merging to main." // Log failure message
            }
        }
        always {
            script {
                try {
                    echo "Sending Slack notification..." // Indicate Slack notification is being sent
                    slackSend(
                        channel: '#jenkis_alerts',
                        color: currentBuild.result == 'SUCCESS' ? 'good' : 'danger', // Set color based on build status
                        message: "Build #${env.BUILD_NUMBER} - ${currentBuild.result}\nBranch: ${env.GIT_BRANCH}\nRepo: ${env.REPO_URL}"
                    ) // Send Slack notification with build details
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}" // Log Slack notification errors
                }
            }
        }
    }
}

