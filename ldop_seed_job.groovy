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
      // downstreamParameterized {
      //   trigger('ldop/integration-testing') {
      //     block {
      //       buildStepFailure()
      //       failure()
      //       unstable()
      //     } 
      //   }
      // }
    }
  }
}

// Create LDOP Integration Testing Job
// job('ldop/integration-testing') {
//   parameters {
    
//   }
//   steps {
//     shell('echo')
//   }
// }

// Create LDOP Image Deployment Jobs
