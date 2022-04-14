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
      // healthChk is tested in UserImportIT.java
    }
  }
}
