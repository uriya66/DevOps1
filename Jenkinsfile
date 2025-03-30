pipeline {
    agent any

    environment {
        DEPLOY_SUCCESS = false  // Global flag for deploy status
        MERGE_SUCCESS = false   // Global flag for merge status
        BRANCH_NAME = ''        // Global branch name to use across pipeline
    }

    stages {
        stage('Start SSH Agent') {
            steps {
                sshagent(['git']) {
                    script {
                        echo "Starting SSH Agent and verifying authentication"
                        sh 'ssh-add -l'
                        sh 'ssh -o StrictHostKeyChecking=no -T git@github.com | grep -q "successfully authenticated" && echo "SSH connection successful"'
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Detecting triggering branch using Git log"
                    def detectedBranch = sh(
                        script: "git log -1 --pretty=format:%D | grep -oE origin/(main|feature-test) | cut -d/ -f2",
                        returnStdout: true
                    ).trim()

                    echo "Trigger branch is: ${detectedBranch}"
                    env.BRANCH_NAME = detectedBranch  // Assign to global env var
                    echo "env.BRANCH_NAME set to: ${env.BRANCH_NAME}"

                    checkout([$class: 'GitSCM',
                        branches: [[name: "origin/${env.BRANCH_NAME}"]],
                        userRemoteConfigs: [[
                            url: 'git@github.com:uriya66/DevOps1.git',
                            credentialsId: 'Jenkins-GitHub-SSH'
                        ]]
                    ])
                }
            }
        }

        stage('Create Feature Branch') {
            steps {
                script {
                    def featureBranch = "feature-${env.BUILD_NUMBER}"
                    echo "Creating feature branch: ${featureBranch} from ${env.BRANCH_NAME}"
                    sh """
                        git checkout -b ${featureBranch}
                        git push origin ${featureBranch}
                    """
                    env.BRANCH_NAME = featureBranch
                    echo "env.BRANCH_NAME updated to: ${env.BRANCH_NAME}"
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
                    sleep 3
                    echo Executing API tests...
                    pytest test_app.py
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    try {
                        sh '''
                            set -e
                            echo Running deployment script...
                            chmod +x deploy.sh
                            ./deploy.sh
                        '''
                        env.DEPLOY_SUCCESS = true
                    } catch (Exception e) {
                        echo "Deployment failed: ${e.getMessage()}"
                        env.DEPLOY_SUCCESS = false
                    }
                }
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    return env.DEPLOY_SUCCESS == 'true'
                }
            }
            steps {
                script {
                    try {
                        def branchToMerge = env.BRANCH_NAME
                        echo "Current branch for merge verification: ${branchToMerge}"
                        echo "Attempting to merge ${branchToMerge} into main..."

                        sh '''
                            git config user.name jenkins
                            git config user.email jenkins@example.com
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${BRANCH_NAME}
                            git push origin main
                        '''

                        env.MERGE_SUCCESS = true
                    } catch (Exception e) {
                        echo "❌ Merge to main failed: ${e.getMessage()}"
                        env.MERGE_SUCCESS = false
                        error("Merge to main failed")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def notifier = load('slack_notifications.groovy')
                def message = notifier.constructSlackMessage(
                    env.BUILD_NUMBER,
                    currentBuild.absoluteUrl,
                    env.MERGE_SUCCESS == 'true',
                    env.DEPLOY_SUCCESS == 'true'
                )
                notifier.sendSlackNotification(message, env.MERGE_SUCCESS == 'true' && env.DEPLOY_SUCCESS == 'true' ? 'good' : 'danger')
            }
        }
    }
}

