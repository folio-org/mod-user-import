buildMvn {
  publishModDescriptor = 'yes'
  doApiDoc = true
  doApiLint = true
  apiTypes = 'RAML'
  apiDirectories = 'ramls'
  mvnDeploy = 'yes'
  doKubeDeploy = true
  buildNode = 'jenkins-agent-java11'

  doDocker = {
    buildJavaDocker {
      publishMaster = 'yes'
      healthChk = 'yes'
      healthChkCmd = 'curl -sS --fail -o /dev/null  http://localhost:8081/apidocs/ || exit 1'
    }
  }
}
