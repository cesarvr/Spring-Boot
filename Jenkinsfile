/* 
  assumes build and deployment configurations have been created beforehand as below:

  oc new-build --binary=true --name=spring-demo-bake registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift:1.3 --to=spring-demo
  oc new-app --image-stream spring-demo --name=spring-demo-dev
  oc expose dc spring-demo-dev
  oc set env dc spring-demo-dev JAVA_OPTIONS="-Dmanagement.endpoints.jmx.exposure.include=health,info"
  oc set probe dc spring-demo-dev --readiness --get-url=http://:8080/health
  oc set probe dc spring-demo-dev --liveness --get-url=http://:8080/health
*/

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
    stage("Creating Project ${appName}") {
      
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
          sh "oc rollout latest dc/${appName}"
          sh "oc wait dc/${appName} --for condition=available"
         
         
        }
      }
    }

  }
}
