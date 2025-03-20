pipeline {
    agent any

    options {
        disableConcurrentBuilds() // Prevent multiple builds from running at the same time
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git' // The repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Generate a unique feature branch name
    }

    stages {
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
                script {
                    echo "Checking out the repository."
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']], // Fetch the main branch from remote
                        userRemoteConfigs: [[
                            url: REPO_URL, // Use the defined repository URL
                            credentialsId: 'Jenkins-GitHub-SSH' // Use SSH credentials
                        ]]
                    ])
                    
                    // Ensure GIT_BRANCH is set correctly without "origin/"
                    env.GIT_BRANCH = sh(
                        script: """
                            branch_name=\$(git rev-parse --abbrev-ref HEAD) // Get the current branch name
                            branch_name=\${branch_name#origin/} // Remove "origin/" prefix if it exists
                            echo \$branch_name // Print the cleaned branch name
                        """,
                        returnStdout: true
                    ).trim()

                    echo "DEBUG: Final GIT_BRANCH = ${env.GIT_BRANCH}" // Print the final branch name
                }
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
                    echo "Creating a new feature branch: ${BRANCH_NAME}"

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME} // Create a new feature branch
                            git push origin ${BRANCH_NAME} // Push the new branch to remote
                        """
                    }

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
                    echo "DEBUG: Checking merge condition: env.GIT_BRANCH=${env.GIT_BRANCH}" // Print the branch before merging
                    return env.GIT_BRANCH.startsWith("feature-") || env.GIT_BRANCH == "feature-test" // Merge only feature branches or "feature-test"
                }
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."

                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') { // Merge only if the build was successful
                        echo "Tests passed, merging ${env.GIT_BRANCH} back to main..."

                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh """
                                git checkout main // Switch to the main branch
                                git pull origin main // Pull the latest changes from main
                                git merge --no-ff ${env.GIT_BRANCH} // Merge the feature branch into main
                                git push origin main // Push the merged changes to remote
                            """
                        }
                    } else {
                        echo "Tests failed, skipping merge!" // Do not merge if tests failed
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
                    def slack = load 'slack_notifications.groovy' // Load the Slack notification script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL) // Construct a message with build details
                    slack.sendSlackNotification(message, "good") // Send Slack notification on success
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}" // Log Slack notification errors
                }
            }
        }
    }
}


