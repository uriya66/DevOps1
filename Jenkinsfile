pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds() // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Define GitHub repository URL
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Define a unique feature branch using build number
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                // Start SSH agent to authenticate with GitHub using Jenkins credentials
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
                    // Clone the main branch using secure SSH access
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
                    return !(env.GIT_BRANCH?.startsWith("feature-") ?: false) // Only create branch if it doesn't already exist
                }
            }
            steps {
                script {
                    echo "Creating a new feature branch: ${BRANCH_NAME}"

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            git checkout -b ${BRANCH_NAME}  # Create new local feature branch
                            git push origin ${BRANCH_NAME}  # Push it to GitHub
                        '''
                    }

                    env.GIT_BRANCH = BRANCH_NAME  // Set feature branch name for later usage
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  // Exit if any command fails
                    echo "Setting up Python virtual environment."
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  // Create venv if missing
                    . venv/bin/activate  // Activate Python virtual environment
                    venv/bin/python -m pip install --upgrade pip  // Upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn  // Install required packages
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  // Stop if any command fails
                    echo "Starting Flask app for testing..."
                    . venv/bin/activate  // Activate virtual environment
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  // Start the app with Gunicorn in background
                    sleep 3  // Give server time to start
                    echo "Running API tests."
                    venv/bin/python -m pytest test_app.py  // Run test suite with pytest
                '''
            }
        }

        stage('Backup') {
            steps {
                sh '''
                    echo "Creating project backup..."
                    chmod +x backup.sh  // Ensure the backup script is executable
                    ./backup.sh  // Run the backup script
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith("feature-") ?: false  // Only merge if it's a feature branch
                }
            }
            steps {
                script {
                    echo "Checking if all tests passed before merging..."

                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        echo "Tests passed, merging ${env.GIT_BRANCH} into main."

                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh '''
                                git config user.name "jenkins"  // Set Git user for automated commit
                                git config user.email "jenkins@example.com"  // Set Git email
                                git checkout main  // Switch to main branch
                                git pull origin main  // Update local main
                                git merge --no-ff ${GIT_BRANCH}  // Merge without fast-forward
                                git push origin main  // Push merge to GitHub
                            '''
                        }
                    } else {
                        echo "Tests failed, skipping merge."
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return env.GIT_BRANCH == 'main'  // Only deploy if on main branch
                }
            }
            steps {
                script {
                    echo "Starting deployment..."
                    sh '''
                        chmod +x deploy.sh  // Ensure deploy script is executable
                        ./deploy.sh  // Run the deployment script
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
                    def slack = load 'slack_notifications.groovy'  // Load Slack script
                    def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build message content
                    slack.sendSlackNotification(message, "good")  // Send Slack notification
                } catch (Exception e) {
                    echo "Error sending Slack notification: ${e.message}"  // Log Slack errors but do not fail pipeline
                }
            }
        }
    }
}
