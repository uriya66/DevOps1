pipeline {
    agent any

    options {
        disableConcurrentBuilds()  // Prevent multiple parallel runs
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Git SSH URL
        BASE_BRANCH = ''  // Will hold triggering branch
        BRANCH_NAME = ''  // Will hold new feature branch
        GIT_BRANCH = ''  // Used to validate merge
        DEPLOY_SUCCESS = 'false'  // Track deploy
        MERGE_SUCCESS = 'false'  // Track merge
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication"
                        sh 'ssh-add -l'
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

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out the triggering branch"
                    BASE_BRANCH = sh(script: "git log -1 --pretty=format:%D | grep -oE 'origin/(main|feature-test)' | cut -d/ -f2", returnStdout: true).trim()
                    if (!BASE_BRANCH) {
                        error("Could not detect triggering branch")
                    }
                    echo "Trigger branch is: ${BASE_BRANCH}"
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
                    BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Set feature branch name
                    env.GIT_BRANCH = BRANCH_NAME  // Save into env variable
                    echo "Creating a new branch from ${BASE_BRANCH} → ${BRANCH_NAME}"
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
                script {
                    try {
                        sh '''
                            set -e
                            echo "Running deployment script"
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        env.DEPLOY_SUCCESS = 'true'  // Mark deploy success
                    } catch (err) {
                        env.DEPLOY_SUCCESS = 'false'  // Mark deploy failure
                        error("Deploy failed: ${err.message}")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith('feature-')  // Merge only if feature branch
                }
            }
            steps {
                script {
                    try {
                        echo "Merging ${env.GIT_BRANCH} into main"
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
                        env.MERGE_SUCCESS = 'true'  // Mark merge success
                    } catch (err) {
                        env.MERGE_SUCCESS = 'false'  // Mark merge failure
                        error("Merge failed: ${err.message}")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'
                def message = slack.constructSlackMessage(
                    env.BUILD_NUMBER,
                    env.BUILD_URL,
                    env.MERGE_SUCCESS == 'true',
                    env.DEPLOY_SUCCESS == 'true'
                )
                def color = (env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true') ? "good" : "danger"
                slack.sendSlackNotification(message, color)
            }
        }
    }  // Close post block
}  // Close pipeline block
