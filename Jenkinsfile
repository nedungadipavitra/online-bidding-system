// =============================================================================
// Root Jenkinsfile (defaults to product-service)
// Prefer: backend/<service>/Jenkinsfile as Script Path
// =============================================================================

def cfg = [
  serviceName      : 'product-service',
  ecrRepo          : 'obs/product-service',
  containerPort    : '8082',
  healthEndpoint   : '/products',
  require2xxHealth : true,
  remoteEnvFile    : '',
  awsRegion        : 'ap-south-1',
  ec2User          : 'ec2-user',
]

def runner
node {
  checkout scm
  runner = load 'jenkins/obs-service-pipeline.groovy'
}
runner.run(cfg)
