node('master') {
    def workSpaceHome = pwd()
    stage('Clean') {
        deleteDir()
    }
    stage('Checkout') {
        checkout scm
    }
    stage('Build') {    
	
		echo "Current Working Directory : " + pwd()
		sh "mvn clean package"
		
    }
    stage('Copy') {
		sh "mv target/QTMJenkinsPlugin.hpi QTMJenkinsPlugin.hpi"
    }
	stage('Clean') {
    	deleteDir()
    }
}