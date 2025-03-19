pipeline {
    agent any

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication..."
                        
                        // Print SSH_AUTH_SOCK to verify it's set
                        sh "echo 'SSH_AUTH_SOCK is: $SSH_AUTH_SOCK'"

                        // Ensure the key is loaded
                        sh "ssh-add -l || echo 'No SSH keys loaded!'"

                        // Test SSH connection to GitHub
                        sh "ssh -T git@github.com || echo 'SSH Connection failed!'"
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository..."
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[
                            url: 'git@github.com:uriya66/DevOps1.git',
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])
                }
            }
        }

        stage('Start SSH Agent & Verify Key') {
            steps {
                sshagent(['Jenkins-GitHub-Token']) {
                    script {
                        echo "Verifying SSH Agent and Loaded Keys..."
                        
                        // Check if the SSH Agent is running and if the key is loaded
                        sh """
                            echo "Checking SSH_AUTH_SOCK: \$SSH_AUTH_SOCK"
                            ssh-add -l || echo "No SSH keys loaded!"
                            ssh -T git@github.com || echo "SSH Connection failed!"
                        """
                    }
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    def newBranch = "feature-${env.BUILD_NUMBER}"
                    echo "Creating a new feature branch: ${newBranch}"

                    withEnv(["SSH_AUTH_SOCK=${env.HOME}/.ssh/ssh-agent.sock"]) {
                        sh """
                            git checkout -b ${newBranch}
                            git push git@github.com:uriya66/DevOps1.git ${newBranch}
                        """
                    }
                    env.GIT_BRANCH = newBranch
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e
                    echo "Setting up Python virtual environment..."

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
                    echo "Running API tests..."

                    . venv/bin/activate
                    venv/bin/python -m pytest test_app.py
                """
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.GIT_BRANCH.startsWith("feature-") }
            }
            steps {
                script {
                    echo "Merging ${env.GIT_BRANCH} back to main..."

                    sh """
                        git checkout main
                        git pull git@github.com:uriya66/DevOps1.git main
                        git merge --no-ff ${env.GIT_BRANCH}
                        git push git@github.com:uriya66/DevOps1.git main
                    """
                }
            }
        }
    }

    post {
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

