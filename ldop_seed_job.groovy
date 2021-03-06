/////////////////////////////////////
// ldop-* Multibranch Pipeline Jobs
/////////////////////////////////////

org = 'liatrio'
project = 'ldop'
repos = [
    'ldop-gerrit',
    'ldop-jenkins',
    'ldop-jenkins-slave',
    'ldop-ldap',
    'ldop-ldap-ltb',
    'ldop-logstash',
    'ldop-nexus',
    'ldop-nginx',
    'ldop-sensu',
    'ldop-sonar'
]

folder(project)

repos.each { repo ->
  	multibranchPipelineJob("${project}/${repo}") {
        branchSources {
            git {
                remote("https://github.com/${org}/${repo}")
                includes('*')
            }
        }
        orphanedItemStrategy {
            discardOldItems {
                daysToKeep(7)
            }
        }
        triggers {
            periodic(2)
        }
    }
}


///////////////////////////
// ldop-docker-compose Job
///////////////////////////

job("${project}/ldop-docker-compose") {
    def repoURL = 'https://github.com/liatrio/ldop-docker-compose'
    description('This job was created with automation. Manual edits to this job are discouraged.')
    logRotator(-1, 15, -1, 3)
    parameters {
        textParam('sha1')
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
                github('liatrio/ldop-docker-compose')
                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
            }
            branch('${sha1}')
        }
    }
    triggers {
        githubPullRequest {
            admins(['stein321', 'liatriojoe', 'RobertKelly'])
            orgWhitelist('liatrio')
            useGitHubHooks()
        }
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

///////////////////////////
// image cleanup job
///////////////////////////
job("${project}/cleanup-images") {
  triggers {
    cron('00 10 * * *')
  }
  steps {
    shell('docker rmi -f $(docker images -aq) || echo "some images weren\'t deleted automatically"')
  }
}
