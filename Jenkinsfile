pipeline {
    agent any  // Run on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent simultaneous builds
        skipDefaultCheckout()  // Disable default Git checkout to control it manually
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repository
        BASE_BRANCH = ''  // Will store the triggering branch (main or feature-test)
        BRANCH_NAME = ''  // Will store the feature branch name (feature-${BUILD_NUMBER})
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication"  // Log stage
                        sh 'ssh-add -l'  // List available SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection successful"  # SSH verification passed
                            else
                                echo "ERROR: SSH connection failed!"  # SSH failed
                                exit 1  # Stop pipeline
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the triggering branch"  // Log checkout process
                    BASE_BRANCH = sh(script: "git log -1 --pretty=format:%D | grep -oE 'origin/(main|feature-test)' | cut -d/ -f2", returnStdout: true).trim()
                    if (!BASE_BRANCH) {
                        error("Could not detect triggering branch")  // Stop if no base branch detected
                    }
                    echo "Trigger branch is: ${BASE_BRANCH}"  // Log the branch

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
                    BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Dynamic feature branch
                    echo "Creating new branch from ${BASE_BRANCH} → ${BRANCH_NAME}"  // Log creation
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create branch
                            git push origin ${BRANCH_NAME}  # Push to remote
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Export for later
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit on error
                    echo "Setting up Python virtual environment"  # Log setup
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if needed
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
                    echo "Starting tests with Gunicorn"  # Log start
                    . venv/bin/activate  # Activate venv
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Run app
                    sleep 3  # Wait for server
                    echo "Running API tests"  # Log testing
                    venv/bin/pytest test_app.py  # Run tests
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e  # Exit on error
                    echo "Running deployment script"  # Log deploy
                    chmod +x deploy.sh  # Make script executable
                    ./deploy.sh  # Deploy
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.GIT_BRANCH?.startsWith('feature-') }  // Only feature branches
            }
            steps {
                script {
                    echo "Merging ${GIT_BRANCH} into main"  // Log merge
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            git config user.name "jenkins"  # Git username
                            git config user.email "jenkins@example.com"  # Git email
                            git checkout main  # Switch to main
                            git pull origin main  # Sync latest
                            git merge --no-ff ${GIT_BRANCH}  # Merge feature branch
                            git push origin main  # Push updated main
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
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, true, true)  // Build success message
                slack.sendSlackNotification(message, "good")  // Send to Slack
            }
        }
        failure {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack helper
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)  // Build failure message
                slack.sendSlackNotification(message, "danger")  // Send to Slack
            }
        }
    }  // Close post block
}  // Close pipeline block
