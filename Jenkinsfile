pipeline {
    agent any  // Use any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple concurrent builds
        skipDefaultCheckout(true)  // Skip default automatic Git checkout
    }

    triggers {
        pollSCM('* * * * *')  // Poll SCM every minute
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub repository URL (SSH)
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Load SSH key from Jenkins credentials
                    script {
                        echo "Authenticating GitHub with SSH"
                        sh "ssh-add -l"  // Show loaded SSH key
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection OK"
                            else
                                echo "SSH connection failed!"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Detect Branch') {
            steps {
                script {
                    // Get the Git branch that triggered the build
                    env.GIT_BRANCH = sh(
                        script: "git rev-parse --abbrev-ref HEAD",
                        returnStdout: true
                    ).trim()
                    env.BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Define new feature branch
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Cloning from triggering branch: ${env.GIT_BRANCH}"
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.GIT_BRANCH}"]],  // Use current triggering branch
                        userRemoteConfigs: [[
                            url: REPO_URL,
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating new branch ${env.BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${env.BRANCH_NAME}
                            git push origin ${env.BRANCH_NAME}
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e
                    echo "Preparing virtual environment"
                    python3 -m venv venv  # Always create fresh venv
                    . venv/bin/activate
                    venv/bin/pip install --upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Running unit tests"
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    venv/bin/python -m pytest test_app.py
                    pkill gunicorn
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.GIT_BRANCH.startsWith("feature-") }
            }
            steps {
                script {
                    def slack = load 'slack_notifications.groovy'
                    echo "Merging ${env.BRANCH_NAME} to main"
                    sh """
                        git checkout main
                        git pull origin main
                        git merge --no-ff ${env.BRANCH_NAME}
                        git push origin main
                    """
                    slack.sendSlackNotification("Merge completed successfully for ${env.BRANCH_NAME}", "good")
                }
            }
        }

        stage('Deploy') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                sh "bash deploy.sh"
                script {
                    def slack = load 'slack_notifications.groovy'
                    slack.sendSlackNotification("Deployment completed for ${env.BRANCH_NAME}", "good")
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
                    echo "Slack final notification failed: ${e.message}"
                }
            }
        }
    }
}

