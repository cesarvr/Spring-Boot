def appName = "${params.APPLICATION_NAME}"
def PROXY   = "${params.PROXY}"

// If you are behind a proxy uncomment use:
// def PROXY_JVM_OPTIONS ="-DproxySet=true -DproxyHost=${PROXY} -DproxyPort=8080"
def PROXY_JVM_OPTIONS = "" 

pipeline {

  agent {
    label 'maven'
  }

  stages {
    stage("Creating Openshift Components") {
      steps {
        sh "echo creating objects for ${appName} && chmod +x ./jenkins/build.sh && ./jenkins/build.sh ${appName}"
      }
    }

    stage("Test and Packaging") {
      steps {
        echo "Run unit tests"
        sh "mvn ${PROXY_JVM_OPTIONS} package"
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
        }
      }
    }

    stage('Creating and Deploying Container') {
      steps {
        script {
            sh "oc start-build bc/${appName} --from-file=\$(ls target/*.jar) --follow"
        }
      }

      post {
        success {
          archiveArtifacts artifacts: 'target/**.jar', fingerprint: true
        }
      }
    }

    stage('Deploy') {
      steps {
        script {
          sh "oc rollout latest dc/${appName} || true"
          sh "oc wait dc/${appName} --for condition=available --timeout=-1s"
        }
      }
    }
  }
}
