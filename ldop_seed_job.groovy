// file: ldop-seed-job.groovy
// authored: Justin Bankes on 6.19.17
// Last Modified:
folder('ldop')

// Create LDOP image jobs
def ldopImages = ['ldop-gerrit', 
                  'ldop-jenkins', 
                  'ldop-jenkins-slave',
                  'ldop-ldap',
                  'ldop-ldap-ltb',
                  'ldop-ldap-phpadmin',
                  'ldop-logstash',
                  'ldop-nexus',
                  'ldop-nginx',
                  'ldop-sensu',
                  'ldop-sonar']

ldopImages.each {
  ldopImageName->

  def repoURL = "https://github.com/liatrio/" + ldopImageName + ".git"
  def singleImageJobName = ldopImageName + '-1-build'

  job('ldop/' + singleImageJobName){
    description('This job was created with automation. Manual edits to this job are discouraged.')
    wrappers {
      colorizeOutput()
    }
    properties {
      githubProjectUrl(repoURL)
    }
    scm {
      git {
        remote {
          url(repoURL)
        }
        extensions {
          gitTagMessageExtension()
        }
      }
    }
    triggers {
      githubPush()
    }
    steps {
      shell("docker build -t jbankes/${ldopImageName}:\${GIT_TAG_NAME} -t jbankes/${ldopImageName}:latest .")
    }
    publishers {
      downstreamParameterized {
        trigger('ldop/ldop-integration-testing') { 
          condition('SUCCESS')
          parameters {
            predefinedProp('IMAGE_VERSION', '\${GIT_TAG_NAME}')
            predefinedProp('IMAGE_NAME', ldopImageName)
          }
        }
      }
    }
  }
}

// Create LDOP Docker Compose Job
job('ldop/ldop-docker-compose') {
  def repoURL = 'https://github.com/liatrio/ldop-docker-compose'
  description('This job was created with automation. Manual edits to this job are discouraged.')
  wrappers{
    colorizeOutput()
  }
  properties {
    githubProjectUrl(repoURL)
  }
  scm {
    git {
      remote {
        url(repoURL)
      }
    }
  }
  triggers {
    githubPush()
  } 
  steps {
    shell('echo "TODO: Test the platform"') 
  }
}

// Create LDOP Integration Testing Job
job('ldop/ldop-integration-testing') {
  def repoURL = 'https://github.com/liatrio/ldop-docker-compose'
  description('This job was created with automation. Manual edits to this job are discouraged.')
  parameters {
    textParam('IMAGE_VERSION')
    textParam('IMAGE_NAME')
  }
  properties {
    githubProjectUrl(repoURL)
  }
  scm {
    git {
      remote {
        url(repoURL)
      }
    }
  }
  steps {
    shell('echo "This is where the tests would go"')
    shell('echo \$IMAGE_VERSION')
    shell('echo \$IMAGE_NAME')
  }
  publishers {
    downstreamParameterized {
      trigger('ldop/ldop-image-deploy') { 
        condition('SUCCESS')
        parameters {
          currentBuild()
        }
      }
    }
  }
}

// Create LDOP Image Deployment Jobs
job('ldop/ldop-image-deploy') {
  description('This job was created with automation. Manual edits to this job are discouraged.')
  parameters {
    textParam('IMAGE_VERSION')
    textParam('IMAGE_NAME')
  }
  steps {
    dockerBuildAndPublish {
      repositoryName('jbankes/\$IMAGE_NAME')
      tag('\$IMAGE_VERSION') 
      registryCredentials('docker_temp_password')
      skipBuild(true)
    }
    dockerBuildAndPublish {
      repositoryName('jbankes/\$IMAGE_NAME')
      tag('\$IMAGE_VERSION') 
      registryCredentials('docker_temp_password')
      skipBuild(true)
    }
    // shell('docker push jbankes/\$IMAGE_NAME:\$IMAGE_VERSION')
    // shell('docker push jbankes/\$IMAGE_NAME:latest')
  }
}
