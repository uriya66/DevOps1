pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub SSH repository URL
        BASE_BRANCH = ''
        BRANCH_NAME = ''
        MERGE_SUCCESS = 'false'  // Track if merge succeeded
        DEPLOY_SUCCESS = 'false'  // Track if deploy succeeded
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
                    BRANCH_NAME = "feature-${env.BUILD_NUMBER}"
                    echo "Creating a new branch from ${BASE_BRANCH} → ${BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}
                            git push origin ${BRANCH_NAME}
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME
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
                        DEPLOY_SUCCESS = 'true'  // Set success
                    } catch (err) {
                        DEPLOY_SUCCESS = 'false'  // Set failure
                        error("Deploy failed: ${err.message}")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith('feature-')
                }
            }
            steps {
                script {
                    try {
                        echo "Merging ${GIT_BRANCH} into main"
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
                        MERGE_SUCCESS = 'true'  // Set merge success
                    } catch (err) {
                        MERGE_SUCCESS = 'false'  // Set merge failure
                        error("Merge to main failed: ${err.message}")
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
