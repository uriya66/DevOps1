
pipeline {
    agent any

    options {
        disableConcurrentBuilds() // No concurrent builds allowed
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git' // The repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Create feature branch with the build number
    }

    stages {
        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
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
                    branches: [[name: '*/main']],  // Fetch the main branch from remote
                    userRemoteConfigs: [[
                        url: REPO_URL,  // Use the defined repository URL
                        credentialsId: 'Jenkins-GitHub-SSH' // Use SSH credentials
                    ]]
                ])
            }
        }

        stage('Create Feature Branch') {
            when {
                expression {
                    return !(env.GIT_BRANCH?.startsWith("feature-") ?: false) // Only create a feature branch if not already in one
                }
            }
            steps {
                script {
                    git checkout -b ${BRANCH_NAME} // Create a new feature branch
                    git push origin ${BRANCH_NAME} // Push the new branch to remote
                    env.GIT_BRANCH = BRANCH_NAME // Update GIT_BRANCH to the newly created branch

                    echo "DEBUG: Updated GIT_BRANCH = ${env.GIT_BRANCH}" // Print the updated branch name
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e // Stop execution on error
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
                    set -e // Stop execution on error
                    echo "Running API tests."
                    . venv/bin/activate // Activate the virtual environment
                    venv/bin/python -m pytest test_app.py // Run the tests
                """
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    def cleanBranch = env.GIT_BRANCH?.replace("origin/", "") ?: ""
                    echo "DEBUG: Checking merge condition: env.GIT_BRANCH=${env.GIT_BRANCH}" // Print the branch before merging
                    return cleanBranch.startsWith("feature-")
                }
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."

                    def cleanBranch = env.GIT_BRANCH.replace("origin/", "")
                   
                    echo "Tests passed, merging ${env.GIT_BRANCH} back to main..."

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout main // Switch to the main branch
                            git pull origin main // Pull the latest changes from main
                            git merge --no-ff ${cleanBranch} || echo "Nothing to merge" //
                            git push origin main // Push the merged changes to remote
                        """
                    }
                } else {
                        echo "Tests failed, skipping merge!" // Do not merge if tests failed
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

