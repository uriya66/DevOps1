pipeline {
    agent any

    environment {
        GIT_BRANCH = ""  // Global variable to be updated later with feature-${BUILD_NUMBER}
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo 'Starting SSH Agent and verifying authentication'
                        sh 'ssh-add -l'
                        // SSH test with fallback to avoid pipeline failure
                        sh '''
                            echo "Testing SSH connection to GitHub..."
                            ssh -o StrictHostKeyChecking=no -T git@github.com || true
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo 'Detecting triggering branch using Git log'
                    def base = sh(script: "git log -1 --pretty=format:%D | grep -oE origin/(main|feature-test) | cut -d/ -f2", returnStdout: true).trim()
                    echo "Trigger branch is: ${base}"
                    env.BASE_BRANCH = base
                    env.GIT_BRANCH = "feature-${env.BUILD_NUMBER}"  // Set GIT_BRANCH globally
                }

                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${env.BASE_BRANCH}"]],
                    userRemoteConfigs: [[
                        url: 'git@github.com:uriya66/DevOps1.git',
                        credentialsId: 'Jenkins-GitHub-SSH'
                    ]]
                ])
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    echo "Creating feature branch: ${env.GIT_BRANCH} from ${env.BASE_BRANCH}"
                    withEnv(["BRANCH_NAME=${env.GIT_BRANCH}"]) {
                        sh """
                            git checkout -b ${env.GIT_BRANCH}
                            git push origin ${env.GIT_BRANCH}
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    echo Setting up Python virtual environment...
                    [ ! -d venv ] && python3 -m venv venv
                    . venv/bin/activate
                    pip install --upgrade pip
                    pip install flask requests pytest gunicorn
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    echo Running tests using Gunicorn...
                    . venv/bin/activate
                    sleep 3
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 2
                    echo Executing API tests...
                    venv/bin/pytest test_app.py
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    echo 'Running deployment script...'
                    sh '''
                        set -e
                        chmod +x deploy.sh
                        ./deploy.sh
                    '''
                    env.DEPLOY_SUCCESS = "true"
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.DEPLOY_SUCCESS == "true"
                }
            }
            steps {
                script {
                    echo "Attempting to merge ${env.GIT_BRANCH} into main..."
                    withEnv(["BRANCH_NAME=${env.GIT_BRANCH}"]) {
                        sh '''
                            git config user.name jenkins
                            git config user.email jenkins@example.com
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${BRANCH_NAME}
                            git push origin main
                        '''
                    }
                    env.MERGE_SUCCESS = "true"
                }
            }
        }
    }

    post {
        always {
            script {
                def notifier = load('slack_notifications.groovy')
                def message = notifier.constructSlackMessage(
                    currentBuild.number,
                    env.BUILD_URL,
                    env.MERGE_SUCCESS == "true",
                    env.DEPLOY_SUCCESS == "true"
                )
                def color = currentBuild.result == 'SUCCESS' ? 'good' : 'danger'
                notifier.sendSlackNotification(message, color)
            }
        }
    }
}

