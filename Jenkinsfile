pipeline {
    agent any

    environment {
        GIT_CREDENTIALS = 'Jenkins-GitHub-SSH' // Git SSH credentials ID
    }

    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm // Checkout source from GitHub
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent (credentials: [env.GIT_CREDENTIALS]) {
                    script {
                        echo 'Starting SSH Agent and verifying authentication.'
                        sh 'ssh-add -l'
                        sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo 'Checking out the repository.'
                    checkout([$class: 'GitSCM',
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[
                            url: 'git@github.com:uriya66/DevOps1.git',
                            credentialsId: env.GIT_CREDENTIALS
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            when {
                expression { return env.BRANCH_NAME ==~ /feature-.*/ }
            }
            steps {
                script {
                    echo 'Creating a new feature branch.'
                    sh '''
                        git rev-parse --git-dir
                        git remote remove origin || true
                        git remote add origin git@github.com:uriya66/DevOps1.git
                        git fetch origin
                        git checkout -b feature-${BUILD_NUMBER} origin/main
                        git push origin feature-${BUILD_NUMBER}
                    '''
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    echo Setting up Python virtual environment.
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
                    echo Starting Flask app for testing...
                    . venv/bin/activate
                    sleep 3
                    echo Running API tests.
                    python -m pytest test_app.py
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                '''
            }
        }
    }

    post {
        success {
            script {
                load 'slack_notifications.groovy'
                def slack = new slack_notifications()
                slack.sendSlackNotification("✅ Build and tests passed for ${env.JOB_NAME} #${env.BUILD_NUMBER}", "good")
            }

            script {
                echo 'Trying to merge feature branch into main.'
                withEnv(['GIT_AUTHOR_NAME=jenkins', 'GIT_AUTHOR_EMAIL=jenkins@example.com']) {
                    sh '''
                        git checkout main
                        git pull origin main
                        git merge --no-ff
                        git push origin main
                    '''
                }
            }
        }

        failure {
            script {
                load 'slack_notifications.groovy'
                def slack = new slack_notifications()
                slack.sendSlackNotification("❌ Build failed for ${env.JOB_NAME} #${env.BUILD_NUMBER}", "danger")
            }
        }
    }  // Close post block
}  // Close pipeline block

