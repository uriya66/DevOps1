pipeline {
    agent any  // Use any available agent for the pipeline

    environment {
        DEPLOY_SUCCESS = 'false'   // Track deployment result across stages
        MERGE_SUCCESS = 'false'    // Track merge result across stages
    }

    triggers {
        // Use Generic Webhook Trigger Plugin to filter payload before execution
        GenericTrigger(
            genericVariables: [
                [key: 'GIT_COMMIT_MESSAGE', value: '$.head_commit.message']  // Extract commit message from GitHub payload
            ],
            causeString: 'Triggered by GitHub Push: $GIT_COMMIT_MESSAGE',
            tokenCredentialId: 'GitHub PAT token for webhook and repo access',
            regexpFilterText: '$GIT_COMMIT_MESSAGE',
            regexpFilterExpression: '^(?!JENKINS AUTO MERGE).*'  // Prevent infinite loop on auto-merge
        )
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'  // Verify GitHub SSH access
                }
            }
        }

        stage('Clone feature-test branch (clean)') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/feature-test']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:uriya66/DevOps1.git',
                        credentialsId: 'Jenkins-GitHub-SSH'
                    ]]
                ])
            }
        }

        stage('Create & Push feature-${BUILD_NUMBER}') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh """
                        git checkout -b feature-${BUILD_NUMBER}       # Create new feature branch from feature-test
                        git push origin feature-${BUILD_NUMBER}       # Push the new feature branch to GitHub
                    """
                }
            }
        }

        stage('Build') {
            steps {
                echo "[DEBUG] Installing dependencies"
                sh '''
                    set -e
                    [ ! -d venv ] && python3 -m venv venv                     # Create virtual environment if not exists
                    . venv/bin/activate                                       # Activate Python virtual environment
                    pip install --upgrade pip flask pytest gunicorn requests  # Install required Python packages
                '''
            }
        }

        stage('Test') {
            steps {
                echo "[DEBUG] Running tests"
                sh '''
                    set -e
                    . venv/bin/activate                                       # Activate virtual environment
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &                 # Start Flask app for testing
                    sleep 5                                                   # Wait for app to be available
                    pytest test_app.py                                        # Run test suite
                    pkill -f gunicorn || true                                 # Stop Gunicorn server
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        echo "[DEBUG] Running deploy script"
                        sh '''
                            chmod +x deploy.sh                                # Make deploy script executable
                            ./deploy.sh                                       # Execute deployment script
                        '''
                        DEPLOY_SUCCESS = 'true'                               // Mark deployment as successful
                        echo "[DEBUG] DEPLOY_SUCCESS=true"
                    } catch (err) {
                        echo "[ERROR] Deployment failed: ${err.message}"
                        DEPLOY_SUCCESS = 'false'                              // Mark deployment as failed
                        echo "[DEBUG] DEPLOY_SUCCESS=false"
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return DEPLOY_SUCCESS == 'true'  // Only merge if deployment succeeded
                }
            }
            steps {
                script {
                    try {
                        echo "[INFO] Merging feature-${BUILD_NUMBER} to main"
                        sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                            def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                            echo "[DEBUG] Commit before merge: ${commitMsg}"

                            if (!commitMsg.startsWith("JENKINS AUTO MERGE -")) {
                                sh """
                                    git config user.name 'jenkins'                                    # Set Git username
                                    git config user.email 'jenkins@example.com'                       # Set Git email
                                    git checkout main                                                 # Checkout main branch
                                    git pull origin main                                              # Pull latest main changes
                                    git merge --no-ff feature-${BUILD_NUMBER} -m 'JENKINS AUTO MERGE - CI: Auto-merge feature branch to main after successful pipeline'
                                    git push origin main                                              # Push merged changes
                                """
                                MERGE_SUCCESS = 'true'                                                // Mark merge as successful
                                echo "[DEBUG] MERGE_SUCCESS=true"
                            } else {
                                echo "[INFO] Skipping merge - already merged automatically"
                            }
                        }
                    } catch (err) {
                        echo "[ERROR] Merge failed: ${err.message}"
                        MERGE_SUCCESS = 'false'  // Mark merge as failed
                        echo "[DEBUG] MERGE_SUCCESS=false"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack notification helper script

                def mergeStatus = MERGE_SUCCESS == 'true'
                def deployStatus = DEPLOY_SUCCESS == 'true'
                def color = (mergeStatus && deployStatus) ? 'good' : 'danger'

                echo "[DEBUG] Sending Slack notification with DEPLOY_SUCCESS=${deployStatus}, MERGE_SUCCESS=${mergeStatus}"

                def message = slack.constructSlackMessage(
                    env.BUILD_NUMBER,
                    env.BUILD_URL,
                    mergeStatus,
                    deployStatus
                )

                slack.sendSlackNotification(message, color)  // Send formatted message to Slack
            }
        }
    }  // Close post block
}  // Close pipeline block

