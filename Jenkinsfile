pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running simultaneously
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repository URL
        BASE_BRANCH = ''  // Will hold the original triggering branch
        BRANCH_NAME = ''  // Will hold the generated feature-${BUILD_NUMBER} branch name
        GIT_BRANCH = ''  // Alias for feature branch created in this build
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication"
                        sh 'ssh-add -l'  // List loaded SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection successful"
                            else
                                echo "ERROR: SSH connection failed!"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout SCM') {
            steps {
                checkout scm  // Standard Jenkins checkout

                script {
                    // Get the branch name from Git environment
                    BASE_BRANCH = env.GIT_BRANCH?.replaceFirst(/^origin\//, '') ?: 'main'
                    echo "Detected trigger branch: ${BASE_BRANCH}"

                    if (!(BASE_BRANCH == 'main' || BASE_BRANCH == 'feature-test')) {
                        error("This pipeline only supports pushes to main or feature-test.")
                    }
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Define feature branch name
                    GIT_BRANCH = BRANCH_NAME  // Set alias for use later
                    echo "Creating branch ${BRANCH_NAME} from ${BASE_BRANCH}"
                    sh """
                        git checkout -b ${BRANCH_NAME}
                        git push origin ${BRANCH_NAME}
                    """
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    echo "Setting up Python virtual environment"
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    venv/bin/pip install --upgrade pip
                    venv/bin/pip install flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Starting Flask app for testing..."
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    echo "Running API tests"
                    venv/bin/pytest test_app.py
                    pkill gunicorn || true
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
                    // Only allow merge if triggered from main or feature-test
                    return BASE_BRANCH == 'main' || BASE_BRANCH == 'feature-test'
                }
            }
            steps {
                script {
                    echo "Merging ${GIT_BRANCH} into main"
                    sh '''
                        git config user.name "jenkins"
                        git config user.email "jenkins@example.com"
                        git checkout main
                        git pull origin main
                        git merge --no-ff ${GIT_BRANCH}
                        git push origin main
                    '''
                }
            }
        }
    }

    post {
        success {
            script {
                def slack = load 'slack_notifications.groovy'  // Load Slack helper
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, true, true)
                slack.sendSlackNotification(message, "good")
            }
        }
        failure {
            script {
                def slack = load 'slack_notifications.groovy'
                def message = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)
                slack.sendSlackNotification(message, "danger")
            }
        }
    }  // Close post block
}  // Close pipeline block
