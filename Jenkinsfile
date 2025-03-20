pipeline {
    agent any
    
    options {
        disableConcurrentBuilds() // No concurrent builds allowed
    }

    environment {
        REPO_URL = 'git@github.com:uriya66/DevOps1.git'
        BRANCH_NAME = "feature-${env.BUILD_NUMBER}" // Create feature branch with the build number
    }

    stages {
        stage('Skip Redundant Merge Builds') {
            steps {
                script {
                    def lastCommitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    if (lastCommitMessage.startsWith("Merge remote-tracking branch")) {
                        echo "Skipping build: This is a merge commit."
                        currentBuild.result = 'SUCCESS'
                        error("Stopping Pipeline: Merge commit detected.")
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
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

        stage('Create Feature Branch') {
            when {
                expression {
                    return !(env.GIT_BRANCH?.startsWith("feature-") ?: false)
                }
            }
            steps {
                script {
                    sh "git checkout -b ${BRANCH_NAME}"
                    sh "git push origin ${BRANCH_NAME}"
                    env.GIT_BRANCH = BRANCH_NAME
                }
            }
        }

        stage('Build') {
            steps {
                sh """
                    set -e
                    if [ ! -d "venv" ]; then python3 -m venv venv; fi
                    . venv/bin/activate
                    venv/bin/python -m pip install --upgrade pip
                    venv/bin/python -m pip install flask requests pytest gunicorn
                """
            }
        }

        stage('Test') {
            steps {
                sh """
                    set -e
                    . venv/bin/activate
                    venv/bin/python -m pytest test_app.py
                """
            }
        }

        stage('Merge to Main') {
            when {
                expression {
                    def cleanBranch = env.GIT_BRANCH?.replace("origin/", "") ?: ""
                    return cleanBranch.startsWith("feature-")
                }
            }
            steps {
                script {
                    def cleanBranch = env.GIT_BRANCH.replace("origin/", "")
                    withEnv(["SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}"]) {
                        sh """
                            git checkout main
                            git pull origin main
                            git merge --no-ff ${cleanBranch} || echo "Nothing to merge"
                            git push origin main
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Build & Tests passed. Merging branch automatically."
        }
        failure {
            echo "Build or Tests failed. NOT merging to main."
        }
    }
}

