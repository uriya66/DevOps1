pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent parallel runs of the same job
        skipDefaultCheckout()  // Disable the default SCM checkout
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH URL for the GitHub repository
        TRIGGER_BRANCH = ''  // Will be set dynamically based on GitHub trigger
        FEATURE_BRANCH = "feature-${env.BUILD_NUMBER}"  // Feature branch based on build number
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {  // Start SSH agent with GitHub credentials
                    script {
                        echo "Starting SSH Agent and verifying authentication"  // Log info
                        sh "ssh-add -l"  // Show loaded SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection successful"  # SSH success log
                            else
                                echo "ERROR: SSH connection failed!"  # SSH failure log
                                exit 1  # Fail the build if SSH doesn't work
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the triggering branch"  // Log current stage

                    // Detect triggering branch
                    def detectedBranch = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
                    env.TRIGGER_BRANCH = detectedBranch  // Save for later usage

                    echo "Trigger branch is: ${env.TRIGGER_BRANCH}"  // Log the triggering branch

                    // Checkout the correct branch
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.TRIGGER_BRANCH}"]],
                        userRemoteConfigs: [[
                            url: env.REPO_URL,
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating branch ${env.FEATURE_BRANCH} from ${env.TRIGGER_BRANCH}"  // Log creation

                    sh '''
                        git checkout -b ${FEATURE_BRANCH}  # Create new local feature branch
                        git push origin ${FEATURE_BRANCH}  # Push to remote
                    '''
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit on any error
                    echo "Setting up Python virtual environment"  # Log setup
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if missing
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
                    echo "Running tests with Gunicorn"  # Log info
                    . venv/bin/activate  # Activate venv
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &  # Run app in background
                    sleep 3  # Allow app to start
                    venv/bin/python -m pytest test_app.py  # Run pytest tests
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    echo "Deploying app with deploy.sh script"  # Log deployment
                    chmod +x deploy.sh  # Ensure script is executable
                    ./deploy.sh  # Run deploy
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression { env.FEATURE_BRANCH.startsWith("feature-") }  // Only merge from feature branches
            }
            steps {
                script {
                    echo "Merging ${env.FEATURE_BRANCH} into main..."  // Log merging
                    sh '''
                        git config user.name "jenkins"  # Set Git name
                        git config user.email "jenkins@example.com"  # Set Git email
                        git checkout main  # Switch to main
                        git pull origin main  # Get latest
                        git merge --no-ff ${FEATURE_BRANCH}  # Merge with no fast-forward
                        git push origin main  # Push changes
                    '''
                }
            }
        }
    }

    post {
        success {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack script
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, true, true)  // Full Slack message
                slack.sendSlackNotification(message, "good")  // Send green notification
            }
        }

        failure {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack script
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)  // Failure message
                slack.sendSlackNotification(message, "danger")  // Send red notification
            }
        }

        always {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack again
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Basic message
                slack.sendSlackNotification(message, "good")  // Send regardless of result
            }
        }
    }  // Close post block
}  // Close pipeline block
