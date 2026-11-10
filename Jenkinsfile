
pipeline {
    agent any

    options {
        timestamps()
        skipDefaultCheckout()
        disableConcurrentBuilds()
    }

    environment {
        IMAGE_NAME = 'neelima-cicd'
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
                    whoami
                    pwd
                    java -version
                    mvn -version
                    docker version
                    curl --version
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

                    docker image inspect \
                      "$IMAGE_NAME:$BUILD_NUMBER" >/dev/null

                    echo "Docker image built successfully"
                '''
            }
        }

        stage('Deploy to Dev') {
            steps {
                sh '''
                    set -eu

                    if docker container inspect \
                      neelima-cicd-dev >/dev/null 2>&1; then
                        docker rm -f neelima-cicd-dev
                    fi

                    docker run -d \
                      --name neelima-cicd-dev \
                      -p 127.0.0.1:8081:8080 \
                      -e SPRING_PROFILES_ACTIVE=dev \
                      "$IMAGE_NAME:$BUILD_NUMBER"

                    echo "Dev container started"
                '''
            }
        }

        stage('Verify Dev') {
            steps {
                sh '''
                    set -eu

                    for attempt in $(seq 1 20); do
                        if curl -fsS --max-time 3 \
                          http://127.0.0.1:8081/actuator/health; then
                            echo "DEV HEALTHY"
                            exit 0
                        fi

                        echo "Waiting for Dev: $attempt"
                        sleep 3
                    done

                    docker logs --tail 100 neelima-cicd-dev || true
                    exit 1
                '''
            }
        }

        stage('Deploy to Test') {
            steps {
                sh '''
                    set -eu

                    if docker container inspect \
                      neelima-cicd-test >/dev/null 2>&1; then
                        docker rm -f neelima-cicd-test
                    fi

                    docker run -d \
                      --name neelima-cicd-test \
                      -p 127.0.0.1:8082:8080 \
                      -e SPRING_PROFILES_ACTIVE=test \
                      "$IMAGE_NAME:$BUILD_NUMBER"

                    echo "Test container started"
                '''
            }
        }

        stage('Verify Test') {
            steps {
                sh '''
                    set -eu

                    for attempt in $(seq 1 20); do
                        if curl -fsS --max-time 3 \
                          http://127.0.0.1:8082/actuator/health; then
                            echo "TEST HEALTHY"
                            exit 0
                        fi

                        echo "Waiting for Test: $attempt"
                        sleep 3
                    done

                    docker logs --tail 100 neelima-cicd-test || true
                    exit 1
                '''
            }
        }

        stage('Production Approval') {
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input(
                        message: 'Dev and Test passed. Deploy to Production?',
                        ok: 'Approve Production',
                        submitter: 'YOUR_JENKINS_USERNAME'
                    )
                }
            }
        }

        stage('Deploy to Production') {
            steps {
                sh '''
                    set -eu

                    if docker container inspect \
                      neelima-cicd-prod >/dev/null 2>&1; then
                        docker rm -f neelima-cicd-prod
                    fi

                    docker run -d \
                      --name neelima-cicd-prod \
                      -p 127.0.0.1:8083:8080 \
                      -e SPRING_PROFILES_ACTIVE=prod \
                      "$IMAGE_NAME:$BUILD_NUMBER"

                    echo "Production container started"
                '''
            }
        }

        stage('Verify Production') {
            steps {
                sh '''
                    set -eu

                    for attempt in $(seq 1 20); do
                        if curl -fsS --max-time 3 \
                          http://127.0.0.1:8083/actuator/health; then
                            echo "PRODUCTION HEALTHY"
                            exit 0
                        fi

                        echo "Waiting for Production: $attempt"
                        sleep 3
                    done

                    docker logs --tail 100 neelima-cicd-prod || true
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
            echo 'DEV TEST PRODUCTION DEPLOYMENT SUCCESSFUL'
        }

        failure {
            echo 'PIPELINE FAILED - CHECK CONSOLE OUTPUT'
        }

        aborted {
            echo 'PIPELINE ABORTED - NO PRODUCTION APPROVAL'
        }
    }
}
