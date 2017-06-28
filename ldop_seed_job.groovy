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
  // def projectURL = "https://github.com/liatrio/" + ldopImageName 
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
      shell("docker build -t liatrio/${ldopImageName}:\${GIT_TAG_NAME} .")
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

// Create LDOP Integration Testing Job
job('ldop/ldop-integration-testing') {
  parameters {
    textParam('IMAGE_VERSION')
    textParam('IMAGE_NAME')
  }
  steps {
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
  parameters {
    textParam('IMAGE_VERSION')
    textParam('IMAGE_NAME')
  }
  steps {
    shell('echo \$IMAGE_VERSION')
    shell('echo \$IMAGE_NAME')
  }
}
