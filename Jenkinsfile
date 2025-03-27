pipeline {
    agent any  // Use any available Jenkins agent for the build

    options {
        disableConcurrentBuilds()  // Prevent concurrent builds
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // Git repository over SSH
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Branch name for each build
        GIT_BRANCH = ''  // Will hold the real branch name
        DEPLOY_SUCCESS = 'false'  // Deployment status
    }

    stages {
        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    if (lastCommitMessage.startsWith("Merge remote-tracking branch")) {
                        echo "Skipping build: This is a merge commit."
                        currentBuild.result = 'SUCCESS'
                        error("Stopping Pipeline: Merge commit detected.")
                    }
                }
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh 'ssh-add -l'
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection successful"
                            else
                                echo "ERROR: SSH authentication failed"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: REPO_URL,
                        credentialsId: 'Jenkins-GitHub-SSH'
                    ]]
                ])
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating feature branch ${BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}
                            git push origin ${BRANCH_NAME}
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // ✅ Set environment variable correctly
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e
                    echo "Setting up virtualenv"
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo "Running Flask app for testing..."
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    echo "Running pytest"
                    venv/bin/python -m pytest test_app.py
                    pkill gunicorn || true
                '''
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return currentBuild.result == null || currentBuild.result == 'SUCCESS'
                }
            }
            steps {
                script {
                    try {
                        sh '''
                            set -e
                            echo "Running deployment script"
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        DEPLOY_SUCCESS = 'true'
                    } catch (Exception e) {
                        DEPLOY_SUCCESS = 'false'
                        error("Deployment failed: ${e.message}")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith("feature-") && env.DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                script {
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
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'
                def msg = slack.constructSlackMessage(
                    env.BUILD_NUMBER,
                    env.BUILD_URL,
                    false,  // will be ignored if Merge failed
                    env.DEPLOY_SUCCESS == 'true'
                )
                def mergeSuccess = currentBuild.rawBuild.getLog().any { it.contains("git push origin main") }
                slack.sendSlackNotification(msg,
                    (mergeSuccess && env.DEPLOY_SUCCESS == 'true') ? "good" : "danger"
                )
            }
        }
    }  // Close post block
}  // Close pipeline block
