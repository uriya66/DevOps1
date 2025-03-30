pipeline {
    agent any

    environment {
        DEPLOY_SUCCESS = false  // Default value
        GIT_BRANCH = ''         // Will be set dynamically
    }

    stages {

        stage('Declarative: Checkout SCM') {
            steps {
                checkout scm
            }
        }

        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh 'ssh -o StrictHostKeyChecking=no -T git@github.com || true'
                }
            }
        }

        stage('Checkout Main') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:uriya66/DevOps1.git',
                        credentialsId: 'Jenkins-GitHub-SSH'
                    ]]
                ])
            }
        }

        stage('Create Feature Branch') {
            when {
                expression {
                    def rawBranch = sh(returnStdout: true, script: "git rev-parse --abbrev-ref HEAD").trim()
                    env.GIT_BRANCH = rawBranch
                    echo "[DEBUG] Detected branch: ${env.GIT_BRANCH}"
                    return env.GIT_BRANCH ==~ /origin\/(main|feature-test)/
                }
            }
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        def newBranch = "feature-${BUILD_NUMBER}"
                        sh "git checkout -b ${newBranch}"
                        sh "git push origin ${newBranch}"
                        env.GIT_BRANCH = newBranch
                        echo "[DEBUG] Created and pushed new branch: ${newBranch}"
                    }
                }
            }
        }

        stage('Build') {
            steps {
                echo "[DEBUG] Installing dependencies"
                sh '''
                    set -e
                    [ ! -d venv ] && python3 -m venv venv
                    . venv/bin/activate
                    pip install --upgrade pip flask pytest gunicorn requests
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e
                    . venv/bin/activate
                    sleep 5
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 5
                    pytest test_app.py
                    pkill -f gunicorn || true
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        sh '''
                            set -e
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        echo "[INFO] Deployment succeeded"
                        DEPLOY_SUCCESS = true
                    } catch (err) {
                        echo "[ERROR] Deployment failed: ${err}"
                        DEPLOY_SUCCESS = false
                        error("Deployment failed.")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    def isFeature = env.GIT_BRANCH ==~ /^feature-[0-9]+$/
                    echo "[DEBUG] GIT_BRANCH=${env.GIT_BRANCH}, DEPLOY_SUCCESS=${DEPLOY_SUCCESS}, isFeature=${isFeature}"
                    return isFeature && DEPLOY_SUCCESS.toString() == "true"
                }
            }
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    sh '''
                        git checkout main
                        git pull origin main
                        git merge origin/${GIT_BRANCH}
                        git push origin main
                    '''
                }
            }
        }
    }

    post {
        always {
            script {
                def branch = sh(returnStdout: true, script: "git rev-parse --abbrev-ref HEAD").trim()
                def commit = sh(returnStdout: true, script: "git log -1 --pretty=%B").trim()
                def publicIP = sh(returnStdout: true, script: "curl -s http://checkip.amazonaws.com").trim()
                def color = DEPLOY_SUCCESS.toString() == "true" ? "good" : "danger"

                slackSend (
                    channel: '#jenkis_alerts',
                    color: color,
                    message: "*Branch:* ${branch}\n*Commit:* ${commit}\n*Deployed:* ${DEPLOY_SUCCESS}\n*App:* http://${publicIP}:5000",
                    tokenCredentialId: 'Jenkins-Slack-Token'
                )
            }
        }
    }  // Close post block
}  // Close pipeline block

