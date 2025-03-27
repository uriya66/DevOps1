pipeline {
    agent any  // Use any available Jenkins agent

    environment {
        BASE_BRANCH = ""  // Base branch that triggered the pipeline
        BRANCH_NAME = ""  // Feature branch to be created
        mergeSuccess = null  // Track success of Merge to Main
        deploySuccess = null  // Track success of Deploy
    }

    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm  // Initial checkout to read the Jenkinsfile
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(['git']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication"
                        sh 'ssh-add -l'  // List loaded SSH keys
                        sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || echo SSH connection failed'  // Test SSH to GitHub
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the triggering branch"
                    BASE_BRANCH = sh(script: "git log -1 --pretty=format:%D | grep -oE origin/(main|feature-test) | cut -d/ -f2", returnStdout: true).trim()  // Extract triggering branch
                    echo "Trigger branch is: ${BASE_BRANCH}"
                    checkout([$class: 'GitSCM',
                              branches: [[name: "origin/${BASE_BRANCH}"]],
                              userRemoteConfigs: [[url: 'git@github.com:uriya66/DevOps1.git', credentialsId: 'Jenkins-GitHub-SSH']]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    BRANCH_NAME = "feature-${BUILD_NUMBER}"  // Create branch name from build number
                    echo "Creating a new branch from ${BASE_BRANCH} → ${BRANCH_NAME}"
                    withEnv(["GIT_SSH_COMMAND=ssh -o StrictHostKeyChecking=no"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}  # Create new branch locally
                            git push origin ${BRANCH_NAME}  # Push to GitHub
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit on error
                    echo Setting up Python virtual environment
                    [ ! -d venv ] && python3 -m venv venv  # Create venv if not exists
                    . venv/bin/activate
                    pip install --upgrade pip
                    pip install flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e  # Exit on error
                    echo Starting tests with Gunicorn
                    . venv/bin/activate
                    sleep 3
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 2
                    pytest test_app.py
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        sh '''
                            set -e  # Exit on error
                            echo Running deployment script
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        deploySuccess = true  // Mark deploy as successful
                    } catch (Exception e) {
                        deploySuccess = false  // Mark deploy as failed
                        error("Deploy failed")  // Fail pipeline if deploy fails
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression { return BASE_BRANCH == "main" || BASE_BRANCH == "feature-test" }  // Only allow auto-merge from valid branches
            }
            steps {
                script {
                    try {
                        echo "Merging ${BRANCH_NAME} into main"
                        withEnv(["GIT_SSH_COMMAND=ssh -o StrictHostKeyChecking=no"]) {
                            sh '''
                                git checkout main  # Switch to main branch
                                git pull origin main  # Update main
                                git merge --no-ff ${BRANCH_NAME} -m "CI: Auto-merge ${BRANCH_NAME} into main"
                                git push origin main  # Push changes to GitHub
                            '''
                        }
                        mergeSuccess = true  // Mark merge as successful
                    } catch (Exception e) {
                        mergeSuccess = false  // Mark merge as failed
                        error("Merge to main failed")  // Fail pipeline
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def slackUtils = load("slack_notifications.groovy")  // Load Slack utility script
                def message = slackUtils.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)  // Build Slack message
                def color = (mergeSuccess == false || deploySuccess == false) ? "danger" : "good"  // Color based on status
                slackUtils.sendSlackNotification(message, color)  // Send Slack message
            }
        }
    }  // Close post block
}  // Close pipeline block
