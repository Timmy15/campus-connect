pipeline {
  agent any

  options {
    skipDefaultCheckout(true)
  }

  tools {
    maven 'Maven_3'
  }

  parameters {
      booleanParam(
        name: 'RUN_UI_TESTS',
        defaultValue: false,
        description: 'Run Selenium UI tests'
      )
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build, Test & Coverage (JaCoCo)') {
      steps {
        // Runs tests AND produces JaCoCo XML + HTML under target/site/jacoco
        bat 'mvn -U clean org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report package'
      }
    }

    stage('Publish Coverage Report') {
      steps {
        publishHTML(target: [
          reportDir: 'target/site/jacoco',
          reportFiles: 'index.html',
          reportName: 'JaCoCo Coverage',
          keepAll: true,
          alwaysLinkToLastBuild: true,
          allowMissing: true
        ])
      }
    }

    stage('SonarQube Analysis') {
  steps {
    withSonarQubeEnv('sonarqube-local') {
      bat '''
        mvn -X -DskipTests sonar:sonar ^
          -Dsonar.projectKey=call-failure-analyser ^
          -Dsonar.projectName="Call Failure Analyser" ^
          -Dsonar.host.url=%SONAR_HOST_URL% ^
          -Dsonar.token=%SONAR_AUTH_TOKEN% ^
          -Dsonar.coverage.jacoco.xmlReportPaths=target\\site\\jacoco\\jacoco.xml
      '''
    }
  }
}

  stage('UI Tests (Selenium)') {
        when {
          expression { params.RUN_UI_TESTS }
        }
        steps {
          bat 'mvn -B verify -DskipUnitTests=true'
        }
  }
}
  post {
    always {
      junit '**/target/surefire-reports/*.xml'
      archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
      archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
    }
  }
}
