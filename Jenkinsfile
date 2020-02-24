pipeline {
// Initially run on any agent
   agent {
      label 'codesigning'
   }
   environment {
//Configure Maven from the maven tooling in Jenkins
      def mvnHome = tool 'Default'
      PATH = "${mvnHome}/bin:${env.PATH}"
      
//Set some defaults
      def workspace  = pwd()
   }
   stages {
// Set up the workspace, clear the git directories and setup the manve settings.xml files
      stage('prep-workspace') { 
         steps {
            configFileProvider([configFile(fileId: '86dde059-684b-4300-b595-64e83c2dd217', targetLocation: 'settings.xml')]) {
            }
            dir('repository/dev.galasa') {
               deleteDir()
            }
            dir('repository/dev/galasa') {
               deleteDir()
            }
         }
      }
      
      stage('SimPlatform Application') {
         steps {
            withCredentials([string(credentialsId: 'galasa-gpg', variable: 'GPG')]) {
               withSonarQubeEnv('GalasaSonarQube') {
                  dir('galasa-simplatform-application') {
                     sh "mvn --settings ${workspace}/settings.xml -Dmaven.repo.local=${workspace}/repository -Dgpg.skip=${GPG_SKIP} -Dgpg.passphrase=$GPG  -P ${MAVEN_PROFILE} -B -e -fae --non-recursive ${MAVEN_GOAL}"
                  }
               }
            }
         }
      }
      stage('SimBank-Tests') {
         steps {
            withCredentials([string(credentialsId: 'galasa-gpg', variable: 'GPG')]) {
               withSonarQubeEnv('GalasaSonarQube') {
                  dir('galasa-simbank-tests') {
                     sh "mvn --settings ${workspace}/settings.xml -Dmaven.repo.local=${workspace}/repository -Dgpg.skip=${GPG_SKIP} -Dgpg.passphrase=$GPG  -P ${MAVEN_PROFILE} -B -e -fae --non-recursive ${MAVEN_GOAL}"

                     dir('dev.galasa.simbank.manager') {
                        sh "mvn --settings ${workspace}/settings.xml -Dmaven.repo.local=${workspace}/repository -Dgpg.skip=${GPG_SKIP} -Dgpg.passphrase=$GPG  -P ${MAVEN_PROFILE} -B -e -fae --non-recursive ${MAVEN_GOAL}"
                     }

                     dir('dev.galasa.simbank.tests') {
                        sh "mvn --settings ${workspace}/settings.xml -Dmaven.repo.local=${workspace}/repository -Dgpg.skip=${GPG_SKIP} -Dgpg.passphrase=$GPG  -P ${MAVEN_PROFILE} -B -e -fae --non-recursive ${MAVEN_GOAL}"
                     }

                     dir('dev.galasa.simbank.obr') {
                        sh "mvn --settings ${workspace}/settings.xml -Dmaven.repo.local=${workspace}/repository -Dgpg.skip=${GPG_SKIP} -Dgpg.passphrase=$GPG  -P ${MAVEN_PROFILE} -B -e -fae --non-recursive ${MAVEN_GOAL}"
                     }
                  }
               }
            }
         }
      }
      stage('SimBank Eclipse Comms Maven') {
         steps {
            withCredentials([string(credentialsId: 'galasa-gpg', variable: 'GPG')]) {
               withSonarQubeEnv('GalasaSonarQube') {
                  dir('galasa-simbank-eclipse') {
                     sh "mvn --settings ${workspace}/settings.xml -Dmaven.repo.local=${workspace}/repository -Dgpg.skip=${GPG_SKIP} -Dgpg.passphrase=$GPG  -P ${MAVEN_PROFILE} -B -e -fae --non-recursive ${MAVEN_GOAL}"

                     dir('dev.galasa.simbank.ui') {
                        sh "mvn --settings ${workspace}/settings.xml -Dmaven.repo.local=${workspace}/repository -Djarsigner.skip=${env.SIGN_SKIP} -Dgpg.skip=${GPG_SKIP} -Dgpg.passphrase=$GPG  -P ${MAVEN_PROFILE} -B -e -fae --non-recursive ${MAVEN_GOAL}"
                     }

                     dir('dev.galasa.simbank.feature') {
                        sh "mvn --settings ${workspace}/settings.xml -Dmaven.repo.local=${workspace}/repository -Djarsigner.skip=${env.SIGN_SKIP} -Dgpg.skip=${GPG_SKIP} -Dgpg.passphrase=$GPG  -P ${MAVEN_PROFILE} -B -e -fae --non-recursive ${MAVEN_GOAL}"
                     }
                  }
               }
            }
         }
      }
   }
   post {
       // triggered when red sign
       failure {
           slackSend (channel: '#galasa-devs', color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
       }
    }
}
