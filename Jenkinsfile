pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Define the GitHub repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Create a unique feature branch per build
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "echo 'SSH_AUTH_SOCK is: $SSH_AUTH_SOCK'"
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
                            git checkout -b ${BRANCH_NAME}
                            git push origin ${BRANCH_NAME}
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME
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
                    def branchName = env.GIT_BRANCH.replace("origin/", "")  
                    echo "Current Branch after cleanup: ${branchName}"
                    return branchName.startsWith("feature-")
                }
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Tests passed, merging ${env.GIT_BRANCH} back to main..."
                        sh """
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${env.GIT_BRANCH}
                            git push origin main
                        """
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

