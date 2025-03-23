pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds() // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Define GitHub repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Generate a unique feature branch name for this build
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l"  // List loaded SSH keys
                        sh '''
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
                    // Checkout the latest code from the main branch
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
            when {
                expression {
                    return !(env.GIT_BRANCH?.startsWith("feature-") ?: false)  // Only create if not already a feature branch
                }
            }
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            git checkout -b ${BRANCH_NAME}  # Create new feature branch
                            git push origin ${BRANCH_NAME}  # Push new branch to remote
                        '''
                    }

                    env.GIT_BRANCH = BRANCH_NAME  // Set this for use in later pipeline stages
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit immediately if a command exits with a non-zero status
                    echo "Setting up Python virtual environment."
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if it doesn't exist
                    . venv/bin/activate  # Activate virtual environment
                    venv/bin/python -m pip install --upgrade pip  # Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn  # Install dependencies
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Stop on first error
                    echo "Starting Flask app for testing..."
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Launch the app
                    sleep 3  # Wait for server to start
                    echo "Running API tests."
                    venv/bin/python -m pytest test_app.py  # Run tests
                '''
            }
        }

        stage('Backup') {
            steps {
                sh '''
                    echo "Creating project backup..."
                    chmod +x backup.sh  # Make sure script is executable
                    ./backup.sh  # Run backup script
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith("feature-") ?: false  // Merge only if we're in a feature branch
                }
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."

                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Tests passed, merging ${env.GIT_BRANCH} into main..."

                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh """
                                git config user.name "jenkins"  # Set Git user
                                git config user.email "jenkins@example.com"  # Set Git email
                                git checkout main  # Switch to main branch
                                git pull origin main  # Pull latest main
                                git merge --no-ff ${env.GIT_BRANCH}  # Merge feature branch
                                git push origin main  # Push merged result
                            """
                        }
                    } else {
                        echo "Tests failed, skipping merge!"
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return env.GIT_BRANCH == 'main'  // Deploy only if we are on main
                }
            }
            steps {
                script {
                    echo "Starting deployment..."
                    sh '''
                        chmod +x deploy.sh  # Ensure deploy script is executable
                        ./deploy.sh  # Run the deploy script
                    '''
                }
            }
        }
    }

    post {
        success {
            script {
                echo "Build and tests passed successfully."
            }
        }
        failure {
            script {
                echo "Build or tests failed."
            }
        }
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load external Slack notifier
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Create message
                    slack.sendSlackNotification(message, "good")  // Send to Slack
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"  // Don't fail build for notification error
                }
            }
        }
    }
}

