// file: ldop-seed-job.groovy
folder('ldop')

// Create LDOP image jobs
def ldopImages = ['ldop-gerrit',
                  'ldop-jenkins',
                  'ldop-jenkins-slave',
                  'ldop-ldap',
                  'ldop-ldap-ltb',
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
        logRotator(-1, 15, -1, 3)
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

echo -e "\\nrunning hadolint..."
docker run --rm -i lukasmartinelli/hadolint < Dockerfile
if [ \$? -ne 0 ]; then
    echo -e "Hadolint found errors, continuing.\\n"
else
    echo -e "Hadolint passed without errors\\n"
fi

echo -e "\\nrunning dockerlint..."
docker run -i --rm -v "\$PWD/Dockerfile":/Dockerfile:ro \\
redcoolbeans/dockerlint
if [ \$? -ne 0 ]; then
    echo -e "Dockerlint found errors, continuing.\\n"
else
    echo -e "Dockerlint passed without errors\\n"
fi

echo -e "\\nrunning dockerfile_lint..."
docker run -i --rm -v `pwd`:/root/ projectatomic/dockerfile-lint \\
    dockerfile_lint -f Dockerfile
if [ \$? -ne  0]; then
    echo -e "Dockerlint found errors, continuing.\\n"
else
    echo -e "Dockerlint passed without errors\\n"
fi
"""
            )
        }
        publishers {
            downstream("ldop/$singleImageJobName", 'SUCCESS')
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
        logRotator(-1, 15, -1, 3)
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
        steps {
            shell(
"""\
#!/bin/bash
TOPIC=\"\${GIT_BRANCH#*/}\"
docker build -t liatrio/$ldopImageName:\${TOPIC} .
docker push liatrio/$ldopImageName:\${TOPIC}
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
    logRotator(-1, 15, -1, 3)
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
echo \"Running Validation on extensions\"
./test/validation/validation.sh
if [ \$? -ne 0 ]; then
    echo \"Extensions has issues; exiting failure\"
    exit 1
else
    echo \"Validation successful\"
fi
TOPIC=\"\${GIT_BRANCH#*/}\"
export TF_VAR_branch_name="\${TOPIC}"
echo \"Running integration tests\"
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
    logRotator(-1, 15, -1, 3)
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
    logRotator(-1, 15, -1, 3)
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

// Cleanup images jobs
job('cleanup images') {
  triggers {
        cron('00 10 * * *')
    }
    steps {
        shell('docker rmi $(docker images -q)')
    }
}
