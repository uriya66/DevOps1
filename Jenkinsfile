pipeline {
    agent any  // Run the pipeline on any available agent

    options {
        disableConcurrentBuilds()  // Prevent concurrent builds
    }

    triggers {
        pollSCM('* * * * *')  // Trigger build on any push
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // GitHub repo via SSH
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}"  // Always create a feature branch for every push
    }

    stages {

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication."
                        sh "ssh-add -l"  // List active SSH keys
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH authentication succeeded."
                            else
                                echo "SSH authentication failed."
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
                    echo "Checking out 'main' branch."
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
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating feature branch: ${BRANCH_NAME}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${BRANCH_NAME}
                            git push origin ${BRANCH_NAME}
                        """
                    }
                    env.GIT_BRANCH = BRANCH_NAME  // Save branch name for later
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e  # Exit on error
                    echo "Setting up virtual environment."
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
                    echo "Running API tests."
                    . venv/bin/activate
                    venv/bin/python -m pytest test_app.py
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH.startsWith("feature-")  // Merge only feature branches
                }
            }
            steps {
                script {
                    echo "Checking test results before merging."
                    if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
                        sh '''
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${GIT_BRANCH}
                            git push origin main
                        '''
                        echo "Merge successful."
                        def slack = load 'slack_notifications.groovy'
                        slack.sendSlackNotification("Merge to main succeeded for branch ${GIT_BRANCH}", "good")
                    } else {
                        echo "Skipping merge due to failure."
                        def slack = load 'slack_notifications.groovy'
                        slack.sendSlackNotification("Merge failed for branch ${GIT_BRANCH}", "danger")
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return currentBuild.result == null || currentBuild.result == 'SUCCESS'  // Deploy only on success
                }
            }
            steps {
                sh 'bash deploy.sh'
                script {
                    def slack = load 'slack_notifications.groovy'
                    slack.sendSlackNotification("Deployment successful for build #${env.BUILD_NUMBER}", "good")
                }
            }
        }
    }

    post {
        always {
            script {
                def slack = load 'slack_notifications.groovy'
                def msg = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL)
                slack.sendSlackNotification(msg, "good")
            }
        }
    }
}

