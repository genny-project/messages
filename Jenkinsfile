pipeline {
	agent any
  tools {
      maven 'mvn 3.5'
  }
	stages {
    stage ('Clone') {
	    steps {
	    	checkout scm
	    }
    }
		stage('Build') {
			steps {
				sh 'mvn clean package'
			}
		}
	stage('Build Docker') {
		steps {
			sh "./build-docker.sh ${env.BRANCH_NAME}-latest"
			//sh "./push.sh ${BUILD_NUMBER}"
		}
		}
	}
}
