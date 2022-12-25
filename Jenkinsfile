pipeline {
  agent any
  tools {
    jdk 'openjdk17'
    maven 'maven-3.6.3'
  }
  stages {
    stage ('Build macappbundler-maven-plugin') {
      steps {
        sh 'mvn clean -Dmaven.test.failure.ignore=true install'
      }
    }
  }
}
