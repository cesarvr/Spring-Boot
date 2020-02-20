def POD_LABEL  = 'jnlp'
def CONFIG_MAP = 'my-configmap'
def CONFIG_MAP_MOUNT = '/cfg'

podTemplate(cloud: 'openshift', 
	label: BUILD_TAG, 
	volumes: [configMapVolume(configMapName: CONFIG_MAP, mountPath: CONFIG_MAP_MOUNT)], 
	containers: [containerTemplate(name: POD_LABEL, 
		image: 'registry.redhat.io/openshift3/jenkins-agent-maven-35-rhel7:v3.11', 
		args: '${computer.jnlpmac} ${computer.name}')]) {
    node (BUILD_TAG) {
        container(POD_LABEL) {
			pipeline {
			  agent {
			      label POD_LABEL
			  }
			  stages {
			    stage('Run maven') {
			      steps {
			        sh 'mvn -version'
			      }
			    }
			  }
}
        }
    }
}