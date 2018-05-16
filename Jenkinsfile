/* 
  assumes build and deployment configurations have been created beforehand as below:

  oc new-build --binary=true --name=spring-demo-bake registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift:1.3 --to=spring-demo
  oc new-app --image-stream spring-demo --name=spring-demo-dev
  oc expose dc spring-demo-dev
  oc set env dc spring-demo-dev JAVA_OPTIONS="-Dmanagement.endpoints.jmx.exposure.include=health,info"
  oc set probe dc spring-demo-dev --readiness --get-url=http://:8080/health
  oc set probe dc spring-demo-dev --liveness --get-url=http://:8080/health
*/

def appName = "${params.PROJECT_NAME}"
def imageBuildConfig = appName
def deploymentConfig = appName

pipeline {
  agent {
    label 'maven'
  }
  stages {
    stage('Run unit tests') {
      steps {
        echo "Run unit tests"
        sh 'mvn test'
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
        }
      }
    }

    stage('Build') {
      steps {
        echo "Build artifact"
        sh 'mvn package'
        echo "Trigger image build"
        script {
          openshift.withCluster() {
            openshift.selector("bc", imageBuildConfig).startBuild("--from-file=target/ROOT.war", "--wait")
            }
          }
        }
      post {
        success {
          archiveArtifacts artifacts: 'target/**.war', fingerprint: true
        }
      }
    }

    stage('Deploy') {
      steps {
        script {
          openshift.withCluster() {
            openshift.selector("dc", deploymentConfig).rollout()
          }
        }
      }
    }
  }
}
