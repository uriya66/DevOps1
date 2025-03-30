pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repository URL
        BASE_BRANCH = ''  // Will hold the original triggering branch (main or feature-test)
        BRANCH_NAME = ''  // Will hold the generated feature-${BUILD_NUMBER} branch name
        GIT_BRANCH = ''  // Will hold current working branch name
        MERGE_SUCCESS = 'false'  // Track merge status for Slack
        DEPLOY_SUCCESS = 'false'  // Track deploy status for Slack
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication"  // Start SSH agent and verify
                        sh 'ssh-add -l'  // List loaded SSH keys
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
                    echo "Detecting triggering branch using Git log"  // Debug branch detection
                    BASE_BRANCH = sh(script: "git log -1 --pretty=format:%D | grep -oE 'origin/(main|feature-test)' | cut -d/ -f2", returnStdout: true).trim()
                    if (!BASE_BRANCH) {
                        error("❌ Could not detect triggering branch. Please push to 'main' or 'feature-test'.")  // Fail if detection fails
                    }
                    echo "Trigger branch is: ${BASE_BRANCH}"  // Log detected branch name

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
                    BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Define feature branch name
                    echo "Creating feature branch: ${BRANCH_NAME} from ${BASE_BRANCH}"  // Log branch creation
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create new branch
                            git push origin ${BRANCH_NAME}  # Push to remote
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Save current working branch
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit on failure
                    echo "Setting up Python virtual environment..."  # Log setup
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
                    set -e  # Exit on error
                    echo "Running tests using Gunicorn..."  # Log test phase
                    . venv/bin/activate  # Activate virtualenv
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Start app in background
                    sleep 3  # Wait for app to be ready
                    echo "Executing API tests..."  # Log tests
                    venv/bin/pytest test_app.py  # Run pytest
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        sh '''
                            set -e  # Stop on error
                            echo "Running deployment script..."  # Log deploy
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        env.DEPLOY_SUCCESS = 'true'  // Mark deploy as successful
                    } catch (Exception e) {
                        echo "❌ Deployment failed: ${e.message}"  // Log deploy error
                        env.DEPLOY_SUCCESS = 'false'  // Mark as failed
                        error("Deployment failed: ${e.message}")  // Stop pipeline
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    def currentBranch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                    echo "Current branch for merge verification: ${currentBranch}"  // Debug branch
                    return currentBranch.startsWith('feature-')  // Only merge feature branches
                }
            }
            steps {
                script {
                    try {
                        echo "Attempting to merge ${env.GIT_BRANCH} into main..."  // Log merge attempt
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
                        echo "✅ Merge completed successfully."  // Log merge success
                        env.MERGE_SUCCESS = 'true'  // Mark merge as successful
                    } catch (Exception e) {
                        echo "❌ Merge to main failed: ${e.message}"  // Log merge failure
                        env.MERGE_SUCCESS = 'false'  // Mark as failed
                        error("Merge to main failed: ${e.message}")  // Stop pipeline
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'  // Load Slack helper
                    def message = slack.constructSlackMessage(
                        env.BUILD_NUMBER,
                        env.BUILD_URL,
                        env.MERGE_SUCCESS == 'true',
                        env.DEPLOY_SUCCESS == 'true'
                    )  // Build Slack message with merge/deploy status
                    def color = (env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true') ? 'good' : 'danger'
                    slack.sendSlackNotification(message, color)  // Send Slack notification
                } catch (Exception e) {
                    echo "❌ Failed to send Slack notification: ${e.message}"  // Log Slack error
                }
            }
        }
    }  // Close post
}  // Close pipeline

