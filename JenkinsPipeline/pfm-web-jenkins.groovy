pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = "jeffjojerjonescatulay/pfm-web-image-repo"
        DOCKER_TAG = "v1.0.${BUILD_NUMBER}"
    }

    stages {
        stage('Git Checkout') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/JeffJojerJonesCatulay/pfm-web']]
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
                bat """
                    docker stop pfm-web || echo No container to stop
                    docker rm pfm-web || echo No container to remove
                    docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}
                    docker run -d -p 9011:80 --name pfm-web ${DOCKER_IMAGE}:${DOCKER_TAG}
                """
            }
        }

    }
}
