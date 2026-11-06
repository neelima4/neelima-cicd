
pipeline {
    agent any

    options {
        timestamps()
        skipDefaultCheckout()
    }

    environment {
        IMAGE_NAME = 'neelima-cicd'
        CONTAINER_NAME = 'neelima-cicd-app'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Environment Check') {
            steps {
                sh '''
                    set -eu
                    echo "Running as:"
                    whoami

                    echo "Workspace:"
                    pwd

                    java -version
                    mvn -version
                    docker version
                '''
            }
        }

        stage('Build and Unit Test') {
            steps {
                sh 'mvn -B clean test'
            }
        }

        stage('Package JAR') {
            steps {
                sh 'mvn -B -DskipTests package'
                sh 'ls -lh target/*.jar'
            }
        }

        stage('Archive Artifact') {
            steps {
                archiveArtifacts(
                    artifacts: 'target/neelima-cicd-1.0.0.jar',
                    fingerprint: true
                )
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -eu
                    docker build \
                      -t "$IMAGE_NAME:$BUILD_NUMBER" .

                    docker images "$IMAGE_NAME"
                '''
            }
        }

        stage('Deploy Container') {
            steps {
                sh '''
                    set -eu

                    echo "Checking existing lab container"

                    if docker container inspect \
                      "$CONTAINER_NAME" >/dev/null 2>&1; then
                        docker rm -f "$CONTAINER_NAME"
                    fi

                    docker run -d \
                      --name "$CONTAINER_NAME" \
                      -p 8081:8080 \
                      "$IMAGE_NAME:$BUILD_NUMBER"

                    docker ps \
                      --filter "name=$CONTAINER_NAME"
                '''
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    set -eu

                    for attempt in $(seq 1 20); do
                        if curl -fsS --max-time 3 \
                          http://127.0.0.1:8081/actuator/health; then

                            echo "Application is healthy"
                            exit 0
                        fi

                        echo "Waiting for application: $attempt"
                        sleep 3
                    done

                    echo "Health check failed"
                    docker logs --tail 100 "$CONTAINER_NAME" || true
                    exit 1
                '''
            }
        }

    }

    post {
        always {
            junit(
                testResults: 'target/surefire-reports/TEST-*.xml',
                allowEmptyResults: true
            )

            echo 'Pipeline execution completed'
        }

        success {
            echo 'CI/CD DEPLOYMENT SUCCESSFUL'
        }

        failure {
            echo 'CI/CD PIPELINE FAILED'

            sh '''
                echo "Container status:"
                docker ps -a || true

                echo "Disk usage:"
                df -h . || true
            '''
        }
    }
}
