pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repository URL
        BASE_BRANCH = ''  // Will hold the original triggering branch (main or feature-test)
        BRANCH_NAME = ''  // Will hold the generated feature-${BUILD_NUMBER} branch name
        GIT_BRANCH = ''  // Will be used for merge condition and Slack
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication"  // Log SSH agent init
                        sh 'ssh-add -l'  // List current keys
                        sh '''
                            echo "Testing SSH connection to GitHub..."  // Log connection test
                            ssh -o StrictHostKeyChecking=no -T git@github.com || true  // Validate SSH
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
                    ).trim()  // Extract branch name

                    if (!detectedBranch) {
                        error("Could not detect triggering branch")  // Fail if not found
                    }

                    BASE_BRANCH = detectedBranch  // Set pipeline var
                    echo "Trigger branch is: ${BASE_BRANCH}"  // Log detected

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
                    def buildBranch = "feature-${env.BUILD_NUMBER}"  // Use temporary local var
                    BRANCH_NAME = buildBranch  // Set pipeline var
                    env.GIT_BRANCH = buildBranch.toString()  // Set env var as String

                    echo "Creating a new branch from ${BASE_BRANCH} → ${BRANCH_NAME}"  // Log creation

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create local branch
                            git push origin ${BRANCH_NAME}  # Push to GitHub
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Stop on error
                    echo "Setting up Python virtual environment"  # Log setup
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi  # Create venv if missing
                    . venv/bin/activate  # Activate
                    venv/bin/pip install --upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Starting tests with Gunicorn"
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    echo "Running API tests"
                    venv/bin/pytest test_app.py
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e
                    echo "Running deployment script"
                    chmod +x deploy.sh
                    ./deploy.sh
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith('feature-')  // Only feature branches
                }
            }
            steps {
                script {
                    echo "Merging ${env.GIT_BRANCH} into main"  // Log merge

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git config user.name "jenkins"
                            git config user.email "jenkins@example.com"
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${env.GIT_BRANCH}
                            git push origin main
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack helper
                def didMerge = env.GIT_BRANCH?.startsWith('feature-')  // Check merge condition
                def didDeploy = true  // You can improve this by parsing deploy log later
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, didMerge, didDeploy)
                slack.sendSlackNotification(message, "good")  // Send Slack success
            }
        }
        failure {
            script {
                def slack = load 'slack_notifications.groovy'
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)
                slack.sendSlackNotification(message, "danger")  // Send Slack failure
            }
        }
    }
}

