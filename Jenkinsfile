pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repository URL
        BASE_BRANCH = ''  // Will hold the original triggering branch (main or feature-test)
        BRANCH_NAME = ''  // Will hold the generated feature-${BUILD_NUMBER} branch name
        GIT_BRANCH = ''  // Used later for merge and Slack
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication"  // Log SSH agent init
                        sh 'ssh-add -l'  // List current keys
                        sh '''
                            echo "Testing SSH connection to GitHub..."  # Log connection test
                            ssh -o StrictHostKeyChecking=no -T git@github.com || true  # Validate SSH
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Detecting triggering branch using Git log"  // Log detection start

                    def detectedBranch = sh(
                        script: "git log -1 --pretty=format:%D | grep -oE 'origin/(main|feature-test)' | cut -d/ -f2",
                        returnStdout: true
                    ).trim()  // Extract branch name from Git log

                    echo "Detected Branch From Git: ${detectedBranch}"  // Debug output

                    if (!detectedBranch) {
                        error("Could not detect triggering branch")  // Fail if detection failed
                    }

                    BASE_BRANCH = detectedBranch  // Save base branch
                    echo "Trigger branch is: ${BASE_BRANCH}"  // Log result

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${BASE_BRANCH}"]],
                        userRemoteConfigs: [[
                            url: REPO_URL,
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])  // Checkout real triggering branch
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Set dynamic feature branch
                    echo "Creating a new branch from ${BASE_BRANCH} → ${BRANCH_NAME}"  // Log creation

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create local feature branch
                            git push origin ${BRANCH_NAME}  # Push to remote
                        """
                    }

                    env.GIT_BRANCH = BRANCH_NAME  // Set the final GIT_BRANCH globally and cleanly
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Stop on error
                    echo "Setting up Python virtual environment"  # Log setup
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if missing
                    . venv/bin/activate  # Activate virtualenv
                    venv/bin/pip install --upgrade pip  # Upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn  # Install dependencies
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Stop on error
                    echo "Starting tests with Gunicorn"  # Log start
                    . venv/bin/activate  # Activate virtualenv
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Run app
                    sleep 3  # Give it time to start
                    echo "Running API tests"  # Log test phase
                    venv/bin/pytest test_app.py  # Run tests
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e  # Stop on error
                    echo "Running deployment script"  # Log start
                    chmod +x deploy.sh  # Ensure script is executable
                    ./deploy.sh  # Run deploy
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH && env.GIT_BRANCH.startsWith('feature-')  // Only run on feature-* branches
                }
            }
            steps {
                script {
                    echo "Merging ${env.GIT_BRANCH} into main"  // Log merge

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git config user.name "jenkins"  # Git username
                            git config user.email "jenkins@example.com"  # Git email
                            git checkout main  # Switch to main
                            git pull origin main  # Update main
                            git merge --no-ff ${env.GIT_BRANCH}  # Merge the feature branch
                            git push origin main  # Push changes
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack helper script
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, true, true)  // Build Slack message
                slack.sendSlackNotification(message, "good")  // Send Slack success message
            }
        }
        failure {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack script
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)  // Build error message
                slack.sendSlackNotification(message, "danger")  // Send Slack failure message
            }
        }
    }
}

