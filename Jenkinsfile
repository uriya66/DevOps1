pipeline {
    agent any  // Run on any available agent

    options {
        disableConcurrentBuilds()  // Prevent concurrent builds
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH URL
        GIT_BRANCH = ''  // Will hold the feature-${BUILD_NUMBER}
        BASE_BRANCH = ''  // Will hold main or feature-test
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "🔐 Starting SSH agent and testing GitHub SSH connection"
                        sh 'ssh-add -l'
                        sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "🔍 Detecting triggering branch..."
                    env.BASE_BRANCH = sh(
                        script: "git log -1 --pretty=format:%D | grep -oE 'origin/(main|feature-test)' | cut -d/ -f2",
                        returnStdout: true
                    ).trim()

                    if (!env.BASE_BRANCH) {
                        error("❌ Could not detect the triggering branch")
                    }

                    echo "✅ Trigger branch: ${env.BASE_BRANCH}"

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
                    env.GIT_BRANCH = "feature-${env.BUILD_NUMBER}"  // Set final branch name
                    echo "🌿 Creating new branch ${env.GIT_BRANCH} from ${env.BASE_BRANCH}"

                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
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
                    echo "🛠 Setting up Python venv and installing dependencies"
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
                    echo "🧪 Running tests using Gunicorn"
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
                    venv/bin/pytest test_app.py
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e
                    echo "🚀 Running deployment script"
                    chmod +x deploy.sh
                    ./deploy.sh
                '''
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.GIT_BRANCH?.startsWith('feature-')  // Only if feature- branch
                }
            }
            steps {
                script {
                    echo "🔁 Merging ${env.GIT_BRANCH} into main..."

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
        success {
            script {
                def slack = load 'slack_notifications.groovy'
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
    }
}

