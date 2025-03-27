pipeline {
    agent any

    options {
        disableConcurrentBuilds()  // Prevent overlapping builds
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'  // SSH URL for GitHub
        BASE_BRANCH = ''  // Will hold the source branch (main or feature-test)
        GIT_BRANCH = ''  // Will hold the new feature-* branch name
        DEPLOY_SUCCESS = 'false'  // Deployment status
        MERGE_SUCCESS = 'false'  // Merge status
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(credentials: ['Jenkins-GitHub-SSH']) {
                    script {
                        echo "Starting SSH Agent"
                        sh 'ssh-add -l'
                        sh '''
                            if ssh -o StrictHostKeyChecking=no -T git@github.com | grep -q "successfully authenticated"; then
                                echo "SSH OK"
                            else
                                echo "SSH FAIL"
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
                    echo "Detecting trigger branch"
                    BASE_BRANCH = sh(
                        script: "git log -1 --pretty=format:%D | grep -oE 'origin/(main|feature-test)' | cut -d/ -f2",
                        returnStdout: true
                    ).trim()
                    if (!BASE_BRANCH) {
                        error("Could not detect base branch (main or feature-test)")
                    }
                    echo "Trigger branch: ${BASE_BRANCH}"

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
                    GIT_BRANCH = "feature-${env.BUILD_NUMBER}"
                    echo "Creating branch: ${GIT_BRANCH} from ${BASE_BRANCH}"
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout -b ${GIT_BRANCH}
                            git push origin ${GIT_BRANCH}
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    echo "Building venv"
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
                    echo "Starting test app"
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
                        DEPLOY_SUCCESS = 'true'
                    } catch (err) {
                        DEPLOY_SUCCESS = 'false'
                        error("Deploy failed: ${err}")
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return GIT_BRANCH.startsWith("feature-") && DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                script {
                    try {
                        echo "Merging ${GIT_BRANCH} → main"
                        withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                            sh """
                                git config user.name "jenkins"
                                git config user.email "jenkins@example.com"
                                git checkout main
                                git pull origin main
                                git merge --no-ff ${GIT_BRANCH}
                                git push origin main
                            """
                        }
                        MERGE_SUCCESS = 'true'
                    } catch (err) {
                        MERGE_SUCCESS = 'false'
                        error("Merge failed: ${err}")
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                echo "Post SUCCESS block"
                echo "DEPLOY_SUCCESS=${DEPLOY_SUCCESS}"
                echo "MERGE_SUCCESS=${MERGE_SUCCESS}"
                def slack = load 'slack_notifications.groovy'
                def msg = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, MERGE_SUCCESS == 'true', DEPLOY_SUCCESS == 'true')
                slack.sendSlackNotification(msg, "good")
            }
        }

        failure {
            script {
                echo "Post FAILURE block"
                echo "DEPLOY_SUCCESS=${DEPLOY_SUCCESS}"
                echo "MERGE_SUCCESS=${MERGE_SUCCESS}"
                def slack = load 'slack_notifications.groovy'
                def msg = slack.constructSlackMessage(env.BUILD_NUMBER, env.BUILD_URL, MERGE_SUCCESS == 'true', DEPLOY_SUCCESS == 'true')
                slack.sendSlackNotification(msg, "danger")
            }
        }
    }  // Close post block
}  // Close pipeline block
