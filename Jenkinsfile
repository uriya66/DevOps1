pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent concurrent executions
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH URL for GitHub
        BASE_BRANCH = ''  // Will store the original triggering branch
        BRANCH_NAME = ''  // Will store the new feature-${BUILD_NUMBER} branch
        GIT_BRANCH = ''  // Will hold the actual working branch
        MERGE_SUCCESS = 'false'  // Status for Slack
        DEPLOY_SUCCESS = 'false'  // Status for Slack
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent"  // Debug message
                        sh 'ssh-add -l'
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH connection successful"
                            else
                                echo "SSH authentication failed"
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
                    echo "Detecting original trigger branch (main or feature-test)"
                    BASE_BRANCH = sh(script: "git log -1 --pretty=format:%D | grep -oE 'origin/(main|feature-test)' | cut -d/ -f2", returnStdout: true).trim()
                    if (!BASE_BRANCH) {
                        error("❌ Could not detect trigger branch.")
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
                    echo "Creating branch: ${BRANCH_NAME} from ${BASE_BRANCH}"
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
                    echo "Setting up virtual environment"
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
                    echo "Running tests with Gunicorn"
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    echo "Running pytest"
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
                            echo "Starting deploy.sh script"
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        env.DEPLOY_SUCCESS = 'true'
                    } catch (Exception e) {
                        echo "❌ Deploy failed: ${e.message}"
                        env.DEPLOY_SUCCESS = 'false'
                        error("Deployment failed: ${e.message}")
                    }
                }
            }
        }

        stage('Merge to Main') {
            steps {
                script {
                    try {
                        echo "Merging branch ${env.GIT_BRANCH} into main"
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh """
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main
                                git pull origin main
                                git branch --set-upstream-to=origin/main main
                                git merge --no-ff ${env.GIT_BRANCH}
                                git push origin main
                            """
                        }
                        echo "✅ Merge successful"
                        env.MERGE_SUCCESS = 'true'
                    } catch (Exception e) {
                        echo "❌ Merge failed: ${e.message}"
                        env.MERGE_SUCCESS = 'false'
                        error("Merge to main failed: ${e.message}")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    def slack = load 'slack_notifications.groovy'
                    def message = slack.constructSlackMessage(
                        env.BUILD_NUMBER,
                        env.BUILD_URL,
                        env.MERGE_SUCCESS == 'true',
                        env.DEPLOY_SUCCESS == 'true'
                    )
                    def color = (env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true') ? 'good' : 'danger'
                    slack.sendSlackNotification(message, color)
                } catch (Exception e) {
                    echo "❌ Slack notification error: ${e.message}"
                }
            }
        }
    }
}

