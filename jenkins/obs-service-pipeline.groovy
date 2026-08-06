// =============================================================================
// Shared OBS service pipeline (loaded by each backend/*/Jenkinsfile)
// -----------------------------------------------------------------------------
// Not a standalone Jenkins job script path. Each service Jenkinsfile does:
//   def runner = load 'jenkins/obs-service-pipeline.groovy'
//   runner.run([ serviceName: '...', ecrRepo: '...', ... ])
//
// required plugins / credentials: see any backend/*/Jenkinsfile header.
// =============================================================================

/**
 * @param cfg Map keys:
 *   serviceName        backend/<name> directory (required)
 *   ecrRepo            ECR repository name without registry host (required)
 *   containerPort      published port string (required)
 *   healthEndpoint     HTTP path for health checks (required)
 *   require2xxHealth   boolean — false for user-service auth-style probes
 *   remoteEnvFile      default docker --env-file path on EC2 (optional)
 *   awsRegion          default region (optional, default ap-south-1)
 *   ec2User            default SSH user (optional, default ubuntu for Ubuntu AMIs)
 *   ec2Host            default EC2 private IP / hostname for this service (optional)
 */
def run(Map cfg) {
  // Fail fast if a service Jenkinsfile forgot required keys.
  ['serviceName', 'ecrRepo', 'containerPort', 'healthEndpoint'].each { k ->
    if (!cfg[k]) {
      error "obs-service-pipeline: missing required config key '${k}'"
    }
  }

  def serviceName      = cfg.serviceName as String
  def ecrRepo          = cfg.ecrRepo as String
  def containerPort    = cfg.containerPort as String
  def healthEndpoint   = cfg.healthEndpoint as String
  def require2xxDef    = (cfg.require2xxHealth != null) ? (cfg.require2xxHealth as Boolean) : true
  def remoteEnvDefault = (cfg.remoteEnvFile ?: '') as String
  def awsRegionDef     = (cfg.awsRegion ?: 'ap-south-1') as String
  def ec2UserDef       = (cfg.ec2User ?: 'ubuntu') as String
  def ec2HostDef       = (cfg.ec2Host ?: '') as String

  pipeline {
    agent any

    options {
      timestamps()
      // Per-service job: another service can still build in parallel (separate job).
      // Same service stays serial so workspace / BUILD_NUMBER / EC2 deploy do not race.
      disableConcurrentBuilds(abortPrevious: false)
      buildDiscarder(logRotator(numToKeepStr: '30'))
      timeout(time: 60, unit: 'MINUTES')
    }

    parameters {
      // SERVICE_NAME is fixed by the service Jenkinsfile — operators do not choose another app here.
      string(
        name: 'ECR_REPO',
        defaultValue: ecrRepo,
        description: "ECR repository name for ${serviceName} (without registry host)"
      )
      string(
        name: 'EC2_HOST',
        defaultValue: ec2HostDef,
        description: 'EC2 private IP/host for this service; blank falls back to credential obs-ec2-host'
      )
      string(
        name: 'CONTAINER_PORT',
        defaultValue: containerPort,
        description: "Container publish port for ${serviceName}"
      )
      string(
        name: 'HEALTH_ENDPOINT',
        defaultValue: healthEndpoint,
        description: "Health path for ${serviceName} on 127.0.0.1:CONTAINER_PORT"
      )
      string(
        name: 'EC2_USER',
        defaultValue: ec2UserDef,
        description: 'SSH user (Amazon Linux: ec2-user, Ubuntu: ubuntu)'
      )
      string(
        name: 'AWS_REGION',
        defaultValue: awsRegionDef,
        description: 'AWS region for ECR'
      )
      string(
        name: 'EXTRA_ENV',
        defaultValue: '',
        description: 'Optional comma-separated docker -e pairs (no secrets)'
      )
      string(
        name: 'REMOTE_ENV_FILE',
        defaultValue: remoteEnvDefault,
        description: 'Optional path already on EC2 for docker --env-file'
      )
      booleanParam(
        name: 'SKIP_TESTS',
        defaultValue: false,
        description: 'Emergency only — skip Unit Tests'
      )
      booleanParam(
        name: 'PERFORM_ROLLBACK_ONLY',
        defaultValue: false,
        description: 'Skip build/push; run scripts/rollback.sh on EC2 only'
      )
      booleanParam(
        name: 'REQUIRE_2XX_HEALTH',
        defaultValue: require2xxDef,
        description: 'Health requires HTTP 2xx/3xx (false accepts 4xx — useful for user-service)'
      )
    }

    environment {
      AWS_CREDENTIALS_ID  = 'obs-aws-deploy'
      AWS_ACCOUNT_CRED_ID = 'obs-aws-account-id'
      EC2_SSH_CREDENTIALS = 'obs-ec2-ssh'
      EC2_HOST_CRED_ID    = 'obs-ec2-host'

      // Fixed for this service job (not a build parameter).
      SERVICE_NAME        = "${serviceName}"
      SERVICE_DIR         = "backend/${serviceName}"
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
            echo "Checked out ${env.GIT_COMMIT_SHORT} for fixed service ${env.SERVICE_NAME}"
          }
        }
      }

      stage('Validate') {
        when {
          expression { return !params.PERFORM_ROLLBACK_ONLY }
        }
        steps {
          script {
            if (!fileExists("${env.SERVICE_DIR}/pom.xml")) {
              error "Missing ${env.SERVICE_DIR}/pom.xml"
            }
            if (!fileExists("${env.SERVICE_DIR}/Dockerfile")) {
              error "Missing ${env.SERVICE_DIR}/Dockerfile"
            }
            if (!(params.CONTAINER_PORT ==~ /^[0-9]+$/)) {
              error 'CONTAINER_PORT must be numeric'
            }
          }
          sh '''
            set -e
            command -v mvn >/dev/null || { echo "Maven (mvn) is required on the agent"; exit 1; }
            command -v docker >/dev/null || { echo "Docker is required on the agent"; exit 1; }
            command -v aws >/dev/null || { echo "AWS CLI is required on the agent"; exit 1; }
            java -version
            mvn -version
            docker version
            aws --version
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
        steps {
          dir(env.SERVICE_DIR) {
            sh 'mvn -B test'
          }
        }
        post {
          always {
            junit allowEmptyResults: true, testResults: "${env.SERVICE_DIR}/target/surefire-reports/*.xml"
          }
        }
      }

      stage('Package') {
        when {
          expression { return !params.PERFORM_ROLLBACK_ONLY }
        }
        steps {
          dir(env.SERVICE_DIR) {
            sh 'mvn -B -DskipTests package'
          }
        }
      }

      stage('Build Docker Image') {
        when {
          expression { return !params.PERFORM_ROLLBACK_ONLY }
        }
        steps {
          script {
            withCredentials([
              string(credentialsId: env.AWS_ACCOUNT_CRED_ID, variable: 'AWS_ACCOUNT_ID')
            ]) {
              env.RESOLVED_REGION = params.AWS_REGION
              env.ECR_REGISTRY    = "${AWS_ACCOUNT_ID}.dkr.ecr.${env.RESOLVED_REGION}.amazonaws.com"
              env.IMAGE_URI       = "${env.ECR_REGISTRY}/${params.ECR_REPO}"
              env.IMAGE_LATEST    = "${env.IMAGE_URI}:latest"
              env.IMAGE_BUILD     = "${env.IMAGE_URI}:${env.IMAGE_TAG_BUILD}"
              env.IMAGE_SHA       = "${env.IMAGE_URI}:${env.GIT_COMMIT_SHORT}"
            }

            sh """
              set -e
              docker build \
                -t "${env.IMAGE_LATEST}" \
                -t "${env.IMAGE_BUILD}" \
                -t "${env.IMAGE_SHA}" \
                "${env.SERVICE_DIR}"
            """
          }
        }
      }

      stage('Scan image') {
        when {
          expression { return !params.PERFORM_ROLLBACK_ONLY }
        }
        steps {
          script {
            def hasTrivy = sh(script: 'command -v trivy >/dev/null 2>&1', returnStatus: true) == 0
            if (hasTrivy) {
              sh """
                set -e
                trivy image --exit-code 1 --severity CRITICAL,HIGH --ignore-unfixed "${env.IMAGE_BUILD}"
              """
            } else {
              // TODO: install Trivy on the agent and fail the build on CRITICAL/HIGH findings.
              echo 'TODO: Trivy not found on agent — skipping image vulnerability scan'
            }
          }
        }
      }

      stage('Login to ECR') {
        when {
          expression { return !params.PERFORM_ROLLBACK_ONLY }
        }
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
        when {
          expression { return !params.PERFORM_ROLLBACK_ONLY }
        }
        steps {
          sh """
            set -e
            docker push "${env.IMAGE_LATEST}"
            docker push "${env.IMAGE_BUILD}"
            docker push "${env.IMAGE_SHA}"
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
            if (!host?.trim()) {
              error 'EC2_HOST is empty and credential obs-ec2-host is missing/empty'
            }
            env.RESOLVED_EC2_HOST = host.trim()

            def deployImage   = env.IMAGE_BUILD ?: ''
            def require2xx    = params.REQUIRE_2XX_HEALTH ? 'true' : 'false'
            def remoteEnvFile = params.REMOTE_ENV_FILE?.trim() ?: ''
            def extraEnv      = params.EXTRA_ENV?.trim() ?: ''

            lock(resource: "obs-deploy-${env.SERVICE_NAME}", inversePrecedence: false) {
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
                    ssh \$SSH_OPTS "\$REMOTE" "AWS_REGION='${params.AWS_REGION}' /tmp/rollback.sh \\
                      --service '${env.SERVICE_NAME}' \\
                      --port '${params.CONTAINER_PORT}' \\
                      --health '${params.HEALTH_ENDPOINT}' \\
                      --require-2xx '${require2xx}' \\
                      \$EXTRA_ARGS"
                  """
                } else {
                  withCredentials([usernamePassword(
                    credentialsId: env.AWS_CREDENTIALS_ID,
                    usernameVariable: 'AWS_ACCESS_KEY_ID',
                    passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                  )]) {
                    sh """
                      set -euo pipefail
                      SSH_OPTS="-o StrictHostKeyChecking=accept-new -o ServerAliveInterval=30"
                      REMOTE="${params.EC2_USER}@${env.RESOLVED_EC2_HOST}"

                      aws ecr get-login-password --region "${env.RESOLVED_REGION}" \
                        | ssh \$SSH_OPTS "\$REMOTE" "docker login --username AWS --password-stdin ${env.ECR_REGISTRY}"

                      EXTRA_ARGS=""
                      if [ -n "${remoteEnvFile}" ]; then EXTRA_ARGS="--env-file ${remoteEnvFile}"; fi
                      if [ -n "${extraEnv}" ]; then EXTRA_ARGS="\$EXTRA_ARGS --extra-env '${extraEnv}'"; fi

                      ssh \$SSH_OPTS "\$REMOTE" "AWS_REGION='${params.AWS_REGION}' /tmp/deploy.sh \\
                        --service '${env.SERVICE_NAME}' \\
                        --image '${deployImage}' \\
                        --port '${params.CONTAINER_PORT}' \\
                        --health '${params.HEALTH_ENDPOINT}' \\
                        --require-2xx '${require2xx}' \\
                        \$EXTRA_ARGS"
                    """
                  }
                }
              }
            }
          }
        }
      }

      stage('Health check endpoint verification') {
        when {
          expression { return !params.PERFORM_ROLLBACK_ONLY }
        }
        steps {
          script {
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
    }

    post {
      success {
        echo "SUCCESS: ${env.SERVICE_NAME} tags latest / ${env.BUILD_NUMBER} / ${env.GIT_COMMIT_SHORT} → ${env.RESOLVED_EC2_HOST}"
      }
      failure {
        echo "FAILURE: ${env.SERVICE_NAME}. deploy.sh attempts automatic rollback if a previous image exists."
      }
      always {
        sh 'docker logout "${ECR_REGISTRY}" >/dev/null 2>&1 || true'
      }
    }
  }
}

return this
