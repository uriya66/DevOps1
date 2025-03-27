pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repository URL
        BASE_BRANCH = ''  // Will hold the original triggering branch (main or feature-test)
        BRANCH_NAME = ''  // Will hold the generated feature-${BUILD_NUMBER} branch name
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication"  // Start SSH agent and verify
                        sh 'ssh-add -l'  // List loaded keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection successful"  # Confirm SSH connection
                            else
                                echo "ERROR: SSH connection failed!"  # Report SSH failure
                                exit 1  # Fail the build
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the triggering branch"  // Log start of checkout
                    BASE_BRANCH = sh(script: "git log -1 --pretty=format:%D | grep -oE 'origin/(main|feature-test)' | cut -d/ -f2", returnStdout: true).trim()  // Get real triggering branch
                    if (!BASE_BRANCH) {
                        error("Could not detect triggering branch")  // Fail if detection fails
                    }
                    echo "Trigger branch is: ${BASE_BRANCH}"   // Log detected branch

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${BASE_BRANCH}"]],
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
                    BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Define dynamic feature branch name
                    echo "Creating a new branch from ${BASE_BRANCH} → ${BRANCH_NAME}"  // Log branch creation
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create new local branch
                            git push origin ${BRANCH_NAME}  # Push to GitHub
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Save for later use
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Stop if any command fails
                    echo "Setting up Python virtual environment"  # Log setup
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if not exists
                    . venv/bin/activate  # Activate venv
                    venv/bin/pip install --upgrade pip  # Upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn  # Install dependencies
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Stop on failure
                    echo "Starting tests with Gunicorn"  # Log start
                    . venv/bin/activate  # Activate venv
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start Flask app
                    sleep 3  # Wait for app
                    echo "Running API tests"  # Log testing
                    venv/bin/pytest test_app.py  # Run tests
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e  # Stop if deploy fails
                    echo "Running deployment script"  # Log deploy
                    chmod +x deploy.sh  # Ensure script is executable
                    ./deploy.sh  # Run deploy
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith('feature-')  // Only merge feature branches
                }
            }
            steps {
                script {
                    echo "Merging ${GIT_BRANCH} into main"  // Log merge
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            git config user.name "jenkins"  # Configure Git user
                            git config user.email "jenkins@example.com"
                            git checkout main  # Checkout main branch
                            git pull origin main  # Update main
                            git merge --no-ff ${GIT_BRANCH}  # Merge feature
                            git push origin main  # Push merged main
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack helper
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, true, true)  // Build Slack message
                slack.sendSlackNotification(message, "good")  // Send Slack notification
            }
        }
        failure {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack helper
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)  // Build failure message
                slack.sendSlackNotification(message, "danger")  // Send error notification
            }
        }
    }  // Close post block
}  // Close pipeline block
