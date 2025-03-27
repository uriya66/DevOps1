pipeline {
    agent any  // Run the pipeline on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent multiple builds from running at the same time
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH URL for GitHub
        BRANCH_NAME = ''  // Feature branch name like feature-314
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo 'Authenticating SSH with GitHub...'
                        sh 'ssh-add -l'
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
                                echo "SSH authentication successful"
                            else
                                echo "SSH authentication failed!"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Checkout Triggering Branch') {
            steps {
                script {
                    echo 'Detecting triggering branch...'
                    def raw = sh(script: 'git log -1 --pretty=%D', returnStdout: true).trim()
                    def match = raw.find(/origin\\/(main|feature-test)/)
                    if (!match) {
                        error('Could not detect the triggering branch')
                    }
                    env.BASE_BRANCH = match.replace('origin/', '')
                    echo "Detected trigger branch: ${env.BASE_BRANCH}"

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.BASE_BRANCH}"]],
                        userRemoteConfigs: [[
                            url: env.REPO_URL,
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    env.BRANCH_NAME = "feature-${env.BUILD_NUMBER}"
                    echo "Creating branch ${env.BRANCH_NAME} from ${env.BASE_BRANCH}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${env.BRANCH_NAME}
                            git push origin ${env.BRANCH_NAME}
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
                    echo "Running Flask app for testing..."
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    echo "Running tests..."
                    venv/bin/pytest test_app.py
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e
                    echo "Deploying application..."
                    chmod +x deploy.sh
                    ./deploy.sh
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression { return env.BRANCH_NAME.startsWith("feature-") }
            }
            steps {
                script {
                    echo "Merging ${env.BRANCH_NAME} into main"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh '''
                            git config user.name "jenkins"
                            git config user.email "jenkins@example.com"
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${BRANCH_NAME}
                            git push origin main
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                def slack = load 'slack_notifications.groovy'
                def msg = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, true, true)
                slack.sendSlackNotification(msg, "good")
            }
        }
        failure {
            script {
                def slack = load 'slack_notifications.groovy'
                def msg = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, false, false)
                slack.sendSlackNotification(msg, "danger")
            }
        }
    }  // Close post block
}  // Close pipeline block
