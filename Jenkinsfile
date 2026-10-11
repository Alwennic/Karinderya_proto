pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME = 'karinderya_proto'
        TAG = "${env.BUILD_NUMBER}"
    }

    triggers {
        githubPush()                 // instant: GitHub webhook through ngrok
        pollSCM('H/2 * * * *')       // fallback: check Git every 2 minutes
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Test') {
            steps {
                // A failing test fails the build here, before anything is deployed
                sh 'docker build --target test -t karinderya_proto/catalog-api:test ./catalog-api'
                sh 'docker build --target test -t karinderya_proto/auth-api:test ./auth-api'
            }
        }

        stage('Build Images') {
            steps { sh 'docker compose build' }
        }

        stage('Deploy') {
            steps {
                withCredentials([file(credentialsId: 'karinderya_proto-env', variable: 'ENV_FILE')]) {
                    sh 'cp "$ENV_FILE" .env'
                    sh 'docker compose up -d --no-build --remove-orphans'
                }
            }
        }

        stage('Smoke Test') {
            steps { sh 'sh smoke-test.sh' }
        }
    }

    post {
        success { echo "Karinderya_proto build ${TAG} is live at http://localhost:8080" }
        failure { echo "Build ${TAG} failed. Check the stage logs. Roll back with: TAG=<previous> docker compose -p karinderya_proto up -d --no-build" }
        always  { sh 'rm -f .env' }
    }
}
