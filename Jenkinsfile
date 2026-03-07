pipeline {
    agent any
    
    tools {
        maven 'Maven_3'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Show Branch') {
            steps {
                bat 'echo Building branch: %BRANCH_NAME%'
            }
        }

        stage('Stage 1: Build & Unit Test') {
            when {
                anyOf {
                    branch 'dev'
                    branch 'main'
                }
            }
            steps {
                bat 'mvn clean test -Dparallel=none'
            }
        }

        stage('Stage 2: Integration Tests') {
            when {
                anyOf {
                    branch 'dev'
                    branch 'main'
                }
            }
            steps {
                bat 'mvn verify -P integration -Dparallel=none'
            }
        }

        stage('Stage 3: Selenium E2E Tests') {
            when {
                anyOf {
                    branch 'dev'
                    branch 'main'
                }
            }
            steps {
                bat 'mvn verify -P e2e -Dparallel=none -Dcucumber.plugin=json:target/Cucumber.json,pretty,summary'
            }
        }

        stage('SonarQube Analysis') {
            when {
                anyOf {
                    branch 'dev'
                    branch 'master'
                }
            }
            steps {
                withSonarQubeEnv('sonarqube-local') {
                    bat '''
                        mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar ^
                        -Dsonar.projectKey=campus-connect ^
                        -Dsonar.projectName=campus-connect
                    '''
                }
            }
        }

        stage('Quality Gate') {
            when {
                anyOf {
                    branch 'dev'
                    branch 'main'
                }
            }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                bat 'echo Deploy stage for main branch goes here'
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml'

            recordCoverage(
                tools: [[parser: 'JACOCO', pattern: '**/jacoco.xml']]
            )

            cucumber buildStatus: 'UNSTABLE',
                     fileIncludePattern: '**/Cucumber.json',
                     jsonReportDirectory: 'target'

            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target/site/jacoco',
                reportFiles: 'index.html',
                reportName: 'Detailed JaCoCo Report'
            ])

            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target/karate-reports',
                reportFiles: 'karate-summary.html',
                reportName: 'Karate API Report'
            ])
        }

        success {
            bat 'echo Pipeline completed successfully.'
        }

        unstable {
            bat 'echo Pipeline is unstable. Check test or cucumber results.'
        }

        failure {
            bat 'echo Pipeline failed. Check build logs.'
        }
    }
}
