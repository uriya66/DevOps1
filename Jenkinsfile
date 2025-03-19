pipeline {
    agent any

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the repository..."
                    checkout scm

                    // Start SSH Agent and ensure authentication works
                    sh """
                        echo "Starting SSH Agent..."
                        . /var/lib/jenkins/start-ssh-agent.sh
                        . /var/lib/jenkins/.ssh_env  # Load SSH Agent environment
                    """

                    // Get the current branch dynamically
                    def currentBranch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                    env.GIT_BRANCH = currentBranch
                    echo "Current branch: ${env.GIT_BRANCH}"
                }
            }
        }

        stage('Verify SSH Connection') {
            steps {
                script {
                    sh """
                        echo "Checking SSH Authentication..."
                        . /var/lib/jenkins/.ssh_env  # Load SSH Agent environment
                        ssh-add -l || echo "No SSH keys loaded in agent!"
                        ssh -T git@github.com || echo "SSH Connection failed!"
                    """
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

        stage('Start Gunicorn') {
            steps {
                sh """
                    set -e
                    echo "Stopping Gunicorn service..."

                    if systemctl is-active --quiet gunicorn; then
                        sudo -n systemctl stop gunicorn
                    fi

                    echo "Starting Gunicorn service..."
                    sudo -n systemctl start gunicorn

                    sleep 5

                    echo "Checking Gunicorn status..."
                    if ! systemctl is-active --quiet gunicorn; then
                        echo "ERROR: Gunicorn service failed to start!"
                        exit 1
                    fi
                """
            }
        }

        stage('API Health Check') {
            steps {
                sh """
                    set -e
                    chmod +x api_health_check.sh
                    echo "Running API Health Check..."
                    ./api_health_check.sh
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

