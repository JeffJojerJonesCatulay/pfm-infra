pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = "jeffjojerjonescatulay/pfm-api-image-repo"
        DOCKER_TAG = "v1.0.${BUILD_NUMBER}"
    }

    stages {
        stage('Git Checkout') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/JeffJojerJonesCatulay/pfm-springboot-rest-api']]
                )
            }
        }
    
        stage('Build Docker Image') {
            steps {
                bat "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
            }
        }
        
        stage('Login to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    bat 'docker login -u %DOCKER_USER% --password %DOCKER_PASS%'
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                bat "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
            }
        }
        
        stage('Deploy Locally') {
            steps {
                withCredentials([file(credentialsId: 'env-file', variable: 'ENV_FILE')]) {
                    bat """
                        docker stop pfm-api || echo No container to stop
                        docker rm pfm-api || echo No container to remove
                        docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker run -d --env-file "%ENV_FILE%" -p 9010:9010 --name pfm-api ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                }
            }
        }

    }
}
