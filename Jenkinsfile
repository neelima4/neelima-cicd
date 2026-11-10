
pipeline {
    agent any

    options {
        timestamps()
        skipDefaultCheckout()
        disableConcurrentBuilds()
    }

    parameters {
        choice(
            name: 'DEPLOY_SCOPE',
            choices: ['DEV_ONLY', 'THROUGH_TEST', 'THROUGH_PROD'],
            description: 'How far should the image be deployed?'
        )

        booleanParam(
            name: 'DEPLOY_ENABLED',
            defaultValue: true,
            description: 'Enable Docker deployments'
        )

        string(
            name: 'RELEASE_NOTE',
            defaultValue: 'Session 8 deployment',
            description: 'Non-secret release description'
        )
    }

    environment {
        IMAGE_NAME = 'neelima-cicd'
        DEV_PORT = '8081'
        TEST_PORT = '8082'
        PROD_PORT = '8083'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Show Configuration') {
            steps {
                echo "Deployment scope: ${params.DEPLOY_SCOPE}"
                echo "Deployment enabled: ${params.DEPLOY_ENABLED}"
                echo "Release note: ${params.RELEASE_NOTE}"

                sh '''
                    echo "Image name: $IMAGE_NAME"
                    echo "Dev port: $DEV_PORT"
                    echo "Test port: $TEST_PORT"
                    echo "Production port: $PROD_PORT"
                '''
            }
        }

        stage('Verify Credential') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'neelima-lab-token',
                        variable: 'LAB_API_TOKEN'
                    )
                ]) {
                    sh '''
                        set +x
                        set -eu

                        test -n "$LAB_API_TOKEN"
                        echo "Credential binding successful"
                    '''
                }
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

                    echo "Docker image built"
                '''
            }
        }

        stage('Deploy Dev') {
            when {
                expression {
                    return params.DEPLOY_ENABLED
                }
            }

            steps {
                sh '''
                    set -eu

                    docker rm -f neelima-cicd-dev \
                      2>/dev/null || true

                    docker run -d \
                      --name neelima-cicd-dev \
                      -p 127.0.0.1:8081:8080 \
                      -e SPRING_PROFILES_ACTIVE=dev \
                      "$IMAGE_NAME:$BUILD_NUMBER"
                '''
            }
        }

        stage('Verify Dev') {
            when {
                expression {
                    return params.DEPLOY_ENABLED
                }
            }

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

        stage('Deploy Test') {
            when {
                expression {
                    return params.DEPLOY_ENABLED &&
                      params.DEPLOY_SCOPE in [
                        'THROUGH_TEST',
                        'THROUGH_PROD'
                      ]
                }
            }

            steps {
                sh '''
                    set -eu

                    docker rm -f neelima-cicd-test \
                      2>/dev/null || true

                    docker run -d \
                      --name neelima-cicd-test \
                      -p 127.0.0.1:8082:8080 \
                      -e SPRING_PROFILES_ACTIVE=test \
                      "$IMAGE_NAME:$BUILD_NUMBER"
                '''
            }
        }

        stage('Verify Test') {
            when {
                expression {
                    return params.DEPLOY_ENABLED &&
                      params.DEPLOY_SCOPE in [
                        'THROUGH_TEST',
                        'THROUGH_PROD'
                      ]
                }
            }

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
            when {
                expression {
                    return params.DEPLOY_ENABLED &&
                      params.DEPLOY_SCOPE == 'THROUGH_PROD'
                }
            }

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

        stage('Deploy Production') {
            when {
                expression {
                    return params.DEPLOY_ENABLED &&
                      params.DEPLOY_SCOPE == 'THROUGH_PROD'
                }
            }

            steps {
                sh '''
                    set -eu

                    docker rm -f neelima-cicd-prod \
                      2>/dev/null || true

                    docker run -d \
                      --name neelima-cicd-prod \
                      -p 127.0.0.1:8083:8080 \
                      -e SPRING_PROFILES_ACTIVE=prod \
                      "$IMAGE_NAME:$BUILD_NUMBER"
                '''
            }
        }

        stage('Verify Production') {
            when {
                expression {
                    return params.DEPLOY_ENABLED &&
                      params.DEPLOY_SCOPE == 'THROUGH_PROD'
                }
            }

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

            echo 'Pipeline completed'
        }

        success {
            echo 'PIPELINE SUCCESSFUL'
        }

        failure {
            echo 'PIPELINE FAILED - CHECK LOGS'
        }

        aborted {
            echo 'PIPELINE ABORTED'
        }
    }
}
