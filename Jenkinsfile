pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repo URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Always create a new feature branch dynamically
        GIT_BRANCH = ""  // Initialize to avoid null issues later
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l"
                        sh '''
                            # Verify SSH access to GitHub
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH Connection successful."
                            else
                                echo "ERROR: SSH Connection failed!"
                                exit 1
                            fi
                        '''
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
                        branches: [[name: '*/main']],
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
                    echo "Creating a new feature branch: ${BRANCH_NAME}"

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            # Ensure Git is initialized correctly (in case of Jenkins SCM checkout mode)
                            if ! git rev-parse --git-dir > /dev/null 2>&1; then
                                git init
                            fi

                            # Remove origin if exists to avoid duplicate remote error
                            git remote remove origin 2>/dev/null || true
                            git remote add origin ${REPO_URL}
                            git fetch origin

                            # Create and push new branch
                            git checkout -b ${BRANCH_NAME} origin/main
                            git push origin ${BRANCH_NAME}
                        '''
                    }

                    env.GIT_BRANCH = BRANCH_NAME  // Save branch name globally
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    echo "Setting up Python virtual environment."
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Starting Flask app for testing..."
                    . venv/bin/activate
                    sleep 3
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    echo "Running API tests."
                    venv/bin/python -m pytest test_app.py
                '''
            }
        }
    }

    post {
        success {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack helper script

                echo "Build and tests passed successfully."

                def branch = env.GIT_BRANCH?.trim() ? env.GIT_BRANCH : env.BRANCH_NAME
                def mergeSuccess = false
                def deploySuccess = false

                if (branch.startsWith("feature-")) {
                    try {
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh '''
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main
                                git pull origin main
                                git merge --no-ff ${GIT_BRANCH}
                                git push origin main
                            '''
                        }
                        echo "Merge completed successfully."
                        mergeSuccess = true
                    } catch (Exception mergeError) {
                        echo "Merge failed: ${mergeError.message}"
                    }

                    if (mergeSuccess) {
                        try {
                            echo "Starting deployment after merge..."
                            sh '''
                                chmod +x deploy.sh
                                ./deploy.sh
                            '''
                            echo "Deployment script executed successfully."
                            deploySuccess = true
                        } catch (Exception deployError) {
                            echo "Deployment failed: ${deployError.message}"
                        }
                    }
                }

                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, mergeSuccess, deploySuccess)
                def statusColor = (mergeSuccess && deploySuccess) ? "good" : "danger"
                slack.sendSlackNotification(message, statusColor)
            }
        }

        failure {
            script {
                echo "Build or tests failed."
                def slack = load 'slack_notifications.groovy'
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)
                slack.sendSlackNotification(message, "danger")
            }
        }

        always {
            script {
                def slack = load 'slack_notifications.groovy'
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)
                slack.sendSlackNotification(message, "good")
            }
        }
    }  // Close post block
}  // Close pipeline block

