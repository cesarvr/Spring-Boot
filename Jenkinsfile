def appName = "${params.APPLICATION_NAME}"
def PROXY   = "${params.PROXY}"
def imageBuildConfig = appName
def deploymentConfig = appName

def PROXY_JVM_OPTIONS = "-DproxySet=true -DproxyHost=${PROXY} -DproxyPort=8080"

pipeline {

  agent {
    label 'maven'
  }

  stages {
    stage("Creating Openshift Components") {
      
      steps {
        echo "Creating Openshift Objects"
        sh "echo creating objects for ${appName} && chmod +x ./jenkins/build.sh && ./jenkins/build.sh ${appName}"
      }
  }


    stage("Running Test\'s") {
      steps {
        echo "Run unit tests"
        sh "mvn ${PROXY_JVM_OPTIONS} surefire-report:report"
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
        }
      }
    }

    stage('Creating and Deploying Container') {
      steps {

        echo "Trigger image build"
      script {
            sh "mvn ${PROXY_JVM_OPTIONS} package"
            sh "ls target/*.jar"
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
          sh "oc wait dc/${appName} --for condition=available"
         
         
        }
      }
    }

  }
}
