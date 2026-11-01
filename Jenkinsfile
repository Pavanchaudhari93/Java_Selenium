// =============================================================================
// Selenium TestNG Framework: Allure run-to-run comparison (Jenkins declarative pipeline)
// -----------------------------------------------------------------------------
// Runs the TestNG suite, then compares this build's Allure results with the
// previous build's and fails the build when the Passed -> Failed count is above
// REGRESSION_THRESHOLD.
//
// Required Jenkins plugins: Copy Artifact, HTML Publisher, Pipeline Utility Steps (optional)
// The job must allow Copy Artifact from itself:
//   Job config -> "Permission to Copy Artifact" -> project name = this job
//   (the options{} block below does the same thing).
//
// To add only the comparison to an existing pipeline, copy the
// 'Compare With Previous Run' stage and the 'archiveArtifacts' line in post{}.
// =============================================================================

def runCmd(String cmd) {
    return isUnix() ? sh(script: cmd, returnStatus: true) : bat(script: cmd, returnStatus: true)
}

pipeline {
    agent any

    parameters {
        string(name: 'SUITE_FILE', defaultValue: 'testng.xml', description: 'TestNG suite XML')
        choice(name: 'ENVIRONMENT', choices: ['qa', 'prod', 'staging', 'dev'],
               description: 'Environment to run against (config-<env>.properties)')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'edge', 'chromium'],
               description: 'Browser to use for UI tests')
        booleanParam(name: 'HEADLESS', defaultValue: true, description: 'Run in headless mode')
        string(name: 'APP_URL', defaultValue: '',
               description: 'Optional. Overrides baseUrl from environment config; leave blank to use config value')
        string(name: 'REGRESSION_THRESHOLD', defaultValue: '0',
               description: 'Fail the build when Passed -> Failed exceeds this count. Use -1 to report without gating.')
        booleanParam(name: 'INCLUDE_BROKEN', defaultValue: false,
                     description: 'Also count Passed -> Broken against the threshold')
        string(name: 'GRID_URL', defaultValue: '',
               description: 'Optional. Selenium Grid URL for remote execution')
    }

    options {
        copyArtifactPermission("${env.JOB_NAME}")
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timeout(time: 60, unit: 'MINUTES')
    }

    environment {
        // Matches <allure.results.directory> in pom.xml surefire config
        ALLURE_RESULTS = 'target/allure-results'
        PREVIOUS_DIR   = 'previous-run'
        COMPARISON_OUT = 'target/allure-comparison'
    }

    stages {
        stage('Setup') {
            steps {
                script {
                    // Display build description
                    currentBuild.description = "${params.ENVIRONMENT} | ${params.BROWSER} | ${params.SUITE_FILE}"
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    // Test failures must not stop the pipeline before the comparison runs
                    String cmd = "mvn -B -ntp clean test -DsuiteXmlFile=${params.SUITE_FILE}" +
                                 " -Denv=${params.ENVIRONMENT}" +
                                 " -Dbrowser=${params.BROWSER}" +
                                 " -Dheadless=${params.HEADLESS}"
                    if (params.APP_URL?.trim()) {
                        cmd += " -Dapp.baseUrl=${params.APP_URL.trim()}"
                    }
                    if (params.GRID_URL?.trim()) {
                        cmd += " -Dexecution.mode=grid -Dgrid.url=${params.GRID_URL.trim()}"
                    }

                    int status = runCmd(cmd)
                    if (status != 0) {
                        unstable("Test execution exited with ${status}")
                    }
                }
            }
        }

        stage('Compare With Previous Run') {
            steps {
                script {
                    // lastCompleted() excludes the running build, so this is the previous run
                    copyArtifacts(
                        projectName: env.JOB_NAME,
                        selector: lastCompleted(),
                        filter: "${env.ALLURE_RESULTS}/*-result.json",
                        target: env.PREVIOUS_DIR,
                        optional: true,
                        fingerprintArtifacts: false)

                    String previousResults = "${env.PREVIOUS_DIR}/${env.ALLURE_RESULTS}"
                    if (!fileExists(previousResults)) {
                        echo 'No previous Allure results found (first build or artifacts expired): skipping comparison.'
                        return
                    }

                    int status = runCmd(
                        "mvn -B -ntp test-compile exec:exec@allure-compare" +
                        " -Dallure.compare.previous=${previousResults}" +
                        " -Dallure.compare.current=${env.ALLURE_RESULTS}" +
                        " -Dallure.compare.output=${env.COMPARISON_OUT}" +
                        " -Dallure.compare.threshold=${params.REGRESSION_THRESHOLD}" +
                        " -Dallure.compare.includeBroken=${params.INCLUDE_BROKEN}")

                    publishHTML(target: [
                        reportName : 'Allure Run Comparison',
                        reportDir  : env.COMPARISON_OUT,
                        reportFiles: 'comparison-report.html',
                        keepAll    : true,
                        alwaysLinkToLastBuild: true,
                        allowMissing: true])

                    // Exit codes from AllureComparisonRunner: 1 = threshold exceeded, 2 = error
                    if (status == 1) {
                        error("Regression threshold exceeded: more than ${params.REGRESSION_THRESHOLD} tests went Passed -> Failed. See 'Allure Run Comparison'.")
                    } else if (status != 0) {
                        unstable("Allure comparison could not run (exit ${status}); see console log.")
                    }
                }
            }
        }

        stage('Allure Report') {
            steps {
                script {
                    // Generate Allure report for this run
                    int status = runCmd("mvn -B -ntp allure:report")
                    if (status != 0) {
                        unstable("Allure report generation failed (exit ${status})")
                    }
                }
            }
        }
    }

    post {
        always {
            // The results archived here become the baseline for the next build
            archiveArtifacts(artifacts: "${env.ALLURE_RESULTS}/*-result.json, ${env.COMPARISON_OUT}/**",
                             allowEmptyArchive: true)

            // Archive Allure report
            archiveArtifacts(artifacts: 'target/site/allure-maven-plugin/**',
                             allowEmptyArchive: true)
        }
        success {
            echo 'Build completed successfully.'
        }
        unstable {
            echo 'Build unstable - tests failed or comparison threshold exceeded.'
        }
        failure {
            echo 'Build failed.'
        }
    }
}