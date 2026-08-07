// =============================================================================
// Root Jenkinsfile — product-service only (self-contained, no load)
// Prefer Script Path: backend/product-service/Jenkinsfile
// =============================================================================
// Identical body to backend/product-service/Jenkinsfile so root Script Path=Jenkinsfile
// also avoids hudson.FilePath / load failures.
// =============================================================================

pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds(abortPrevious: false)
    buildDiscarder(logRotator(numToKeepStr: '30'))
    timeout(time: 60, unit: 'MINUTES')
  }

  parameters {
    string(name: 'ECR_REPO', defaultValue: 'obs/product-service', description: 'ECR repo name (no registry host)')
    string(name: 'EC2_HOST', defaultValue: '13.207.135.101', description: 'obs-catalog-instance EIP (product); blank → credential obs-ec2-host')
    string(name: 'CONTAINER_PORT', defaultValue: '8082', description: 'product-service port')
    string(name: 'HEALTH_ENDPOINT', defaultValue: '/actuator/health', description: 'Health path on EC2 loopback')
    string(name: 'EC2_USER', defaultValue: 'ubuntu', description: 'SSH user (Ubuntu AMI)')
    string(name: 'AWS_REGION', defaultValue: 'ap-south-1', description: 'ECR region')
    string(name: 'EXTRA_ENV', defaultValue: '', description: 'Optional KEY=V pairs (no secrets)')
    string(name: 'REMOTE_ENV_FILE', defaultValue: '', description: 'Optional env-file path on EC2')
    booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip unit tests')
    booleanParam(name: 'PERFORM_ROLLBACK_ONLY', defaultValue: false, description: 'Only rollback on EC2')
    booleanParam(name: 'REQUIRE_2XX_HEALTH', defaultValue: true, description: 'Health requires 2xx/3xx (/actuator/health returns 200 when UP)')
  }

  environment {
    AWS_CREDENTIALS_ID  = 'obs-aws-deploy'
    AWS_ACCOUNT_CRED_ID = 'obs-aws-account-id'
    EC2_SSH_CREDENTIALS = 'obs-ec2-ssh'
    EC2_HOST_CRED_ID    = 'obs-ec2-host'
    SERVICE_NAME        = 'product-service'
    SERVICE_DIR         = 'backend/product-service'
    IMAGE_TAG_BUILD     = "${env.BUILD_NUMBER}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          def fullSha = env.GIT_COMMIT ?: sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
          env.GIT_COMMIT_FULL  = fullSha
          env.GIT_COMMIT_SHORT = fullSha.take(7)
          echo "product-service @ ${env.GIT_COMMIT_SHORT}"
        }
      }
    }

    stage('Validate') {
      when { expression { return !params.PERFORM_ROLLBACK_ONLY } }
      steps {
        script {
          if (!fileExists("${env.SERVICE_DIR}/pom.xml")) { error "Missing ${env.SERVICE_DIR}/pom.xml" }
          if (!fileExists("${env.SERVICE_DIR}/Dockerfile")) { error "Missing ${env.SERVICE_DIR}/Dockerfile" }
          if (!(params.CONTAINER_PORT ==~ /^[0-9]+$/)) { error 'CONTAINER_PORT must be numeric' }
        }
        sh '''
          set -e
          command -v mvn >/dev/null || { echo "Maven required"; exit 1; }
          command -v docker >/dev/null || { echo "Docker required"; exit 1; }
          command -v aws >/dev/null || { echo "AWS CLI required"; exit 1; }
          java -version; mvn -version; docker version; aws --version
        '''
      }
    }

    stage('Unit Tests') {
      when {
        allOf {
          expression { return !params.PERFORM_ROLLBACK_ONLY }
          expression { return !params.SKIP_TESTS }
        }
      }
      steps { dir(env.SERVICE_DIR) { sh 'mvn -B test' } }
      post {
        always {
          junit allowEmptyResults: true, testResults: "${SERVICE_DIR}/target/surefire-reports/*.xml"
        }
      }
    }

    stage('Package') {
      when { expression { return !params.PERFORM_ROLLBACK_ONLY } }
      steps { dir(env.SERVICE_DIR) { sh 'mvn -B -DskipTests package' } }
    }

    stage('Build Docker Image') {
      when { expression { return !params.PERFORM_ROLLBACK_ONLY } }
      steps {
        script {
          withCredentials([string(credentialsId: env.AWS_ACCOUNT_CRED_ID, variable: 'AWS_ACCOUNT_ID')]) {
            env.RESOLVED_REGION = params.AWS_REGION
            env.ECR_REGISTRY    = "${AWS_ACCOUNT_ID}.dkr.ecr.${env.RESOLVED_REGION}.amazonaws.com"
            env.IMAGE_URI       = "${env.ECR_REGISTRY}/${params.ECR_REPO}"
            env.IMAGE_LATEST    = "${env.IMAGE_URI}:latest"
            env.IMAGE_BUILD     = "${env.IMAGE_URI}:${env.IMAGE_TAG_BUILD}"
            env.IMAGE_SHA       = "${env.IMAGE_URI}:${env.GIT_COMMIT_SHORT}"
          }
          sh """
            set -e
            docker build -t "${env.IMAGE_LATEST}" -t "${env.IMAGE_BUILD}" -t "${env.IMAGE_SHA}" "${env.SERVICE_DIR}"
          """
        }
      }
    }

    stage('Scan image') {
      when { expression { return !params.PERFORM_ROLLBACK_ONLY } }
      steps {
        script {
          if (sh(script: 'command -v trivy >/dev/null 2>&1', returnStatus: true) == 0) {
            sh "trivy image --exit-code 1 --severity CRITICAL,HIGH --ignore-unfixed \"${env.IMAGE_BUILD}\""
          } else {
            echo 'TODO: Trivy not installed — skipping image scan'
          }
        }
      }
    }

    stage('Login to ECR') {
      when { expression { return !params.PERFORM_ROLLBACK_ONLY } }
      steps {
        withCredentials([usernamePassword(
          credentialsId: env.AWS_CREDENTIALS_ID,
          usernameVariable: 'AWS_ACCESS_KEY_ID',
          passwordVariable: 'AWS_SECRET_ACCESS_KEY'
        )]) {
          sh """
            set -e
            aws ecr get-login-password --region "${env.RESOLVED_REGION}" \
              | docker login --username AWS --password-stdin "${env.ECR_REGISTRY}"
          """
        }
      }
    }

    stage('Push image') {
      when { expression { return !params.PERFORM_ROLLBACK_ONLY } }
      steps {
        sh """
          set -e
          set -euo pipefail
          docker push "${env.IMAGE_BUILD}"
          docker push "${env.IMAGE_SHA}"
          if ! docker push "${env.IMAGE_LATEST}"; then
            echo "WARN: could not push ${env.IMAGE_LATEST} (immutable tag?). Deploy uses ${env.IMAGE_BUILD}"
          fi
        """
      }
    }

    stage('Deploy to EC2 via SSH') {
      steps {
        script {
          def host = params.EC2_HOST?.trim()
          if (!host) {
            withCredentials([string(credentialsId: env.EC2_HOST_CRED_ID, variable: 'HOST_FROM_CRED')]) {
              host = env.HOST_FROM_CRED
            }
          }
          if (!host?.trim()) { error 'EC2_HOST empty and credential obs-ec2-host missing' }
          env.RESOLVED_EC2_HOST = host.trim()
          def deployImage = env.IMAGE_BUILD ?: ''
          def require2xx = params.REQUIRE_2XX_HEALTH ? 'true' : 'false'
          def remoteEnvFile = params.REMOTE_ENV_FILE?.trim() ?: ''
          def extraEnv = params.EXTRA_ENV?.trim() ?: ''

          lock(resource: "obs-deploy-${env.SERVICE_NAME}") {
            sshagent(credentials: [env.EC2_SSH_CREDENTIALS]) {
              sh """
                set -euo pipefail
                SSH_OPTS="-o StrictHostKeyChecking=accept-new -o ServerAliveInterval=30"
                REMOTE="${params.EC2_USER}@${env.RESOLVED_EC2_HOST}"
                scp \$SSH_OPTS scripts/deploy.sh scripts/rollback.sh "\$REMOTE:/tmp/"
                ssh \$SSH_OPTS "\$REMOTE" 'chmod +x /tmp/deploy.sh /tmp/rollback.sh && sudo mkdir -p /opt/obs/deploy-state && sudo chown \$(id -un):\$(id -gn) /opt/obs/deploy-state || mkdir -p /opt/obs/deploy-state'
              """
              if (params.PERFORM_ROLLBACK_ONLY) {
                sh """
                  set -euo pipefail
                  SSH_OPTS="-o StrictHostKeyChecking=accept-new"
                  REMOTE="${params.EC2_USER}@${env.RESOLVED_EC2_HOST}"
                  EXTRA_ARGS=""
                  if [ -n "${remoteEnvFile}" ]; then EXTRA_ARGS="--env-file ${remoteEnvFile}"; fi
                  if [ -n "${extraEnv}" ]; then EXTRA_ARGS="\$EXTRA_ARGS --extra-env '${extraEnv}'"; fi
                  ssh \$SSH_OPTS "\$REMOTE" "AWS_REGION='${params.AWS_REGION}' /tmp/rollback.sh --service '${env.SERVICE_NAME}' --port '${params.CONTAINER_PORT}' --health '${params.HEALTH_ENDPOINT}' --require-2xx '${require2xx}' \$EXTRA_ARGS"
                """
              } else {
                withCredentials([usernamePassword(credentialsId: env.AWS_CREDENTIALS_ID, usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                  sh """
                    set -euo pipefail
                    SSH_OPTS="-o StrictHostKeyChecking=accept-new -o ServerAliveInterval=30"
                    REMOTE="${params.EC2_USER}@${env.RESOLVED_EC2_HOST}"
                    aws ecr get-login-password --region "${env.RESOLVED_REGION}" \
                      | ssh \$SSH_OPTS "\$REMOTE" "docker login --username AWS --password-stdin ${env.ECR_REGISTRY}"
                    EXTRA_ARGS=""
                    if [ -n "${remoteEnvFile}" ]; then EXTRA_ARGS="--env-file ${remoteEnvFile}"; fi
                    if [ -n "${extraEnv}" ]; then EXTRA_ARGS="\$EXTRA_ARGS --extra-env '${extraEnv}'"; fi
                    ssh \$SSH_OPTS "\$REMOTE" "AWS_REGION='${params.AWS_REGION}' /tmp/deploy.sh --service '${env.SERVICE_NAME}' --image '${deployImage}' --port '${params.CONTAINER_PORT}' --health '${params.HEALTH_ENDPOINT}' --require-2xx '${require2xx}' \$EXTRA_ARGS"
                  """
                }
              }
            }
          }
        }
      }
    }

    stage('Health check endpoint verification') {
      when { expression { return !params.PERFORM_ROLLBACK_ONLY } }
      steps {
        sshagent(credentials: [env.EC2_SSH_CREDENTIALS]) {
          sh """
            set -euo pipefail
            SSH_OPTS="-o StrictHostKeyChecking=accept-new"
            REMOTE="${params.EC2_USER}@${env.RESOLVED_EC2_HOST}"
            ssh \$SSH_OPTS "\$REMOTE" '
              code=\$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "http://127.0.0.1:${params.CONTAINER_PORT}${params.HEALTH_ENDPOINT}" || true)
              echo "Remote health HTTP \$code"
              if [ "${params.REQUIRE_2XX_HEALTH}" = "true" ]; then
                case "\$code" in 2??|3??) exit 0 ;; *) exit 1 ;; esac
              else
                case "\$code" in [1234]??) exit 0 ;; *) exit 1 ;; esac
              fi
            '
          """
        }
      }
    }
  }

  post {
    success { echo "SUCCESS: product-service on ${env.RESOLVED_EC2_HOST}" }
    failure { echo 'FAILURE: check deploy/rollback logs on EC2 if deploy ran.' }
    always { sh 'docker logout "${ECR_REGISTRY}" >/dev/null 2>&1 || true' }
  }
}
