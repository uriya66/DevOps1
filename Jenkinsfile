pipeline {
    agent any  // Run on any available Jenkins agent

    options {
        disableConcurrentBuilds()  // Prevent simultaneous builds
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
                        echo "Starting SSH Agent and verifying authentication"
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
                script {
                    echo "Checking out the triggering branch"
                    BASE_BRANCH = sh(
                        script: "git log -1 --pretty=format:%D | grep -oE 'origin/(main|feature-test)' | cut -d/ -f2",
                        returnStdout: true
                    ).trim()
                    if (!BASE_BRANCH) {
                        error("Could not detect base branch (main or feature-test)")
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
                    GIT_BRANCH = "feature-${env.BUILD_NUMBER}"
                    echo "Creating new branch: ${GIT_BRANCH} from ${BASE_BRANCH}"
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
                    echo "Running tests"
                    . venv/bin/activate
                    gunicorn -w 1 -b 127.0.0.1:5000 app:app &
                    sleep 3
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
                    return GIT_BRANCH.startsWith('feature-') && DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                script {
                    try {
                        echo "Merging ${GIT_BRANCH} into main"
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
                    } catch (Exception e) {
                        MERGE_SUCCESS = 'false'
                        error("Merge to main failed: ${e.message}")
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
                    MERGE_SUCCESS == 'true',
                    DEPLOY_SUCCESS == 'true'
                )
                slack.sendSlackNotification(msg,
                    (MERGE_SUCCESS == 'true' && DEPLOY_SUCCESS == 'true') ? "good" : "danger"
                )
            }
        }
    }  // Close post block
}  // Close pipeline block
