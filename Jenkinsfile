pipeline {
    agent any

    environment {
        DEPLOY_SUCCESS = 'false'  //  Track deployment status
        MERGE_SUCCESS = 'false'   // Track merge status
    }

    stages {
        stage('Init Git from Trigger Source') {
            steps {
                checkout scm  // Checkout the source that triggered the pipeline
            }
        }

        stage('Skip Auto-Merge Loop on main') {
            steps {
                script {
                    // Get the current branch name
                    def currentBranch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                    echo "[DEBUG] Current branch for skip check: ${currentBranch}"

                    // Skip redundant builds only for main
                    if (currentBranch == 'main') {
                        def lastCommitMessage = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                        echo "[DEBUG] Last commit message: ${lastCommitMessage}"

                        if (lastCommitMessage.startsWith('JENKINS AUTO MERGE -')) {
                            echo "[INFO] Detected auto-merge commit by Jenkins. Skipping redundant pipeline run."
                            currentBuild.result = 'SUCCESS'
                            error("Skipping pipeline due to auto-merge")  //  Fully stops the pipeline
                        } else {
                            echo "[DEBUG] Commit is not an auto-merge. Continuing pipeline."
                        }
                    } else {
                        echo "[INFO] Skip check not needed for branch: ${currentBranch}"
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'  // Confirm SSH access
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
                        git checkout -b feature-${BUILD_NUMBER}         # Create a feature branch from feature-test
                        git push origin feature-${BUILD_NUMBER}         # Push the feature branch to GitHub
                    """
                }
            }
        }

        stage('Build') {
            steps {
                echo "[DEBUG] Installing Python dependencies"
                sh '''
                    set -e
                    [ ! -d venv ] && python3 -m venv venv             # Create virtual environment if missing
                    . venv/bin/activate                               # Activate virtual environment
                    pip install --upgrade pip flask pytest gunicorn requests  # Install Python dependencies
                '''
            }
        }

        stage('Test') {
            steps {
                echo "[DEBUG] Running pytest and Flask health"
                sh '''
                    set -e
                    . venv/bin/activate                               # Activate Python environment
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &         # Start Flask app
                    sleep 5                                           # Wait for app to load
                    pytest test_app.py                                # Run tests
                    pkill -f gunicorn || true                         # Kill Gunicorn
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        echo "[DEBUG] Running deploy.sh script"
                        sh '''
                            chmod +x deploy.sh                        # Make deploy script executable
                            ./deploy.sh                               # Run deployment script
                        '''
                        DEPLOY_SUCCESS = 'true'                       // Set deployment success flag
                        echo "[DEBUG] DEPLOY_SUCCESS=true"
                        currentBuild.description = "DEPLOY_SUCCESS=true"
                    } catch (err) {
                        echo "[ERROR] Deployment failed: ${err.message}"
                        DEPLOY_SUCCESS = 'false'
                        echo "[DEBUG] DEPLOY_SUCCESS=false"
                        currentBuild.description = "DEPLOY_SUCCESS=false"
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    echo "[DEBUG] Checking DEPLOY_SUCCESS value for merge stage: ${DEPLOY_SUCCESS}"
                    return DEPLOY_SUCCESS == 'true'  // ✅ Check deploy success before merging
                }
            }
            steps {
                script {
                    try {
                        echo "[INFO] Attempting to merge feature-${BUILD_NUMBER} to main"
                        sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                            def lastCommit = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                            echo "[DEBUG] Commit before merge: ${lastCommit}"

                            if (!lastCommit.startsWith('JENKINS AUTO MERGE -')) {
                                sh """
                                    git config user.name 'jenkins'                              # Set Git user
                                    git config user.email 'jenkins@example.com'                 # Set Git email
                                    git checkout main                                           # Switch to main
                                    git pull origin main                                        # Sync with remote
                                    git merge --no-ff feature-${BUILD_NUMBER} -m 'JENKINS AUTO MERGE - CI: Auto-merge feature branch to main after successful pipeline'
                                    git push origin main                                        # Push changes to GitHub
                                """
                                MERGE_SUCCESS = 'true'                                         // Mark merge as successful
                                echo "[DEBUG] MERGE_SUCCESS=true"
                            } else {
                                echo "[INFO] Merge skipped - already merged by Jenkins."
                            }
                        }
                    } catch (e) {
                        echo "[ERROR] Merge failed: ${e.message}"
                        MERGE_SUCCESS = 'false'
                        echo "[DEBUG] MERGE_SUCCESS=false"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack helper

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

                slack.sendSlackNotification(message, color)  //  Send Slack update
            }
        }
    }  // Close post block
}  // Close pipeline block
