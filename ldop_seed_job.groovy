// file: ldop-seed-job.groovy
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
  def validateJobName = ldopImageName + '-0-validate'
  def singleImageJobName = ldopImageName + '-1-build'

  job('ldop/' + validateJobName) {
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
        extensions {
          gitTagMessageExtension()
        }
      }
    }
    triggers {
      githubPush()
    }
    steps{
      shell(
"""\
#!/bin/bash
if [ -z \${GIT_TAG_NAME} ]; then
  echo \"ERROR: No git tag for version, exiting failure\" && exit 1
else
  echo \"Git tag: \${GIT_TAG_NAME}\"
fi
if [[ \"\${GIT_TAG_NAME}\" =~ ^[0-9]+\\.[0-9]+\\.[0-9]+\$ ]]; then
  echo \"version format accepted\"
else
  echo \"ERROR: version format incorrect; exiting failure\" && exit 1
fi
TOPIC=\"\${GIT_BRANCH#*/}\"
"""
      )
    }
    publishers {
      downstreamParameterized {
        trigger("ldop/$singleImageJobName") { 
          condition('SUCCESS')
          parameters {
            predefinedProp('IMAGE_VERSION', '\${GIT_TAG_NAME}')
            predefinedProp('IMAGE_NAME', ldopImageName)
            predefinedProp('TOPIC', '\${GIT_BRANCH}')
          }
        }
      }
      slackNotifier {
        notifyFailure(true)
        notifySuccess(false)
        notifyAborted(false)
        notifyNotBuilt(false)
        notifyUnstable(false)
        notifyBackToNormal(true)
        notifyRepeatedFailure(false)
        startNotification(false)
        includeTestSummary(false)
        includeCustomMessage(false)
        customMessage(null)
        sendAs(null)
        commitInfoChoice('AUTHORS_AND_TITLES')
        teamDomain(null)
        authToken(null)
        room('ldop')
      }
    }
  }

  job('ldop/' + singleImageJobName) {
    description('This job was created with automation. Manual edits to this job are discouraged.')
    parameters {
      textParam('IMAGE_VERSION')
      textParam('IMAGE_NAME')
      textParam('TOPIC')
    }
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
      }
    }
    steps {
      shell(
"""\
#!/bin/bash
docker build -t liatrio/\${IMAGE_NAME}:\${TOPIC} .
docker push liatrio/\${IMAGE_NAME}:\${TOPIC}
"""
      )
    }
    publishers {
      downstreamParameterized {
        trigger('ldop/ldop-integration-testing') {
          condition('SUCCESS')
          parameters {
            currentBuild()
          }
        }
      }
      slackNotifier {
        notifyFailure(true)
        notifySuccess(false)
        notifyAborted(false)
        notifyNotBuilt(false)
        notifyUnstable(false)
        notifyBackToNormal(true)
        notifyRepeatedFailure(true)
        startNotification(false)
        includeTestSummary(false)
        includeCustomMessage(false)
        customMessage(null)
        sendAs(null)
        commitInfoChoice('AUTHORS_AND_TITLES')
        teamDomain(null)
        authToken(null)
        room('ldop')
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
      notifySuccess(false)
      notifyAborted(false)
      notifyNotBuilt(false)
      notifyUnstable(false)
      notifyBackToNormal(true)
      notifyRepeatedFailure(true)
      startNotification(false)
      includeTestSummary(false)
      includeCustomMessage(false)
      customMessage(null)
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
      notifySuccess(false)
      notifyAborted(false)
      notifyNotBuilt(false)
      notifyUnstable(false)
      notifyBackToNormal(true)
      notifyRepeatedFailure(true)
      startNotification(false)
      includeTestSummary(false)
      includeCustomMessage(false)
      customMessage(null)
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
  publishers {
    slackNotifier {
      notifyFailure(true)
      notifySuccess(false)
      notifyAborted(false)
      notifyNotBuilt(false)
      notifyUnstable(false)
      notifyBackToNormal(true)
      notifyRepeatedFailure(true)
      startNotification(false)
      includeTestSummary(false)
      includeCustomMessage(false)
      customMessage(null)
      sendAs(null)
      commitInfoChoice('AUTHORS_AND_TITLES')
      teamDomain(null)
      authToken(null)
      room('ldop')
    }
  }
}
