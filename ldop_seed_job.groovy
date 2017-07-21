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
      shell(
"""\
#!/bin/bash

TOPIC=\"\${GIT_BRANCH#*/}\"

docker build -t liatrio/${ldopImageName}:\${TOPIC} .
docker push liatrio/${ldopImageName}:\${TOPIC}
"""
      )
    }
    publishers {
      downstreamParameterized {
        trigger('ldop/ldop-integration-testing') { 
          condition('SUCCESS')
          parameters {
            predefinedProp('IMAGE_VERSION', '\${GIT_TAG_NAME}')
            predefinedProp('IMAGE_NAME', ldopImageName)
            predefinedProp('TOPIC', '\${GIT_BRANCH}')
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
    shell(
"""\
TOPIC=\"\${GIT_BRANCH#*/}\"
export TF_VAR_branch_name="\${TOPIC}"
./test/integration/run-integration-test.sh
"""
    ) 
  }
  publishers {
    slackNotifier {
      notifyFailure(true)
      notifySuccess(true)
      notifyAborted(false)
      notifyNotBuilt(false)
      notifyUnstable(false)
      notifyBackToNormal(true)
      notifyRepeatedFailure(false)
      startNotification(true)
      includeTestSummary(true)
      includeCustomMessage(false)
      customMessage(null)
      buildServerUrl(null)
      sendAs(null)
      commitInfoChoice('AUTHORS_AND_TITLES')
      teamDomain(null)
      authToken(null)
      room('ldop')
    }
  }
}

// Create LDOP Integration Testing Job
job('ldop/ldop-integration-testing') {
  def repoURL = 'https://github.com/liatrio/ldop-docker-compose'
  description('This job was created with automation. Manual edits to this job are discouraged.')
  parameters {
    textParam('IMAGE_VERSION')
    textParam('IMAGE_NAME')
    textParam('TOPIC')
  }
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
      branch('master')
    }
  }
  steps {
    shell(
"""\
TOPIC="\${TOPIC#*/}"
git checkout \${TOPIC}
sed -i "/liatrio\\/\${IMAGE_NAME}/c\\    image: liatrio/\${IMAGE_NAME}:\${TOPIC}" docker-compose.yml
export TF_VAR_branch_name="\${TOPIC}"
./test/integration/run-integration-test.sh
"""
    )
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
    slackNotifier {
      notifyFailure(true)
      notifySuccess(true)
      notifyAborted(false)
      notifyNotBuilt(false)
      notifyUnstable(false)
      notifyBackToNormal(true)
      notifyRepeatedFailure(false)
      startNotification(true)
      includeTestSummary(true)
      includeCustomMessage(false)
      customMessage(null)
      buildServerUrl(null)
      sendAs(null)
      commitInfoChoice('AUTHORS_AND_TITLES')
      teamDomain(null)
      authToken(null)
      room('ldop')
    }
  }
}

// Create LDOP Image Deployment Jobs
job('ldop/ldop-image-deploy') {
  description('This job was created with automation. Manual edits to this job are discouraged.')
  parameters {
    textParam('IMAGE_VERSION')
    textParam('IMAGE_NAME')
    textParam('TOPIC')
  }
  wrappers{
    colorizeOutput()
  }
  steps {
    shell(
"""\
TOPIC="\${TOPIC#*/}"
docker tag liatrio/\${IMAGE_NAME}:\${TOPIC} liatrio/\${IMAGE_NAME}:\${IMAGE_VERSION}
docker push liatrio/\${IMAGE_NAME}:\${IMAGE_VERSION}
"""
    )
  }
}
