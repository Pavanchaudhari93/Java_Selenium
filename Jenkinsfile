pipeline {
    agent { label 'Windows' }
    environment {
        // GitLab Configurations
        GIT_CREDENTIALS     = 'gitlabnew'
        GIT_REPO_URL        = 'https://repo.arconnet.com/DevOps/epm_automation.git'
        
        // Configure Mail Sender & Recipients
        MAIL_TO             = 'pavan.chaudhari@arconnet.com,shrutika.rachha@arconnet.com,akshata.vaidya@arconnet.com,pavan.chaudhari@arconnet.com,rohan.sul@arconnet.com'
        MAIL_REPLY          = 'devops@arconnet.com'
        VIEW_NAME           = 'automation_testing'
        
        // Core Automation Properties File Path [from GitLab_Automation_Project]
        PROPS_FILE          = 'Env_configs/Automation.Properties'

        // Configure Suite Directories 
        SUITES_DIRS         = 'CICD_Suites,UI_Suite,Suite_API'

        // Configure Default Fallback Props
        DEFAULT_BRANCH      = 'Dev'
        DEFAULT_ENV         = 'QA_MSSQL'
        DEFAULT_BROWSER     = 'edge'
        DEFAULT_SUITE       = 'CICD_Suites/Demo.xml'
        DEFAULT_AGENT       = 'Windows'

        // Pipeline Execution Agents List
        AGENT_LIST          = 'Windows'
    
        // Stores all Scheduled_Triggers
        TRIGGER_HISTORY = "${env.JENKINS_HOME}/${env.JOB_NAME}_trigger_history.json"

        // ═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
        // Jira Defect Tracking Configurations
        // —  Raises / Updates Jira Ticket(s) for Failed & Broken Tests [Parsed from Allure Results] & lists them in the Execution Mail
        // —  Linked with existing Variables: RESOLVED_BRANCH (Version), RESOLVED_ENV / SUITE / BROWSER / AGENT, BUILD_URL, MAIL_TO
        // ═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
        JIRA_ENABLED            = 'true'                                            // Master Switch :: 'true' / 'false'
        JIRA_DRY_RUN            = 'false'                                           // 'true' = Simulate Only [No Live Jira_API Calls]
        JIRA_URL                = 'https://arcon-tech-solution.atlassian.net'       // Jira Cloud / Server Base_URL
        JIRA_PROJECT_KEY        = 'COEX'                                            // Jira Project Key
        JIRA_ISSUE_TYPE         = 'Bug'                                             // Jira Issue Type [Bug / Defect]
        JIRA_CREDS_ID           = 'jira_cloud_creds'                                // Jenkins Credential_ID [Username = E-mail, Password = API_Token]
        JIRA_MAIL_GREETING      = 'Hi Team,'                                        // Greeting Line of the QA Report Section in the Mail
        JIRA_RESULTS_DIR        = 'Execution_Reports/Allure_Report/allure-results' // Current Run's Allure '*-result.json' Dir [matches pom.xml 'allure.results.directory'; excludes Allure_Previous]
        JIRA_OUTPUT_DIR         = 'Jira_Reports'                  // Jira_Engine Output [test_summary.json / jira_summary.json]
        JIRA_PACKAGE_VERSION    = 'AUTO'                                            // 'AUTO' = Derive from RESOLVED_BRANCH [10.12.002_Automation → 10.12.002]
        JIRA_SUMMARY_FORMAT     = 'CI {version} {page} failed'                      // Ticket Title Placeholders :: {version} {page} {env} {suite} {test}
        JIRA_APP_URL            = 'https://epmqamyui.arconnet.com:859/'                      // Application URL [used in Ticket Description]
        JIRA_APP_LOGIN          = 'arcosadmin / pass@123'                           // Application Login [used in Ticket Description]
        JIRA_TARGET_SERVER_IP   = '10.10.2.154'                                     // Application Server IP [used in Ticket Description]
        JIRA_COMMON_LABELS      = 'EPM_Automation'                                  // Labels added to every Ticket [comma separated]
        JIRA_SHOWSTOPPER_KEYS   = 'EPM UI/Agent/Functional Scenario'                            // Component Keywords → 'Show stopper' Priority [else 'High']

         }

    stages {

        // ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
        // STAGE 0  
        // —  Refresh_Choices: Fetch Branches, Parse Props from PROPS_FILE, Fetch & List Suite [.xml] Files
        // —  Scheduled_Triggers: Create Triggers for Scheduled_Execution of the Pipeline
        // ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
        stage('Refresh Choices & Scheduled_Triggers') {
            
            agent { label "${env.DEFAULT_AGENT}" } // Pre-configured Default Agent [only for STAGE-0] for Initial Fetch & Choice Resolution 
            steps {
                script {
                    // 0a. Resolve Choices for Current_Run 
                    env.RESOLVED_BRANCH  = params.branch      ? params.branch.trim()      : env.DEFAULT_BRANCH
                    env.RESOLVED_ENV     = params.env         ? params.env.trim()         : env.DEFAULT_ENV
                    env.RESOLVED_BROWSER = params.browserType ? params.browserType.trim() : env.DEFAULT_BROWSER
                    env.RESOLVED_SUITE   = params.suiteFile   ? params.suiteFile.trim()   : env.DEFAULT_SUITE
                    env.RESOLVED_AGENT   = params.agentLabel  ? params.agentLabel.trim()  : env.DEFAULT_AGENT
                    // null before the Parameter is registered [First_Run] → default 'true'
                    env.RESOLVED_HEADLESS = params.HEADLESS == false ? 'false' : 'true'

                    echo """
                    [Jenkins: STAGE_O_A] Prepared Runtime_Parameters List ::
                    {
                        "Resolved Runtime Parameters":{               
                            "Branch" : {
                                "${env.RESOLVED_BRANCH}"  
                            },
                            "Env" : {
                                "${env.RESOLVED_ENV}"   
                            },
                            "Browser" : {
                                "${env.RESOLVED_BROWSER}"  
                            },
                            "Suite" : {
                                "${env.RESOLVED_SUITE}"  
                            },
                            "Agent" : {
                                "${env.RESOLVED_AGENT}"
                            },
                            "Headless" : {
                                "${env.RESOLVED_HEADLESS}"
                            }
                        }
                    }"""
                    
                    // First_Run / New Parameter added to this Jenkinsfile [not yet registered on the Job] → Force Choice_Refresh so 'Build with Parameters' shows it
                    // ⚠️ Keep this list in sync with the parameters([...]) block in STAGE_O_E-2
                    def expectedParams = ['branch', 'env', 'browserType', 'suiteFile', 'agentLabel', 'HEADLESS',
                                          'refreshChoices', 'scheduleBuild', 'scheduleTime', 'scheduleDay',
                                          'deleteTriggers', 'clearTriggerHistory']
                    def missingParams  = expectedParams.findAll { !params.containsKey(it) }
                    def firstRun       = !missingParams.isEmpty()
                    if (firstRun) echo "[Jenkins: STAGE_O_A] Parameter(s) not registered on the Job yet ${missingParams} — forcing Refresh_Choices . . ."

                    if (firstRun || params.refreshChoices || params.scheduleBuild || params.clearTriggerHistory || (params.deleteTriggers?.trim() ?: ',') != ','){
                         
                    // 0b. Fetch all Remote_Branch names from Git Repository 
                    def rawBranches = []
                    withCredentials([usernamePassword(
                            credentialsId: env.GIT_CREDENTIALS,
                            usernameVariable: 'GIT_U',
                            passwordVariable: 'GIT_P')]) {
                        
                        // Store Git_Password in a Temporary Secure File
                        writeFile file: '.askpass.bat', text: '@echo %GIT_P%\r\n'

                        def lsRemote = bat(
                            script: """
                                @echo on
                                set REPO=${env.GIT_REPO_URL}
                                set GIT_ASKPASS=%WORKSPACE%\\.askpass.bat
                                set GIT_USERNAME=%GIT_U%
                                git ls-remote --heads %REPO%
                            """,
                            returnStdout: true
                        ).trim()

                        // Clear Password [Erase Secrets]
                        bat 'del /f /q ".askpass.bat" 2>nul & exit /b 0'

                        // Resolve Remote Branch_Names
                        def lsLines = lsRemote.split('\n')
                        for (int i = 0; i < lsLines.size(); i++) {
                            def line = lsLines[i].trim()
                            if (line.contains('refs/heads/')) {
                                def parts = line.split('refs/heads/')
                                if (parts.size() > 1) {
                                    def branchName = parts[1].trim()
                                    // Exclude remote-tracking refs like origin/xxx
                                    if (!branchName.contains('/')) {
                                        rawBranches.add(branchName)
                                    }
                                }
                            }
                        }
                    }

                    rawBranches.sort()
                    echo "[Jenkins: STAGE_O_B]Remote_Branches List generated successfully!"
                    def branchChoices = buildChoiceList(rawBranches, env.DEFAULT_BRANCH)

                    // 0c. Build Exec_Agent Choices from AGENT_LIST 
                    def rawAgents  = []
                    def agentSplit = env.AGENT_LIST.split(',')
                    for (int a = 0; a < agentSplit.size(); a++) {
                        def ag = agentSplit[a].trim()
                        if (ag) rawAgents.add(ag)
                    }
                    def agentChoices = buildChoiceList(rawAgents, env.DEFAULT_AGENT)
                    echo "[Jenkins: STAGE_O_C] Successfully organized Pipeline-Exec_Agents List!"

                    // 0d. Checkout the Default Repo_Branch to Read PROPS_FILE & Parse Automation_Framework [for TestNG_Suite_Files]
                    def envChoices     = [env.DEFAULT_ENV]
                    def browserChoices = [env.DEFAULT_BROWSER]
                    def suiteChoices   = [env.DEFAULT_SUITE]

                    try {
                        // cleanWs deleteDirs: true, disableDeferredWipeout: true
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "${env.RESOLVED_BRANCH}"]],
                            userRemoteConfigs: [[
                                url: "${env.GIT_REPO_URL}",
                                credentialsId: "${env.GIT_CREDENTIALS}",
                                refspec: "+refs/heads/${env.RESOLVED_BRANCH}:refs/remotes/origin/${env.RESOLVED_BRANCH}"
                            ]],
                            extensions: [
                                [$class: 'CleanBeforeCheckout', deleteUntrackedNestedRepositories: true],
                                [$class: 'CloneOption',
                                    shallow: true,
                                    depth: 1,
                                    noTags: true,
                                    reference: '',
                                    timeout: 20
                                ]
                            ]
                        ])
                            
                        env.WORKSPACE_READY = 'true'

                        // 0d-1. Parse PROPS_FILE (Automation.Properties)
                        def propsText      = readFile(env.PROPS_FILE)
                        def parsedEnvs     = parseProperty('Environment', propsText)
                        def parsedBrowsers = parseProperty('BrowserType',  propsText)

                        if (parsedEnvs)     envChoices     = buildChoiceList(parsedEnvs,     env.DEFAULT_ENV)
                        if (parsedBrowsers) browserChoices = buildChoiceList(parsedBrowsers, env.DEFAULT_BROWSER)
                        
                        echo "[Jenkins: STAGE_O_D-1] Environment & Browser-Type parameters retrieval successful!"
                        
                        // 0d-2. Discover all TestNG_Suite_XMLs from Automation Project 
                                                   def discovered   = []
                            def suiteDirList = env.SUITES_DIRS.split(',')
                            for (int d = 0; d < suiteDirList.size(); d++) {
                                def dir        = suiteDirList[d].trim()
                                def found      = findFiles(glob: "${dir}/**/*.xml")
                                def dirEntries = []
                                for (int f = 0; f < found.size(); f++) {
                                    // Normalise Windows_backslashes → forward_slashes
                                    dirEntries.add(found[f].path.replace('\\', '/'))
                                }
                                dirEntries.sort()             // sort only within the respective directory
                                discovered.addAll(dirEntries) // append directory [following SUITES_DIRS Order]
                            }
                            // ⚠️ WARNING: Don't call 'discovered.sort()'' here, that would re-sort across all directories and break their grouping
                            if (discovered) suiteChoices = buildChoiceList(discovered, env.DEFAULT_SUITE)
                        
                            echo "[Jenkins: STAGE_O_D-2] TestNG_Suite_Files (.xml) retrieval successful!"
                        
                    } catch (err) {
                        echo "⚠️  Unable to Parse Automation_Project :: ${err.message}"
                    }

                    // 0e. Write Choices & Triggers into Job_Config (The Choices will be effective Next_Run onwards)
                    // 0.e-1 CRON Scripting & Configurations for adding Triggers
                    def triggerList = []
                    
                    if (params.clearTriggerHistory) {
                        // Overwrite & Wipe the Scheduled_Triggers History File.
                        // WARNING: Will remove any Active / Scheduled Triggers
                        if (fileExists(env.TRIGGER_HISTORY)) {
                            writeFile file: env.TRIGGER_HISTORY, text: ''
                            echo "[STAGE_O_E] TRIGGER_HISTORY cleared. All Scheduled_Triggers have been removed!"
                        } else {
                            echo "[STAGE_O_E] TRIGGER_HISTORY File not found . . ."
                        }                  
                    } else if ((params.deleteTriggers?.trim() ?: ',') != ',') {
    
                            // Strip '%'', 'whitespace', and 'lone' commas — "GUARD" against STRAY_CHARS submitted by 'Jenkins'
                            def toRemove = (params.deleteTriggers.trim()
                            .split('\\\\n|\\n|\n')
                            .collect { it.trim().replaceAll('%', '').replaceAll('^,+|,+$', '').trim() }
                            .findAll { it && it != ',' && it.length() > 5 }) as Set
    
                            echo "[STAGE_O_E] Removing ${toRemove.size()} Trigger(s) . . . "
                            toRemove.eachWithIndex { entry, idx ->
                                echo "  [${idx + 1}] ${entry}"
                            }
    
                            if (fileExists(env.TRIGGER_HISTORY)) {
                                def content = readFile(env.TRIGGER_HISTORY).trim()
                                
                                if (content) {
                                    def lines = content.split('\n')
                                    def kept = []
                                    int removedCount = 0
    
                                    for (int i = 0; i < lines.size(); i++) {
                                        def line = lines[i].trim()
                                        if (!line) continue
    
                                        def cronMatch      = line =~ /"cron":"([^"]+)"/
                                        def cronValue      = cronMatch ? cronMatch[0][1] : null
                                        def cronNormalized = cronValue ? cronValue.replaceAll('%', '') : null
    
                                        if (cronNormalized && toRemove.contains(cronNormalized)) {
                                            echo "[STAGE_O_E] ✅ Removing Trigger [${i + 1}]: ${cronValue}"
                                            removedCount++
                                        } else {
                                            kept.add(line)
                                        }
                                    }
    
                                    writeFile file: env.TRIGGER_HISTORY, text: kept.join('\n')
                                    echo "[STAGE_O_E] ${removedCount} Trigger(s) removed! ${kept.size()} Trigger(s) available in Trigger_History."
    
                                    // Re-register remaining Triggers to 'triggerList[]' 
                                    def cronCode = []
                                
                                    for (int i = 0; i < kept.size(); i++) {
                                        def line      = kept[i].trim()
                                        def cronMatch = line =~ /"cron":"([^"]+)"/
                                        if (cronMatch) {
                                            cronCode.add(cronMatch[0][1])
                                        }
                                    }
                                
                                    if (cronCode) {
                                        triggerList.add(parameterizedCron(cronCode.join('\n')))
                                        echo "[STAGE_O_E] ${cronCode.size()} remaining Trigger(s) re-registered successfully!"
                                    } else {
                                        echo "[STAGE_O_E] No Trigger(s) found . . ."
                                    }
                                } 
                            } else {
                                echo "[STAGE_O_E] TRIGGER_HISTORY File not found . . ."
                            }
                        } else {
                        if (params.scheduleBuild){
                            // Resolve Execution_Time
                            def rawTime  = params.scheduleTime?.trim() ?: '09:HH'
                            def splitRaw = rawTime.split(':')
                            
                            def hh = (splitRaw.size() > 0) ? splitRaw[0].replaceAll('[^0-9]', '').trim() : '09'
                            def mm = (splitRaw.size() > 1) ? splitRaw[1].trim().replaceAll('[^0-9A-Z]', '') : 'HH'

                            if (!hh) hh = '09'
                            if (!mm) mm = 'HH'

                            int hour = hh.toInteger()
                            def minute = (mm == 'HH' || mm == '') ? 'H' : "${mm.toInteger()}"
                            def scheduledTime = "${hh}:${mm}"
                            
                            // Resolve Execution_Day
                            def rawDay = params.scheduleDay?.trim()?.replaceAll(',+$', '') ?: 'ALL'
                            def scheduledDay = resolveExecutionDay(rawDay)

                            // Pass all currently RESOLVED Params into the 'cron' so the Scheduled_Execution inherits the exact same Branch / Env / Suite / Browser / Agent.
                            def cronExpr = "${minute} ${hour} * * ${scheduledDay} " +
                                "%branch=${env.RESOLVED_BRANCH}" +
                                ";env=${env.RESOLVED_ENV}" +
                                ";browserType=${env.RESOLVED_BROWSER}" +
                                ";suiteFile=${env.RESOLVED_SUITE}" +
                                ";agentLabel=${env.RESOLVED_AGENT}" +
                                ";scheduleBuild=false" +
                                ";scheduleTime=${scheduledTime}" +
                                ";scheduleDay=${rawDay}" +
                                ";refreshChoices=false"
                        
                            // Append new entry to trigger history file
                            def timestamp = new Date().format('dd-MMM-yyyy HH:mm:ss z')
                            def newEntry  = """{"cron":"${cronExpr}","addedAt":"${timestamp}"}"""
                                
                            try {
                                def existing = readFile(env.TRIGGER_HISTORY).trim()
                                writeFile file: env.TRIGGER_HISTORY, text: existing + '\n' + newEntry
                            } catch (ignored) {
                                // File doesn't exist yet — create it
                                writeFile file: env.TRIGGER_HISTORY, text: newEntry
                            }
                        }
                    
                        try{
                            if (fileExists(env.TRIGGER_HISTORY)) {
                                def content = readFile(env.TRIGGER_HISTORY).trim()
                            
                                if (content) {
                                    def cronCode = []
                                    def lines = content.split('\n')
                                
                                    for (int i = 0; i < lines.size(); i++) {
                                        def line      = lines[i].trim()
                                        def cronMatch = line =~ /"cron":"([^"]+)"/
                                        if (cronMatch) {
                                            cronCode.add(cronMatch[0][1])
                                        } else {
                                            echo "  [${i + 1}] ⚠️ Skipped unreadable entry: ${line}"
                                        }
                                    }
                                
                                    if (cronCode) {
                                        triggerList.add(parameterizedCron(cronCode.join('\n')))
                                        echo "[STAGE_O_E] ${cronCode.size()} Trigger(s) restored from TRIGGER_HISTORY!"
                                    }
                                } else {
                                    echo "[STAGE_O_E] TRIGGER_HISTORY File not found . . ."
                                } 
                            }
                        }catch(ignored){
                            echo "[STAGE_O_E] Unexpected Exception while loading TRIGGER_HISTORY :: ${ignored?.message ?: 'UNKNOWN_ERROR'}"
                        }
                    }
                    
                    // 0.e-2 Adding / Configuring 'Parameter / Choices' for the Pipeline
                    properties([
                    pipelineTriggers(triggerList),
                    parameters([
                        choice(name: 'branch',
                               choices: branchChoices,
                               description: 'Select Git_Branch'),
                        choice(name: 'env',
                               choices: envChoices,
                               description: 'Select Execution Environment'),
                        choice(name: 'browserType',
                               choices: browserChoices,
                               description: 'Select Client / Browser'),
                        choice(name: 'suiteFile',
                               choices: suiteChoices,
                               description: 'Select Product_Module / Suite File'),
                        choice(name: 'agentLabel',
                               choices: agentChoices,
                               description: 'Select Exec_Agent Node'),
                        booleanParam(name: 'HEADLESS',
                                defaultValue: true,
                                description: 'Run browser headless'),
                        booleanParam(name: 'refreshChoices',
                                defaultValue: false,
                                description: 'Re-fetch Branches, Environments & Suite Files from Repository'),
                        booleanParam(name: 'scheduleBuild',
                                defaultValue: false,
                                description: 'Enable Periodic Scheduled_Execution for this Pipeline.'),
                        [$class: 'DynamicReferenceParameter',
                            name: 'scheduleTime',
                            description: "Set Execution_Time in 24-Hour [HH:MM] Format (UTC). Effective only when 'scheduleBuild' is 'Enabled'.",
                            choiceType: 'ET_FORMATTED_HTML',
                            referencedParameters: '',
                            omitValueField: false,
                            script: [
                                $class: 'GroovyScript',
                                script: [
                                    $class: 'SecureGroovyScript',
                                    sandbox: false,
                                    script: '''
                                    def hours   = ['09','10','11','12','13','14','15','16','17','18','19','20',
                                    '21','22','23','00','01','02','03','04','05','06','07','08']
                                    def minutes = [
                                    'HH', '00', '01', '02', '03', '04', '05', '06', '07', '08', 
                                    '09', '10', '11', '12', '13', '14', '15', '16', '17', '18', 
                                    '19','20', '21', '22', '23', '24', '25', '26', '27', '28', 
                                    '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', 
                                    '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', 
                                    '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', 
                                    '59'
                                    ]

                                    def hourOpts   = hours.collect   { h -> "<option value='" + h + "'>" + h + "</option>" }.join('')
                                    def minuteOpts = minutes.collect { m -> "<option value='" + m + "'>" + m + "</option>" }.join('')

                                    return "<div style='display:flex;align-items:center;gap:2px;margin-top:2px;'>" +
                                    "<select name='scheduleHour' id='scheduleHour' onchange='updateScheduleTime()' style='height:32px;width:55px;padding:0 4px;font-size:13px;font-family:inherit;border:1px solid #D8DFEA;border-radius:6px;background:#fff;color:#333;cursor:pointer;'>" + hourOpts + "</select>" +
                                    "<span style='font-size:16px;font-weight:600;color:#555;line-height:32px;padding:0 10px;'>:</span>" +
                                    "<select name='scheduleMinute' id='scheduleMinute' onchange='updateScheduleTime()' style='height:32px;width:55px;padding:0 4px;font-size:13px;font-family:inherit;border:1px solid #D8DFEA;border-radius:6px;background:#fff;color:#333;cursor:pointer;'>" + minuteOpts + "</select>" +
                                    "<input type='hidden' name='value' id='scheduleTimeValue' value='09:00'/>" +
                                    "</div>" +
                                    "<script>function updateScheduleTime() {var hh = document.getElementById('scheduleHour').value; var mm = document.getElementById('scheduleMinute').value; document.getElementById('scheduleTimeValue').value = hh + ':' + mm; } updateScheduleTime(); </script>";
                                    '''
                                ],
                                fallbackScript: [
                                    $class: 'SecureGroovyScript',
                                    sandbox: false,
                                    script: ''' 
                                    return "<div style='display:flex;align-items:center;gap:2px;margin-top:2px;'>" +
                                    "<span style='height:32px;padding:0 8px;font-size:16px;font-family:inherit;border:1px solid #e74c3c;border-radius:6px;background:#fff;color:#e74c3c;display:flex;align-items:center;'>Error : Unable to render Time_Picker</span>" +
                                    "</div>"
                                    '''
                                ]
                            ]
                        ],
                        [$class: 'DynamicReferenceParameter',
                            name: 'scheduleDay',
                            description: "Select Day(s) for Scheduled_Execution. Effective only when 'scheduleBuild' is 'Enabled'.",
                            choiceType: 'ET_FORMATTED_HTML',
                            referencedParameters: '',
                            omitValueField: false,
                            script: [
                                $class: 'GroovyScript',
                                script: [
                                    $class: 'SecureGroovyScript',
                                    sandbox: false,
                                    script: '''
                                    def days = [
                                        [id: 'MON', label: 'Mon'],
                                        [id: 'TUE', label: 'Tue'],
                                        [id: 'WED', label: 'Wed'],
                                        [id: 'THU', label: 'Thu'],
                                        [id: 'FRI', label: 'Fri'],
                                        [id: 'SAT', label: 'Sat'],
                                        [id: 'SUN', label: 'Sun']
                                    ]

                                    def dayCheckboxes = days.collect { d ->"""
                                        <label style='display:inline-flex;align-items:center;gap:4px;margin-right:10px;font-size:12px;font-family:Arial,sans-serif;cursor:pointer;user-select:none;color:#333;'>
                                            <input type='checkbox' id='sday_${d.id}' value='${d.id}' checked
                                                onchange='onDayChange()'
                                                style='width:15px;height:15px;cursor:pointer;accent-color:#0067F3;border:2px solid #D8DFEA;border-radius:6px'>
                                            ${d.label}
                                        </label>"""
                                    }.join('')

                                    return """
                                    <div style='margin-top:4px;font-family:Arial,sans-serif;'>

                                        <!-- All Week Toggle -->
                                        <div style='margin-bottom:6px;'>
                                            <label style='display:inline-flex;align-items:center;gap:4px;font-size:12px;font-family:Arial,sans-serif;cursor:pointer;user-select:none;font-weight:bold;color:#333;'>
                                            <input type='checkbox' id='allWeekToggle' checked
                                            onchange='onAllWeekChange()'
                                            style='width:20px;height:20px;margin-left:2px;cursor:pointer;accent-color:#0067F3;border:2px solid #D8DFEA;border-radius:6px'>
                                            All week
                                            </label>
                                        </div>

                                        <!-- Individual Day Checkboxes -->
                                        <div id='individualDayToggle' style='display:inline-flex;flex-wrap:wrap;align-items:center;gap:2px;padding:5px 10px;border:1px solid #D8DFEA;border-radius:6px;background:#f8f8f8;opacity:0.55;pointer-events:none;'>
                                            ${dayCheckboxes}
                                        </div>

                                        <input type='hidden' name='value' id='scheduleDayValue' value='ALL'/>
                                    </div>

                                    <script>
                                        var DAY_IDS = ['MON','TUE','WED','THU','FRI','SAT','SUN'];

                                        function setAllDayCheckboxes(checked) {
                                            DAY_IDS.forEach(function(dayId) {
                                                var cb = document.getElementById('sday_' + dayId);
                                                if (cb) cb.checked = checked;
                                            });
                                        }

                                        function onAllWeekChange() {
                                            var allChecked = document.getElementById('allWeekToggle').checked;
                                            var panel = document.getElementById('individualDayToggle');

                                            setAllDayCheckboxes(allChecked);

                                            // Dim & Lock Individual CheckBox when 'All week' is 'Enabled'
                                            panel.style.opacity       = allChecked ? '0.55' : '1';
                                            panel.style.pointerEvents = allChecked ? 'none'  : 'auto';
                                            panel.style.background    = allChecked ? '#f8f8f8' : '#fff';

                                            updateDayValue();
                                        }

                                        function onDayChange() {
                                            var allBox  = document.getElementById('allWeekToggle');
                                            var panel   = document.getElementById('individualDayToggle');

                                            // Count how many days are checked
                                            var checkedCount = DAY_IDS.filter(function(dayId) {
                                                var cb = document.getElementById('sday_' + dayId);
                                                return cb && cb.checked;
                                            }).length;

                                            // Auto-enable 'All week' if all 7 days are individually checked
                                            if (checkedCount === DAY_IDS.length) {
                                                allBox.checked = true;
                                                panel.style.opacity = '0.55';
                                                panel.style.pointerEvents = 'none';
                                                panel.style.background    = '#f8f8f8';
                                            } else {
                                                allBox.checked            = false;
                                                panel.style.opacity       = '1';
                                                panel.style.pointerEvents = 'auto';
                                                panel.style.background    = '#fff';
                                            }

                                            updateDayValue();
                                        }

                                        function updateDayValue() {
                                            var allBox = document.getElementById('allWeekToggle');
                                            if (allBox.checked) {
                                                document.getElementById('scheduleDayValue').value = 'ALL';
                                                return;
                                            }

                                            var selectedDays = DAY_IDS.filter(function(dayId) {
                                                var cb = document.getElementById('sday_' + dayId);
                                                return cb && cb.checked;
                                            });

                                            document.getElementById('scheduleDayValue').value = selectedDays.length > 0 ? selectedDays.join(',') : 'ALL';
                                        }

                                        // Initialize on load
                                        onAllWeekChange();
                                    </script>
                                    """
                                    '''
                                ],
                                fallbackScript: [
                                    $class: 'SecureGroovyScript',
                                    sandbox: false,
                                    script: '''
                                        return "<div style='margin-top:4px;'>" +
                                        "<span style='height:32px;padding:0 8px;font-size:13px;font-family:inherit;" +
                                        "border:1px solid #e74c3c;border-radius:6px;background:#fff;color:#e74c3c;" +
                                        "display:inline-flex;align-items:center;'>Error : Unable to render the Week Options</span>" +
                                        "</div>"
                                    '''
                                ]
                            ]
                        ],
                        [$class: 'DynamicReferenceParameter',
                            name: 'deleteTriggers',
                            description: "Enable the Toggle & Paste the Cron Expression(s) of the Trigger(s) you want to remove!",
                            choiceType: 'ET_FORMATTED_HTML',
                            referencedParameters: '',
                            omitValueField: false,
                            script: [
                                $class: 'GroovyScript',
                                script: [
                                    $class: 'SecureGroovyScript',
                                    sandbox: false,
                                    script: '''
                                    return """
                                    <div style='margin-top:4px;font-family:Arial,sans-serif;'>

                                    <!-- Enable Delete Toggle -->
                                    <div style='margin-bottom:6px;'>
                                        <label style='display:inline-flex;align-items:center;gap:4px;font-size:12px;cursor:pointer;user-select:none;font-weight:bold;color:#333;'>
                                            <input type='checkbox' id='enableDeleteTriggers'
                                                onchange='onEnableDeleteTriggersChange()'
                                                style='width:20px;height:20px;margin-left:2px;cursor:pointer;accent-color:#0067F3;border:2px solid #D8DFEA;border-radius:6px;'>
                                            Enable Trigger(s) Deletion
                                        </label>
                                    </div>

                                    <!-- Delete: Triggers Input -->
                                    <div id='deleteTriggersCronPanel' style='opacity:0.45;pointer-events:none;'>
                                        <textarea id='deleteTriggersCronInput' rows='4'
                                            placeholder='Paste Trigger(s) here'
                                            oninput='onDeleteTriggersCronInput()'
                                            style='width:97%;padding:6px 8px;font-size:12px;font-family:monospace;border:1px solid #D8DFEA;border-radius:6px;background:#f8f8f8;color:#333;resize:vertical;'></textarea>
                                    </div>

                                    <input type='hidden' name='value' id='deleteTriggersValue' value=''/>
                                    </div>

                                    <script>
                                    function onEnableDeleteTriggersChange() {
                                        var enabled = document.getElementById('enableDeleteTriggers').checked;
                                        var panel   = document.getElementById('deleteTriggersCronPanel');

                                        panel.style.opacity       = enabled ? '1'    : '0.45';
                                        panel.style.pointerEvents = enabled ? 'auto' : 'none';

                                        if (!enabled) {
                                            document.getElementById('deleteTriggersCronInput').value = '';
                                            document.getElementById('deleteTriggersValue').value     = '';
                                        }
                                    }

                                    function onDeleteTriggersCronInput() {
                                        var enabled = document.getElementById('enableDeleteTriggers').checked;
                                        
                                        if (!enabled) return;
                                        document.getElementById('deleteTriggersValue').value =
                                        document.getElementById('deleteTriggersCronInput').value.trim();
                                    }
                                    </script>
                                    """
                                    '''
                                ],
                                fallbackScript: [
                                    $class: 'SecureGroovyScript',
                                    sandbox: false,
                                    script: '''
                                        return "<div style='margin-top:4px;'>" +
                                        "<span style='height:32px;padding:0 8px;font-size:13px;font-family:inherit;" +
                                        "border:1px solid #e74c3c;border-radius:6px;background:#fff;color:#e74c3c;" +
                                        "display:inline-flex;align-items:center;'>Error : Unable to render Delete Trigger(s) Widget</span>" +
                                        "</div>"
                                    '''
                                ]
                            ]
                        ],
                        booleanParam(name: 'clearTriggerHistory',
                                defaultValue: false,
                                description: 'Clear all saved Trigger History. Disables any Active / Scheduled Triggers.')
                        string(name: 'REGRESSION_THRESHOLD', defaultValue: '0',
                               description: 'Fail the build when Passed -> Failed exceeds this count. Use -1 to report without gating.')
                        booleanParam(name: 'INCLUDE_BROKEN', defaultValue: false,
                                     description: 'Also count Passed -> Broken against the threshold')
                    ])
                ])
                    
                echo "[STAGE_O_E] Updated Pipeline Execution_Choices & Re-Scheduled_Triggers [If Any]!"                    } 
                }
            }
        }
        
        // ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
        // STAGE 1  —  Checkout to the RESOLVED_BRANCH [Selected By User] & Fetch/Pull Updated:Automation_Framework
        // ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
        stage('Pull Automation Project') {
            when { expression { return !params.refreshChoices && !params.scheduleBuild && !params.clearTriggerHistory && (params.deleteTriggers?.trim() ?: ',') == ','} }
            agent { label "${env.RESOLVED_AGENT}" }
            steps {
                script{
                    echo "[STAGE_1] Fetching Automation_Project_Code from Remote Repository . . ."
                
                    // cleanWs deleteDirs: true, disableDeferredWipeout: true
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "${env.RESOLVED_BRANCH}"]],
                        userRemoteConfigs: [[
                            url: "${env.GIT_REPO_URL}",
                            credentialsId: "${env.GIT_CREDENTIALS}",
                            refspec: "+refs/heads/${env.RESOLVED_BRANCH}:refs/remotes/origin/${env.RESOLVED_BRANCH}"
                        ]],
                        extensions: [
                            [$class: 'CleanBeforeCheckout', deleteUntrackedNestedRepositories: true],
                            [$class: 'CloneOption',
                                shallow: true,
                                depth: 1,
                                noTags: true,
                                reference: '',
                                timeout: 20
                            ]
                        ]
                    ])
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
        // STAGE 2  —  Execute the RESOLVED_SUITE via Maven
        // ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
        stage('Project Execution [via. Maven]') {
            when { expression { return !params.refreshChoices && !params.scheduleBuild && !params.clearTriggerHistory && (params.deleteTriggers?.trim() ?: ',') == ','} }
            agent { label "${env.RESOLVED_AGENT}" }
            steps {
                echo "[STAGE_2] Starting Project Execution [via. Maven] . . . "
                
              
                    bat """
                        mvn clean test ^
                            -Dbranch=${env.RESOLVED_BRANCH} ^
                            -Denv=${env.RESOLVED_ENV} ^
                            -DsuiteFile=${env.RESOLVED_SUITE} ^
                            -DbrowserType=${env.RESOLVED_BROWSER} ^
                            -Dheadless=${env.RESOLVED_HEADLESS} ^
                            -Dmaven.test.failure.ignore=true
                    """

                // Compare Current_Run's Allure Results with the Previous Run [same Branch / Env / Suite]
                // —  Runs in THIS stage so it uses the same Workspace [@N] as the Test_Run
                // —  Report_Only: never changes the Build Result [post{} mails only on SUCCESS / FAILURE]
                // —  Report → Build Artifacts: Execution_Reports/Allure_Comparison/comparison-report.html [+ .csv] & attached to the Mail
                script {
                    echo "[STAGE_2_COMPARE] Comparing Allure Results with the Previous Run . . ."
                    compareWithPreviousRun()
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
    // POST BLOCK — Runs after Pipeline Execution
    // ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
    post {
        always {
            script{
                if (!params.refreshChoices && !params.scheduleBuild && !params.clearTriggerHistory && (params.deleteTriggers?.trim() ?: ',') == ','){
                    node("${env.RESOLVED_AGENT}") {
                        echo '[POST_EXECUTION] Preparing Execution_Reports . . .'
                        // fileExists, not 'bat … echo true': bat echoes its own command line, so the output always contained 'true'
                        def reportsExist = fileExists('Execution_Reports')

                        if (reportsExist) {
                            // A Zip failure [empty Folder / locked File] must not fail the Build or stop the Mail → warn & continue
                            def zipRc = bat(returnStatus: true,
                                script: 'powershell -NoProfile -Command "Compress-Archive -Path Execution_Reports\\* -DestinationPath Execution_Reports.zip -Force"')
                            if (zipRc == 0) {
                                archiveArtifacts artifacts: 'Execution_Reports.zip', fingerprint: true
                            } else {
                                echo "⚠️ [POST_EXECUTION] Zipping Execution_Reports failed [exit ${zipRc}] — see the PowerShell error above. Archiving the Reports unzipped . . ."
                                archiveArtifacts artifacts: 'Execution_Reports/**', allowEmptyArchive: true
                            }
                        } else {
                            echo '[POST_EXECUTION] Execution_Reports not found — skipping archive. . .'
                        }
                    }
                }
            }
        }

        success {
            script {
                if (!params.refreshChoices && !params.scheduleBuild && !params.clearTriggerHistory && (params.deleteTriggers?.trim() ?: ',') == ',') {
                    node("${env.RESOLVED_AGENT}") {
                        sendMail('SUCCESS')
                    }
                }
            }
        }

        failure {
            script {
                if (!params.refreshChoices && !params.scheduleBuild && !params.clearTriggerHistory && (params.deleteTriggers?.trim() ?: ',') == ',') {
                    node("${env.RESOLVED_AGENT}") {
                        sendMail('FAILURE')
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
// Parse Properties & Group w.r.t "Key"
def parseProperty(String key, String text) {
    def result = []
    def lines = text.split('\n')
    for (int i = 0; i < lines.size(); i++) {
        def line = lines[i].trim()
        
        // Strip Comment_Keyword (#) from Properties before adding 
        def stripped = line.startsWith('#') ? line.substring(1) : line
        if (stripped.startsWith("${key}=")) {
            def val = stripped.substring(key.length() + 1).trim()
            if (val && !result.contains(val)) result.add(val)
        }
    }
    return result
}

// Build Choice_List
def buildChoiceList(List values, String defaultValue) {
    def result = []
    // Add every value EXCEPT the default, preserving order
    for (int i = 0; i < values.size(); i++) {
        def v = values[i]
        if (v && v != defaultValue) {
            result.add(v)
        }
    }
    // Prepend default so it is always the 'First/Default Option' and appears exactly Once
    result.add(0, defaultValue)
    return result
}

// Translate scheduleDay param value → cron DOW field
def resolveExecutionDay(String raw) {
    if (!raw || raw.trim() == 'ALL') return '*'

    def order   = ['SUN','MON','TUE','WED','THU','FRI','SAT']
    def numMap = ['SUN':'0','MON':'1','TUE':'2','WED':'3','THU':'4','FRI':'5','SAT':'6']
    def tokens  = raw.split(',').collect { it.trim() }.findAll { it && order.contains(it) }
    def selected = order.findAll { tokens.contains(it) }

    if (selected.size() == 0 || selected.size() == 7) return '*'

    // Collapse consecutive runs into numeric ranges e.g. MON,TUE,WED,THU,FRI → 2-6
    def nums  = selected.collect { numMap[it].toInteger() }
    def ranges = []
    int i = 0
    while (i < nums.size()) {
        int j = i
        while (j + 1 < nums.size() && nums[j+1] == nums[j] + 1) j++
        ranges.add(j - i >= 2 ? "${nums[i]}-${nums[j]}" : nums[i..j].join(','))
        i = j + 1
    }
    return ranges.join(',')
}

// E-mail Configurations [for sharing Execution Status & Reports]
def sendMail(String status) {
    def filePath   = 'Execution_Reports.zip'
    // No Zip when the Run produced no Execution_Reports [e.g. Suite failed to start] → mail without the Zip
    def zipExists  = fileExists(filePath)
    def fileSizeMB = zipExists ? powershell(
        script: "(Get-Item ${filePath}).Length / 1MB",
        returnStdout: true
    ).trim().toFloat() : 0

    def attachFile  = zipExists && fileSizeMB < 20
    // Comparison_Report is small → always attached directly when it was generated
    def comparisonFile = 'Execution_Reports/Allure_Comparison/comparison-report.html'
    def attachments    = [attachFile ? filePath : null, fileExists(comparisonFile) ? comparisonFile : null].findAll { it }.join(',')
    def timestamp   = new Date().format('dd-MMM-yyyy  HH:mm:ss z')

    // ── Jira Ticket Creation [Failed / Broken Tests → Jira] — Controlled via 'JIRA_*' Environment Variables ──
    def jiraEnabled        = (env.JIRA_ENABLED ?: 'false').trim().equalsIgnoreCase('true')
    def jiraDryRun         = (env.JIRA_DRY_RUN ?: 'false').trim().equalsIgnoreCase('true')
    def jiraVersion        = resolveJiraVersion(env.JIRA_PACKAGE_VERSION, env.RESOLVED_BRANCH)
    def jiraResult         = jiraEnabled ? raiseJiraTickets() : [summaryReady: false, rows: [], total: 0, passed: 0, failed: 0, skipped: 0, note: '']
    def qaReportSection    = buildQaReportSection(jiraResult, [
                                enabled : jiraEnabled,
                                dryRun  : jiraDryRun,
                                version : jiraVersion,
                                env     : env.RESOLVED_ENV ?: '',
                                suite   : env.RESOLVED_SUITE ?: '',
                                appUrl  : env.JIRA_APP_URL ?: '',
                                serverIp: env.JIRA_TARGET_SERVER_IP ?: '',
                                greeting: env.JIRA_MAIL_GREETING ?: 'Hi Team,'
                             ])

    def statusColor = status == 'SUCCESS' ? '#2ecc71' : status == 'FAILURE' ? '#e74c3c' : '#f39c12'
    def statusLabel = status == 'SUCCESS' ? '✅ PASSED'  : status == 'FAILURE' ? '❌ FAILED': '⚠️ UNSTABLE'

    def attachNote = attachFile
    ? '<p style="margin:8px 0 0;font-size:12px;color:#aab8c2;"> 🌐 Execution_Report is also attached to this E-mail!</p>'
    : '<p style="margin:8px 0 0;font-size:12px;color:#aab8c2;">🌐 Click the Button to View the Execution_Reports.</p>'

    def reportSection = """
    <a href="${env.BUILD_URL}artifact/" target="_blank"
        style="display:inline-block;margin-top:6px;padding:10px 28px;
                background:#b39ddb;color:#16213e;box-shadow:0 2px 10px rgba(15,52,96,0.4);font-weight:bold;font-size:13px;
                border-radius:6px;text-decoration:none;letter-spacing:1px;">
        📂 View Execution Reports
    </a>
    ${attachNote}
    """
    
    def body = """
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="margin:0;padding:0;background:#1a1a2e;font-family:'Courier New',Courier,monospace;">

  <table width="100%" cellpadding="0" cellspacing="0" style="background:#1a1a2e;padding:30px 0;">
    <tr><td align="center">
      <table width="620" cellpadding="0" cellspacing="0"
             style="background:#16213e;border-radius:10px;overflow:hidden;
                    border:1px solid #0f3460;box-shadow:0 4px 24px rgba(0,0,0,0.5);">

        <!-- ── HEADER ── -->
        <tr>
          <td align="center" style="background:#0f3460;padding:28px 24px 20px;">
            <p style="margin:0 0 6px;font-size:11px;letter-spacing:3px;
                      color:#4fc3f7;text-transform:uppercase;">Jenkins CI / CD</p>
            <h1 style="margin:0;font-size:22px;color:#ffffff;letter-spacing:1px;">
              Execution Report
            </h1>
            <p style="margin:8px 0 0;font-size:13px;color:#a0b4cc;">
              ${env.JOB_NAME} &nbsp;|&nbsp; Build <strong style="color:#fff;">#${env.BUILD_NUMBER}</strong>
            </p>
          </td>
        </tr>

        <!-- ── STATUS BADGE ── -->
        <tr>
          <td align="center" style="padding:24px 24px 8px;">
            <span style="display:inline-block;background:${statusColor};color:#fff;
                         font-size:18px;font-weight:bold;letter-spacing:2px;
                         padding:10px 40px;border-radius:50px;">
              ${statusLabel}
            </span>
            <p style="margin:10px 0 0;font-size:12px;color:#7f8c8d;">${timestamp}</p>
          </td>
        </tr>

        <!-- ── BUILD INFO ── -->
        <tr>
          <td style="padding:16px 28px 0;">
            <p style="margin:0 0 10px;font-size:10px;letter-spacing:2px;
                      color:#4fc3f7;text-transform:uppercase;border-bottom:1px solid #0f3460;
                      padding-bottom:8px;">Build Info</p>
            <table width="100%" cellpadding="4" cellspacing="0"
                   style="font-size:13px;color:#c8d6e5;">
              <tr>
                <td width="120" style="color:#7f8c8d;">Project</td>
                <td style="color:#fff;">${env.JOB_NAME}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">Build No</td>
                <td style="color:#fff;">#${env.BUILD_NUMBER}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">View</td>
                <td style="color:#fff;">${env.VIEW_NAME}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">Branch</td>
                <td style="color:#fff;">${env.RESOLVED_BRANCH}</td>
              </tr>
            </table>
          </td>
        </tr>

        <!-- ── EXECUTION DETAILS ── -->
        <tr>
          <td style="padding:16px 28px 0;">
            <p style="margin:0 0 10px;font-size:10px;letter-spacing:2px;
                      color:#4fc3f7;text-transform:uppercase;border-bottom:1px solid #0f3460;
                      padding-bottom:8px;">Execution Details</p>
            <table width="100%" cellpadding="4" cellspacing="0"
                   style="font-size:13px;color:#c8d6e5;">
              <tr>
                <td width="120" style="color:#7f8c8d;">Environment</td>
                <td style="color:#fff;">${env.RESOLVED_ENV}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">Suite</td>
                <td style="color:#fff;">${env.RESOLVED_SUITE}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">Browser</td>
                <td style="color:#fff;">${env.RESOLVED_BROWSER}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">Agent</td>
                <td style="color:#fff;">${env.RESOLVED_AGENT}</td>
              </tr>
            </table>
          </td>
        </tr>

        ${qaReportSection}

        ${buildComparisonSection()}

        <!-- ── REPORT / BUILD URL ── -->
        <tr>
          <td style="padding:16px 28px 0;">
            <p style="margin:0 0 10px;font-size:10px;letter-spacing:2px;
                      color:#4fc3f7;text-transform:uppercase;border-bottom:1px solid #0f3460;
                      padding-bottom:8px;">Execution Reports</p>
            ${reportSection}
          </td>
        </tr>

        <tr>
          <td align="center" style="padding-top:28px; padding-bottom:40px">
            <span style="font-size:12px;letter-spacing:2px;
                      color:#c8d6e5;text-transform:uppercase">↗️ BUILD_URL: </span> <a href="${env.BUILD_URL}"
               style="color:#4fc3f7;font-size:12px;word-break:break-all;">${env.BUILD_URL}
            </a>
          </td>
        </tr>

        <!-- ── FOOTER ── -->
        <tr>
          <td align="center"
              style="padding:24px;margin-top:16px;
                     border-top:1px solid #0f3460;color:#4a5568;font-size:11px;">
            This is an automated notification. Do not reply.
          </td>
        </tr>

      </table>
    </td></tr>
  </table>

</body>
</html>
"""

    emailext(
        subject: "[Jenkins] ${statusLabel} | ${env.JOB_NAME} #${env.BUILD_NUMBER} | ${env.RESOLVED_ENV} | ${env.RESOLVED_BRANCH}",
        body: body,
        to: env.MAIL_TO,
        replyTo: env.MAIL_REPLY,
        attachmentsPattern: attachments,
        mimeType: 'text/html'       // ← changed from text/plain
    )
}

// ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
// ALLURE RUN COMPARISON HELPER FUNCTIONS  [Invoked from STAGE 2 & sendMail()]
// ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════

// Compare Current_Run with the Previous Run of the same Branch / Env / Suite via AllureComparisonRunner [mvn exec:exec@allure-compare]
// —  Baseline is kept on the Exec_Agent NEXT TO the Workspace, because CleanBeforeCheckout wipes the git-ignored Execution_Reports
// —  Current_Run's Results always become the next Run's Baseline
// —  Sets env.CMP_* for the Mail; fails build when threshold exceeded [Quality Gate]
def compareWithPreviousRun() {
    env.CMP_STATE = 'SKIPPED'
    env.CMP_NOTE  = ''
    try {
        def currentDir  = 'Execution_Reports\\Allure_Report\\allure-results'     // matches pom.xml 'allure.results.directory'
        def outputDir   = 'Execution_Reports\\Allure_Comparison'
        def runKey      = "${env.RESOLVED_BRANCH}__${env.RESOLVED_ENV}__${env.RESOLVED_SUITE}".replaceAll('[^A-Za-z0-9._-]', '_')
        // One fixed Dir per Job: drop the concurrent-Workspace suffix [epm_automation@3 → epm_automation] and use '\' only,
        // since the Agent root is '/opt/jenkins-agent' and a leading '/' is read as a switch by robocopy
        def jobWs       = env.WORKSPACE.replaceAll('@\\d+$', '').replace('/', '\\')
        def baselineDir = resolveWindowsPath("${jobWs}_allure_baselines\\${runKey}")
        echo "[STAGE_2_COMPARE] Baseline Dir :: ${baselineDir}"

        if (!hasAllureResults(currentDir)) {
            env.CMP_NOTE = 'No Allure results produced by this run'
            echo "[STAGE_2_COMPARE] ${env.CMP_NOTE} — skipping Comparison . . ."
            return
        }

        def threshold = (params.REGRESSION_THRESHOLD?.trim() ?: '0') as int
        def includeBroken = (params.INCLUDE_BROKEN == true)

        if (hasAllureResults(baselineDir)) {
            def rc = bat(returnStatus: true, script: """
                @mvn -B -ntp test-compile exec:exec@allure-compare ^
                    "-Dallure.compare.previous=${baselineDir}" ^
                    "-Dallure.compare.current=${currentDir}" ^
                    "-Dallure.compare.output=${outputDir}" ^
                    "-Dallure.compare.threshold=${threshold}" ^
                    "-Dallure.compare.includeBroken=${includeBroken}" > allure_compare.log 2>&1
            """)
            def compareLog = readFile('allure_compare.log')
            echo compareLog

            if (rc == 0) {
                def s = parseComparisonSummary(compareLog)
                env.CMP_STATE       = 'DONE'
                env.CMP_REGRESSIONS = "${s.pf.toInteger() + s.pb.toInteger()}"
                env.CMP_FIXES       = "${s.fp.toInteger() + s.bp.toInteger() + s.sp.toInteger()}"
                env.CMP_NEW         = s.added
                env.CMP_REMOVED     = s.removed
                env.CMP_RATE_PREV   = s.ratePrev
                env.CMP_RATE_CUR    = s.rateCur
                archiveArtifacts artifacts: 'Execution_Reports/Allure_Comparison/*', allowEmptyArchive: true
                echo "[STAGE_2_COMPARE] Comparison done :: Regressions=${env.CMP_REGRESSIONS} Fixes=${env.CMP_FIXES} New=${env.CMP_NEW} Removed=${env.CMP_REMOVED}"
            } else if (rc == 1) {
                // Threshold exceeded - FAIL the build (Quality Gate)
                def s = parseComparisonSummary(compareLog)
                env.CMP_STATE       = 'THRESHOLD_EXCEEDED'
                env.CMP_REGRESSIONS = "${s.pf.toInteger() + s.pb.toInteger()}"
                env.CMP_FIXES       = "${s.fp.toInteger() + s.bp.toInteger() + s.sp.toInteger()}"
                env.CMP_NEW         = s.added
                env.CMP_REMOVED     = s.removed
                env.CMP_RATE_PREV   = s.ratePrev
                env.CMP_RATE_CUR    = s.rateCur
                archiveArtifacts artifacts: 'Execution_Reports/Allure_Comparison/*', allowEmptyArchive: true
                error("❌ REGRESSION THRESHOLD EXCEEDED: ${env.CMP_REGRESSIONS} tests went Passed -> Failed (threshold: ${threshold}). See 'Allure Run Comparison' report.")
            } else {
                env.CMP_STATE = 'ERROR'
                env.CMP_NOTE  = "AllureComparisonRunner exited with code ${rc} — see Console_Output"
                echo "⚠️ [STAGE_2_COMPARE] ${env.CMP_NOTE}"
            }
        } else {
            env.CMP_STATE = 'NO_BASELINE'
            env.CMP_NOTE  = 'First run for this Branch / Env / Suite on this Agent — Baseline saved for the next run'
            echo "[STAGE_2_COMPARE] ${env.CMP_NOTE}"
        }

        // Current_Run → Baseline for the next Run [robocopy exit codes below 8 = success]
        def copyRc = bat(returnStatus: true, script: "@robocopy \"${currentDir}\" \"${baselineDir}\" /MIR /NFL /NDL /NJH /NJS /NP > nul")
        if (copyRc >= 8) echo "⚠️ [STAGE_2_COMPARE] Baseline update failed [robocopy exit ${copyRc}] :: ${baselineDir}"
    } catch (err) {
        env.CMP_STATE = 'ERROR'
        env.CMP_NOTE  = err?.message ?: 'UNKNOWN_ERROR'
        echo "⚠️ [STAGE_2_COMPARE] Allure Comparison skipped :: ${env.CMP_NOTE}"
    }
}

// True when the Dir holds at least one Allure '*-result.json'
def hasAllureResults(String dir) {
    return bat(returnStatus: true, script: "@if exist \"${dir}\\*-result.json\" (exit /b 0) else (exit /b 1)") == 0
}

// Expand a Windows Path to its Full form incl. Drive [\opt\jenkins-agent\... → C:\opt\jenkins-agent\...]
def resolveWindowsPath(String path) {
    return bat(returnStdout: true, script: "@for %%I in (\"${path}\") do @echo %%~fI").trim()
}

// Read Counts from AllureComparisonRunner's Console_Summary [e.g. 'Passed -> Failed    : 2']
@NonCPS
def parseComparisonSummary(String text) {
    def labels = [pf: 'Passed -> Failed', pb: 'Passed -> Broken', fp: 'Failed -> Passed', bp: 'Broken -> Passed',
                  sp: 'Skipped -> Passed', added: 'New Test', removed: 'Removed Test',
                  ratePrev: 'Pass Rate Previous', rateCur: 'Pass Rate Current']
    def result = [:]
    labels.each { key, label ->
        def m = (text =~ /(?m)^${label}\s*:\s*(\S+)/)
        result[key] = m.find() ? m.group(1) : '0'
    }
    return result
}

// Mail Section :: Comparison With Previous Run [empty when the Comparison did not run]
def buildComparisonSection() {
    def state = env.CMP_STATE ?: 'SKIPPED'
    if (state == 'SKIPPED') return ''

    def heading = """
            <p style="margin:0 0 10px;font-size:10px;letter-spacing:2px;
                      color:#4fc3f7;text-transform:uppercase;border-bottom:1px solid #0f3460;
                      padding-bottom:8px;">Comparison With Previous Run</p>"""

    if (state == 'THRESHOLD_EXCEEDED') {
        def regressions = env.CMP_REGRESSIONS.toInteger()
        def reportUrl   = "${env.BUILD_URL}artifact/Execution_Reports/Allure_Comparison/comparison-report.html"
        return """
        <tr>
          <td style="padding:16px 28px 0;">${heading}
            <table width="100%" cellpadding="4" cellspacing="0"
                   style="font-size:13px;color:#c8d6e5;">
              <tr>
                <td width="200" style="color:#7f8c8d;">Regressions [Passed → Failed / Broken]</td>
                <td style="color:#e74c3c;font-weight:bold;">${regressions} ⚠️ THRESHOLD EXCEEDED</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">Fixed [now Passing]</td>
                <td style="color:#fff;">${env.CMP_FIXES}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">New / Removed Tests</td>
                <td style="color:#fff;">${env.CMP_NEW} / ${env.CMP_REMOVED}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">Pass Rate [Previous → Current]</td>
                <td style="color:#fff;">${env.CMP_RATE_PREV} → ${env.CMP_RATE_CUR}</td>
              </tr>
            </table>
            <a href="${reportUrl}" target="_blank"
               style="display:inline-block;margin-top:10px;padding:8px 22px;
                      background:#e74c3c;color:#fff;font-weight:bold;font-size:12px;
                      border-radius:6px;text-decoration:none;letter-spacing:1px;">
              📊 View Comparison Report
            </a>
            <p style="margin:8px 0 0;font-size:12px;color:#e74c3c;">❌ Quality Gate FAILED: Regression threshold exceeded!</p>
          </td>
        </tr>"""
    }

    if (state != 'DONE') {
        return """
        <tr>
          <td style="padding:16px 28px 0;">${heading}
            <p style="margin:0;font-size:13px;color:#c8d6e5;">${escapeHtml(env.CMP_NOTE)}</p>
          </td>
        </tr>"""
    }

    def regressions = env.CMP_REGRESSIONS.toInteger()
    def regColor    = regressions > 0 ? '#e74c3c' : '#2ecc71'
    def reportUrl   = "${env.BUILD_URL}artifact/Execution_Reports/Allure_Comparison/comparison-report.html"
    return """
        <tr>
          <td style="padding:16px 28px 0;">${heading}
            <table width="100%" cellpadding="4" cellspacing="0"
                   style="font-size:13px;color:#c8d6e5;">
              <tr>
                <td width="200" style="color:#7f8c8d;">Regressions [Passed → Failed / Broken]</td>
                <td style="color:${regColor};font-weight:bold;">${regressions}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">Fixed [now Passing]</td>
                <td style="color:#fff;">${env.CMP_FIXES}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">New / Removed Tests</td>
                <td style="color:#fff;">${env.CMP_NEW} / ${env.CMP_REMOVED}</td>
              </tr>
              <tr>
                <td style="color:#7f8c8d;">Pass Rate [Previous → Current]</td>
                <td style="color:#fff;">${env.CMP_RATE_PREV} → ${env.CMP_RATE_CUR}</td>
              </tr>
            </table>
            <a href="${reportUrl}" target="_blank"
               style="display:inline-block;margin-top:10px;padding:8px 22px;
                      background:#4fc3f7;color:#16213e;font-weight:bold;font-size:12px;
                      border-radius:6px;text-decoration:none;letter-spacing:1px;">
              📊 View Comparison Report
            </a>
            <p style="margin:8px 0 0;font-size:12px;color:#aab8c2;">📎 comparison-report.html is also attached to this E-mail!</p>
          </td>
        </tr>"""
}

// ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
// JIRA HELPER FUNCTIONS  [Invoked from sendMail()]
// ══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════

// Resolve CI_Version for Jira :: JIRA_PACKAGE_VERSION (if not 'AUTO') → else derive from RESOLVED_BRANCH [10.12.002_Automation → 10.12.002]
@NonCPS
def resolveJiraVersion(String configured, String branch) {
    def cfgVer = configured?.trim()
    if (cfgVer && !cfgVer.equalsIgnoreCase('AUTO')) return cfgVer
    def m = ((branch ?: '') =~ /(\d+\.\d+\.\d+)/)
    return m.find() ? m.group(1) : (branch ?: 'NA')
}

// Run Jira_Engine [Windows PowerShell — no Python required] on the Exec_Agent
// → Parse Allure Results → Create / Update Jira Ticket(s) → Return Summary + Ticket Rows for the Mail
def raiseJiraTickets() {
    def jiraResult = [summaryReady: false, rows: [], total: 0, passed: 0, failed: 0, skipped: 0, note: '']
    def outDir     = (env.JIRA_OUTPUT_DIR ?: 'Jira_Reports').trim()
    def isDryRun   = (env.JIRA_DRY_RUN ?: 'false').trim().equalsIgnoreCase('true')
    def packageVer = resolveJiraVersion(env.JIRA_PACKAGE_VERSION, env.RESOLVED_BRANCH)
    def scriptFile = 'jira_ticket_engine.ps1'
    def psCommand  = "@powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File \"${scriptFile}\""

    echo "[JIRA] Processing Failed / Broken Test(s) :: Project=${env.JIRA_PROJECT_KEY} | Version=${packageVer} | Env=${env.RESOLVED_ENV} | DryRun=${isDryRun}"

    try {
        writeFile file: scriptFile, text: getJiraEngineScript(), encoding: 'UTF-8'

        def rc = 0
        withEnv(["JIRA_RESOLVED_VERSION=${packageVer}"]) {
            if (isDryRun) {
                rc = bat(script: psCommand, returnStatus: true)
            } else {
                withCredentials([usernamePassword(
                        credentialsId: env.JIRA_CREDS_ID,
                        usernameVariable: 'JIRA_USER',
                        passwordVariable: 'JIRA_TOKEN')]) {
                    rc = bat(script: psCommand, returnStatus: true)
                }
            }
        }

        if (rc != 0) {
            def errFile = "${outDir}/jira_engine_error.txt"
            def errText = fileExists(errFile) ? readFile(errFile).trim() : ''
            def errLine = errText ? errText.split('\n')[0].trim() : "Jira_Engine exited with code ${rc}"
            jiraResult.note = errLine.length() > 300 ? errLine.substring(0, 300) + '...' : errLine
            echo "⚠️ [JIRA] Jira_Engine reported an error :: ${jiraResult.note}"
        }

        // Test_Summary [written BEFORE any Jira call, so counts are mailed even if Jira fails]
        def summaryFile = "${outDir}/test_summary.json"
        if (fileExists(summaryFile)) {
            def s = readJSON file: summaryFile
            jiraResult.total        = (s.total   ?: 0) as Integer
            jiraResult.passed       = (s.passed  ?: 0) as Integer
            jiraResult.failed       = (s.failed  ?: 0) as Integer
            jiraResult.skipped      = (s.skipped ?: 0) as Integer
            jiraResult.summaryReady = true
        }

        // Ticket Rows [one per Failed / Broken Test — 'PENDING' rows stay listed as 'Not Raised' if Jira fails]
        def ticketFile = "${outDir}/jira_summary.json"
        if (fileExists(ticketFile)) {
            def t    = readJSON file: ticketFile
            def list = (t instanceof List) ? t : [t]
            for (int i = 0; i < list.size(); i++) {
                def r = list[i]
                if (!(r instanceof Map)) continue
                jiraResult.rows.add([
                    ticket       : "${r.ticket ?: ''}".toString(),
                    ticket_url   : "${r.ticket_url ?: ''}".toString(),
                    action       : "${r.action ?: ''}".toString(),
                    component    : "${r.component ?: ''}".toString(),
                    owner        : "${r.owner ?: ''}".toString(),
                    issue_summary: "${r.issue_summary ?: ''}".toString(),
                    priority     : "${r.priority ?: 'High'}".toString()
                ])
            }
        }

        echo "[JIRA] Jira_Engine finished :: ${jiraResult.rows.size()} Ticket Row(s) | Total=${jiraResult.total} Passed=${jiraResult.passed} Failed=${jiraResult.failed} Skipped=${jiraResult.skipped}"
    } catch (err) {
        jiraResult.note = err?.message ?: 'UNKNOWN_ERROR'
        echo "⚠️ [JIRA] Jira Ticket Creation skipped :: ${jiraResult.note}"
    } finally {
        bat "del /f /q \"${scriptFile}\" 2>nul & exit /b 0"
    }
    return jiraResult
}

// Escape HTML special characters [for Error_Messages / Locators inside Mail]
@NonCPS
def escapeHtml(Object raw) {
    if (raw == null) return ''
    return raw.toString()
            .replace('&', '&amp;')
            .replace('<', '&lt;')
            .replace('>', '&gt;')
            .replace('"', '&quot;')
}

// Mail Section :: QA Report [Greeting → Intro → Target Environment → Total / dependency failure / Pass → Failed Ticket List → Pass List]
@NonCPS
def buildQaReportSection(Map r, Map o) {
    if (!o.enabled) return ''

    def tdHead  = 'border:1px solid #444;padding:8px 6px;text-align:center;'
    def tdCell  = 'border:1px solid #444;padding:8px 6px;'
    def suite   = o.suite ?: ''
    def suiteNm = suite ? suite.substring(suite.lastIndexOf('/') + 1).replaceAll(/(?i)\.xml$/, '') : 'automation'
    def content = ''

    content += """<p style="margin:0 0 14px 0;">${escapeHtml(o.greeting)}</p>"""
    content += """<p style="margin:0 0 14px 0;">We have executed the <b>${escapeHtml(suiteNm)}</b> testing scripts for <b>${escapeHtml(o.version)}</b> in the <b>${escapeHtml(o.env)}</b> environment.</p>"""

    if (o.appUrl) {
        def ipText = o.serverIp ? " (${escapeHtml(o.serverIp)})" : ''
        content += """<p style="margin:0 0 14px 0;">Target Environment : <a href="${escapeHtml(o.appUrl)}" target="_blank" style="color:#0052cc;text-decoration:underline;">${escapeHtml(o.appUrl)}</a>${ipText}</p>"""
    }

    if (o.dryRun) {
        content += """<p style="margin:0 0 14px 0;color:#b45f06;font-weight:bold;">[DRY RUN] Jira Tickets were simulated only — no real tickets were created.</p>"""
    }

    if (!r?.summaryReady) {
        content += """<p style="margin:0;color:#cc0000;font-weight:bold;">⚠️ Test results could not be processed :: ${escapeHtml(r?.note)}</p>"""
    } else {
        content += """<p style="margin:0 0 16px 0;">The total number of scripts executed, the status of the existing tickets, and the new tickets created by the team are listed below</p>"""

        // Summary Table [Total / dependency failure / Pass]
        def skippedRow = (r.skipped > 0) ? """
              <tr>
                <td style="border:1px solid #000;padding:6px 12px;background-color:#ffe599;font-weight:bold;">Skipped</td>
                <td style="border:1px solid #000;padding:6px 12px;text-align:center;font-weight:bold;">${r.skipped}</td>
              </tr>""" : ''

        content += """
            <table cellpadding="0" cellspacing="0" style="border-collapse:collapse;width:220px;margin-bottom:24px;font-family:Arial,Helvetica,sans-serif;font-size:13px;color:#111;">
              <tr>
                <td style="border:1px solid #000;padding:6px 12px;background-color:#fce5cd;font-weight:bold;width:140px;">Total</td>
                <td style="border:1px solid #000;padding:6px 12px;text-align:center;font-weight:bold;">${r.total}</td>
              </tr>
              <tr>
                <td style="border:1px solid #000;padding:6px 12px;background-color:#ff0000;color:#000;font-weight:bold;">dependency failure</td>
                <td style="border:1px solid #000;padding:6px 12px;text-align:center;font-weight:bold;">${r.failed}</td>
              </tr>
              <tr>
                <td style="border:1px solid #000;padding:6px 12px;background-color:#93c47d;font-weight:bold;">Pass</td>
                <td style="border:1px solid #000;padding:6px 12px;text-align:center;font-weight:bold;">${r.passed}</td>
              </tr>${skippedRow}
            </table>"""

        // Failed Ticket List
        if (r.rows) {
            def rowsHtml = ''
            for (row in r.rows) {
                def ticketHtml
                if (row.ticket_url) {
                    ticketHtml = "<a href=\"${escapeHtml(row.ticket_url)}\" target=\"_blank\" style=\"color:#0052cc;text-decoration:underline;\"><b>${escapeHtml(row.ticket)}</b></a>"
                } else if (row.action == 'DRY_RUN') {
                    ticketHtml = "<b>DRY_RUN</b>"
                } else {
                    ticketHtml = "<span style=\"color:#cc0000;font-weight:bold;\">Not Raised</span>"
                }
                if (row.action == 'UPDATED') {
                    ticketHtml += "<div style=\"font-size:10px;color:#666;\">(Existing)</div>"
                }

                rowsHtml += """
                <tr>
                  <td style="${tdCell}text-align:center;">${ticketHtml}</td>
                  <td style="${tdCell}word-break:break-word;">${escapeHtml(row.issue_summary)}</td>
                  <td style="${tdCell}text-align:center;">${escapeHtml(o.version)}</td>
                  <td style="${tdCell}text-align:center;">${escapeHtml(row.priority)}</td>
                  <td style="${tdCell}text-align:center;">${escapeHtml(o.env)}</td>
                  <td style="${tdCell}text-align:center;background-color:#ff0000;color:#000;font-weight:bold;">Not working</td>
                </tr>"""
            }

            content += """
            <p style="margin:0 0 10px 0;">Failed tickets due to which we are blocked to test remaining ${r.failed} test cases.</p>
            <table width="100%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;margin-bottom:24px;font-family:Arial,Helvetica,sans-serif;font-size:11px;color:#111;table-layout:fixed;">
              <thead>
                <tr style="background-color:#fce5cd;">
                  <th width="80" style="${tdHead}"><u>Ticket NO</u></th>
                  <th            style="${tdHead}">Issues</th>
                  <th width="68" style="${tdHead}">Version</th>
                  <th width="60" style="${tdHead}">Priority</th>
                  <th width="68" style="${tdHead}">ENV</th>
                  <th width="66" style="${tdHead}">Ticket Status</th>
                </tr>
              </thead>
              <tbody>
                ${rowsHtml}
              </tbody>
            </table>"""
        } else if (r.failed == 0) {
            content += """<p style="color:#274e13;font-weight:bold;margin:0 0 20px 0;">✅ All automated tests passed successfully with 0 defects.</p>"""
        }

        // Pass List
        if (r.passed > 0) {
            content += """
            <p style="margin:0 0 6px 0;">Pass ticket List</p>
            <p style="color:#274e13;margin:0 0 10px 0;">${r.passed} test script(s) executed and verified working as expected.</p>"""
        }

        // Jira Warning [results parsed, but Jira step failed]
        if (r.note) {
            content += """<p style="margin:10px 0 0 0;font-size:12px;color:#b45f06;">⚠️ Jira Ticket Creation could not be completed :: ${escapeHtml(r.note)}</p>"""
        }
    }

    return """
        <!-- ── QA REPORT & JIRA TICKETS ── -->
        <tr>
          <td style="padding:16px 28px 0;">
            <p style="margin:0 0 10px;font-size:10px;letter-spacing:2px;
                      color:#4fc3f7;text-transform:uppercase;border-bottom:1px solid #0f3460;
                      padding-bottom:8px;">QA Report &amp; Jira Tickets</p>
            <table width="100%" cellpadding="0" cellspacing="0" bgcolor="#ffffff" style="background:#ffffff;border-radius:6px;">
              <tr>
                <td style="padding:16px 14px;font-family:Arial,Helvetica,sans-serif;font-size:13px;color:#111111;line-height:1.5;">
                  ${content}
                </td>
              </tr>
            </table>
          </td>
        </tr>"""
}

// Jira_Engine [Windows PowerShell 5.1+ — built-in on Exec_Agents]. All Configuration is read from the 'JIRA_*' & 'RESOLVED_*' Environment Variables.
@NonCPS
def getJiraEngineScript() {
    return '''$ErrorActionPreference = 'Stop'
try { [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12 } catch {}

$DQ        = [string][char]34
$BS        = [string][char]92
$NL        = [string][char]10
$CRLF      = [string][char]13 + [string][char]10
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Cfg {
    param([string]$Key, [string]$Default = '')
    $v = [Environment]::GetEnvironmentVariable($Key)
    if ($null -eq $v -or $v.Trim() -eq '') { return $Default }
    return $v.Trim()
}

function Split-Csv {
    param([string]$Raw)
    if (-not $Raw) { return @() }
    return @($Raw.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}

function Resolve-Dir {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) { return [System.IO.Path]::GetFullPath($Path) }
    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location).Path $Path))
}

function Save-Json {
    param([string]$Path, $Data)
    $json = ConvertTo-Json -InputObject $Data -Depth 10
    [System.IO.File]::WriteAllText($Path, $json, $Utf8NoBom)
}

# ---- Configuration [from Jenkins 'environment' Section] ----
$jiraUrl       = (Cfg 'JIRA_URL').TrimEnd('/')
$projectKey    = Cfg 'JIRA_PROJECT_KEY' 'COEX'
$issueType     = Cfg 'JIRA_ISSUE_TYPE' 'Bug'
$isDryRun      = ((Cfg 'JIRA_DRY_RUN' 'false').ToLower() -eq 'true')
$resultsDir    = Resolve-Dir (Cfg 'JIRA_RESULTS_DIR' 'Execution_Reports')
$outDir        = Resolve-Dir (Cfg 'JIRA_OUTPUT_DIR' 'Jira_Reports')
$packageVer    = Cfg 'JIRA_RESOLVED_VERSION' 'NA'
$summaryFormat = Cfg 'JIRA_SUMMARY_FORMAT' 'CI {version} {page} failed'
$appUrl        = Cfg 'JIRA_APP_URL'
$appLogin      = Cfg 'JIRA_APP_LOGIN'
$serverIp      = Cfg 'JIRA_TARGET_SERVER_IP'
$commonLabels  = @(Split-Csv (Cfg 'JIRA_COMMON_LABELS') | ForEach-Object { $_.Replace(' ', '_') })
$stopperKeys   = @(Split-Csv (Cfg 'JIRA_SHOWSTOPPER_KEYS') | ForEach-Object { $_.ToLower() })

$execEnv       = Cfg 'RESOLVED_ENV' 'NA'
$execBranch    = Cfg 'RESOLVED_BRANCH' 'NA'
$execSuite     = Cfg 'RESOLVED_SUITE' 'NA'
$execBrowser   = Cfg 'RESOLVED_BROWSER' 'NA'
$execAgent     = Cfg 'RESOLVED_AGENT' 'NA'
$jobName       = Cfg 'JOB_NAME' 'NA'
$buildNumber   = Cfg 'BUILD_NUMBER' '0'
$buildUrl      = Cfg 'BUILD_URL'

$jiraUser      = Cfg 'JIRA_USER'
$jiraToken     = Cfg 'JIRA_TOKEN'
$authHeader    = $null
if ($jiraUser -and $jiraToken) {
    $authHeader = 'Basic ' + [Convert]::ToBase64String($Utf8NoBom.GetBytes($jiraUser + ':' + $jiraToken))
}

New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$summaryPath = Join-Path $outDir 'test_summary.json'
$ticketsPath = Join-Path $outDir 'jira_summary.json'
$errorPath   = Join-Path $outDir 'jira_engine_error.txt'
if (Test-Path -LiteralPath $errorPath)   { Remove-Item -LiteralPath $errorPath -Force }
if (Test-Path -LiteralPath $summaryPath) { Remove-Item -LiteralPath $summaryPath -Force }
Save-Json $ticketsPath @()

$rows = New-Object System.Collections.ArrayList

function Save-Rows {
    $out = @($rows | ForEach-Object {
        [ordered]@{
            test          = $_.test
            title         = $_.title
            component     = $_.component
            owner         = $_.owner
            ticket        = $_.ticket
            ticket_url    = $_.ticket_url
            action        = $_.action
            issue_summary = $_.issue_summary
            priority      = $_.priority
        }
    })
    Save-Json $ticketsPath $out
}

# ---- Module / Owner Mapping [JIRA_OWNER_MAP_n & JIRA_OWNER_DEFAULT] ----
function Parse-Owner {
    param([string]$Raw)
    $parts = @($Raw.Split('|') | ForEach-Object { $_.Trim() })
    while ($parts.Count -lt 5) { $parts += '' }
    $comp = if ($parts[0]) { $parts[0] } else { 'Automation Suite' }
    $name = if ($parts[2]) { $parts[2] } else { 'Unassigned' }
    return @{
        component = $comp
        keywords  = @(Split-Csv $parts[1] | Where-Object { $_ -ne '*' } | ForEach-Object { $_.ToLower() })
        name      = $name
        email     = $parts[3]
        labels    = @(Split-Csv $parts[4] | ForEach-Object { $_.Replace(' ', '_') })
    }
}

$MAPPING_RULES = @()
$ownerVars = @(Get-ChildItem Env: | Where-Object { $_.Name -like 'JIRA_OWNER_MAP_*' -and $_.Value -and $_.Value.Trim() } |
               Sort-Object { $d = ($_.Name -replace '[^0-9]', ''); if ($d) { [int]$d } else { 0 } })
foreach ($ov in $ownerVars) { $MAPPING_RULES += (Parse-Owner $ov.Value) }
$DEFAULT_MAPPING = Parse-Owner (Cfg 'JIRA_OWNER_DEFAULT' 'Automation Suite | * | Unassigned |  | QA-Automation')

$PAGE_NAMES = @{
    'validate_workspace_page'         = 'Workspace Page'
    'validate_accesscontrol_page'     = 'Access Control Page'
    'validateothermethods'            = 'Other Methods'
    'validate_adminconsole_page'      = 'Admin Console Page'
    'validate_sessionmonitoring_page' = 'Session Monitoring Page'
    'validate_identityhub_page'       = 'Identity Hub Page'
}

function Resolve-Mapping {
    param([string]$TestName)
    $t = $TestName.ToLower()
    foreach ($rule in $MAPPING_RULES) {
        foreach ($kw in $rule.keywords) { if ($kw -and $t.Contains($kw)) { return $rule } }
    }
    return $DEFAULT_MAPPING
}

function Get-PageName {
    param([string]$TestName)
    $k = $TestName.ToLower().Trim()
    if ($PAGE_NAMES.ContainsKey($k)) { return $PAGE_NAMES[$k] }
    $c = ($TestName -replace '(?i)^(validate_?|test_?)', '').Replace('_', ' ').Trim()
    if (-not $c) { return $TestName }
    return (Get-Culture).TextInfo.ToTitleCase($c.ToLower())
}

function Get-RealIssue {
    param([string]$Msg)
    if (-not $Msg) { return 'Test execution failed without error message' }
    $tm      = [regex]::Match($Msg, 'Timeout [0-9]+ms exceeded')
    $tPrefix = if ($tm.Success) { $tm.Value } else { 'Timeout exceeded' }
    $marker  = 'waiting for locator('
    $i = $Msg.IndexOf($marker)
    if ($i -ge 0) {
        $part = $Msg.Substring($i + $marker.Length)
        $j    = $part.IndexOf(')')
        $loc  = if ($j -ge 0) { $part.Substring(0, $j) } else { $part }
        $loc  = ($loc.Split([char[]]@([char]32, [char]9, [char]10, [char]13), [StringSplitOptions]::RemoveEmptyEntries)) -join ' '
        if ($loc.Length -gt 100) { $loc = $loc.Substring(0, 97) + '...' }
        return $tPrefix + ' waiting for locator: ' + $loc
    }
    if ($tm.Success -or $Msg.Contains('TimeoutError')) { return $tPrefix + ' during element interaction' }
    $c = $Msg.Trim()
    foreach ($p in @('java.lang.AssertionError:', 'java.lang.Exception:', 'AssertionError:')) {
        $k = $c.IndexOf($p)
        if ($k -ge 0) { $c = $c.Substring($k + $p.Length).Trim() }
    }
    $first = (@($c -split "`r?`n"))[0].Trim()
    if (-not $first) { $first = $Msg.Trim() }
    if ($first.Length -gt 140) { $first = $first.Substring(0, 137) + '...' }
    return $first
}

function Find-Png {
    param($Node, [string]$BaseDir)
    if ($null -eq $Node) { return $null }
    foreach ($a in @($Node.attachments)) {
        if ($null -eq $a) { continue }
        $src = [string]$a.source
        if ($src -and $src.ToLower().EndsWith('.png')) {
            $cand = Join-Path $BaseDir $src
            if (Test-Path -LiteralPath $cand) { return $cand }
        }
    }
    foreach ($s in @($Node.steps)) {
        if ($null -eq $s) { continue }
        $f = Find-Png $s $BaseDir
        if ($f) { return $f }
    }
    return $null
}

function Build-Summary {
    param([string]$Page, [string]$TestName)
    $suiteName = [System.IO.Path]::GetFileName($execSuite)
    return $summaryFormat.Replace('{version}', $packageVer).Replace('{page}', $Page).Replace('{env}', $execEnv).Replace('{suite}', $suiteName).Replace('{test}', $TestName)
}

# ---- Jira API Helpers ----
function Invoke-Jira {
    param([string]$Method, [string]$Endpoint, $Body = $null, [hashtable]$ExtraHeaders = $null, [string]$ContentType = 'application/json; charset=utf-8')
    $headers = @{ 'Accept' = 'application/json' }
    if ($authHeader) { $headers['Authorization'] = $authHeader }
    if ($ExtraHeaders) { foreach ($hk in $ExtraHeaders.Keys) { $headers[$hk] = $ExtraHeaders[$hk] } }
    $req = @{ Uri = ($jiraUrl + $Endpoint); Method = $Method; Headers = $headers; UseBasicParsing = $true; TimeoutSec = 60 }
    if ($null -ne $Body) {
        if ($Body -is [byte[]]) { $req['Body'] = $Body }
        else { $req['Body'] = $Utf8NoBom.GetBytes((ConvertTo-Json -InputObject $Body -Depth 10)) }
        $req['ContentType'] = $ContentType
    }
    try {
        $resp = Invoke-WebRequest @req
        $obj  = $null
        if ($resp.Content) { try { $obj = $resp.Content | ConvertFrom-Json } catch { $obj = $resp.Content } }
        return @{ status = [int]$resp.StatusCode; body = $obj }
    } catch {
        $err  = $_
        $code = 0
        $text = $err.Exception.Message
        if ($err.Exception.Response) {
            try { $code = [int]$err.Exception.Response.StatusCode } catch {}
            try { $sr = New-Object System.IO.StreamReader($err.Exception.Response.GetResponseStream()); $text = $sr.ReadToEnd() } catch {}
        }
        return @{ status = $code; body = $text }
    }
}

function Get-AccountId {
    param([string]$Email)
    if (-not $Email) { return $null }
    $r = Invoke-Jira 'GET' ('/rest/api/2/user/search?query=' + [uri]::EscapeDataString($Email))
    if ($r.status -eq 200 -and $r.body) {
        $first = @($r.body)[0]
        if ($first -and $first.accountId) { return [string]$first.accountId }
    }
    return $null
}

function Find-Existing {
    param([string]$Title)
    $safe = $Title.Replace($DQ, '')
    $jql  = 'project = ' + $DQ + $projectKey + $DQ + ' AND summary ~ ' + $DQ + $BS + $DQ + $safe + $BS + $DQ + $DQ + ' ORDER BY created DESC'
    $r = Invoke-Jira 'POST' '/rest/api/2/search/jql' @{ jql = $jql; maxResults = 5; fields = @('summary', 'status') }
    if ($r.status -eq 200 -and $r.body -and $r.body.issues) {
        foreach ($iss in @($r.body.issues)) {
            if (([string]$iss.fields.summary).Trim() -eq $Title.Trim()) { return $iss }
        }
    }
    return $null
}

function Add-Screenshot {
    param([string]$IssueKey, [string]$Path)
    if (-not $Path -or -not (Test-Path -LiteralPath $Path)) { return $false }
    $boundary = '----JenkinsBoundary' + [guid]::NewGuid().ToString('N')
    $fileName = [System.IO.Path]::GetFileName($Path)
    $head = '--' + $boundary + $CRLF + 'Content-Disposition: form-data; name=' + $DQ + 'file' + $DQ + '; filename=' + $DQ + $fileName + $DQ + $CRLF + 'Content-Type: image/png' + $CRLF + $CRLF
    $tail = $CRLF + '--' + $boundary + '--' + $CRLF
    $ms = New-Object System.IO.MemoryStream
    $hb = $Utf8NoBom.GetBytes($head);            $ms.Write($hb, 0, $hb.Length)
    $fb = [System.IO.File]::ReadAllBytes($Path); $ms.Write($fb, 0, $fb.Length)
    $tb = $Utf8NoBom.GetBytes($tail);            $ms.Write($tb, 0, $tb.Length)
    $r = Invoke-Jira 'POST' ('/rest/api/2/issue/' + $IssueKey + '/attachments') $ms.ToArray() @{ 'X-Atlassian-Token' = 'no-check' } ('multipart/form-data; boundary=' + $boundary)
    return ($r.status -eq 200 -or $r.status -eq 201)
}

try {
    # ==========================================================================
    # STEP-1 :: Parse Allure Results
    # ==========================================================================
    Write-Host ('=' * 75)
    Write-Host ('>>> [JIRA_ENGINE] STEP-1 :: Parsing Allure Results from: ' + $resultsDir)
    Write-Host ('=' * 75)

    $resultFiles = @()
    if (Test-Path -LiteralPath $resultsDir) {
        $resultFiles = @(Get-ChildItem -LiteralPath $resultsDir -Recurse -Filter '*-result.json' -File | Sort-Object FullName)
    }

    # Keep only the latest attempt per Test [handles TestNG Retries]
    $latest = @{}
    foreach ($f in $resultFiles) {
        try {
            $data = Get-Content -LiteralPath $f.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
        } catch {
            Write-Host ('  Warning reading ' + $f.FullName + ' :: ' + $_.Exception.Message)
            continue
        }
        $key = if ($data.historyId) { [string]$data.historyId } elseif ($data.fullName) { [string]$data.fullName } elseif ($data.name) { [string]$data.name } else { $f.FullName }
        $stop = if ($data.stop) { [int64]$data.stop } else { [int64]0 }
        if (-not $latest.ContainsKey($key) -or $stop -ge $latest[$key].stop) {
            $latest[$key] = @{ data = $data; file = $f; stop = $stop }
        }
    }

    $passedCount  = 0
    $skippedCount = 0
    $failures     = New-Object System.Collections.ArrayList

    foreach ($key in @($latest.Keys | Sort-Object)) {
        $entry  = $latest[$key]
        $d      = $entry.data
        $name   = if ($d.name) { [string]$d.name } elseif ($d.fullName) { [string]$d.fullName } else { $entry.file.Name }
        $status = ([string]$d.status).ToLower()

        if ($status -eq 'passed') {
            $passedCount++
        } elseif ($status -eq 'failed' -or $status -eq 'broken') {
            $msg   = [string]$d.statusDetails.message
            if (-not $msg) { $msg = 'No error message provided' }
            $trace = [string]$d.statusDetails.trace
            $traceTop = ''
            if ($trace) { $traceTop = (@($trace -split "`r?`n") | Select-Object -First 6) -join $NL }
            [void]$failures.Add(@{
                test_name  = $name
                error      = $msg.Trim()
                trace      = $traceTop
                screenshot = (Find-Png $d $entry.file.DirectoryName)
            })
        } elseif ($status -eq 'skipped') {
            $skippedCount++
        }
    }

    $totalCount = $passedCount + $failures.Count + $skippedCount
    Save-Json $summaryPath ([ordered]@{ total = $totalCount; passed = $passedCount; failed = $failures.Count; skipped = $skippedCount })

    Write-Host ('  Result Files Found   : ' + $resultFiles.Count)
    Write-Host ('  Total Tests Executed : ' + $totalCount)
    Write-Host ('  PASSED               : ' + $passedCount)
    Write-Host ('  FAILED / BROKEN      : ' + $failures.Count)
    Write-Host ('  SKIPPED              : ' + $skippedCount)

    if ($resultFiles.Count -eq 0) {
        Write-Host '  WARNING: No Allure *-result.json files found. No Jira Tickets will be raised.'
        exit 0
    }
    if ($failures.Count -eq 0) {
        Write-Host '>>> ALL TESTS PASSED! No Jira Tickets needed.'
        exit 0
    }

    # ==========================================================================
    # STEP-2 :: Prepare Ticket Rows [saved as PENDING before any Jira call]
    # ==========================================================================
    $hostName = ''
    if ($appUrl) { $hostName = (($appUrl -replace '^https?://', '').Split('/'))[0] }

    foreach ($ft in $failures) {
        $mapping   = Resolve-Mapping $ft.test_name
        $page      = Get-PageName $ft.test_name
        $realIssue = Get-RealIssue $ft.error
        $labels    = @(@($mapping.labels) + @($commonLabels) | Where-Object { $_ } | Select-Object -Unique)
        $priority  = 'High'
        foreach ($sk in $stopperKeys) { if ($sk -and $mapping.component.ToLower().Contains($sk)) { $priority = 'Show stopper' } }

        $steps = New-Object System.Collections.ArrayList
        [void]$steps.Add('Open browser and navigate to ' + $(if ($appUrl) { $appUrl } else { 'the application URL' }))
        if ($appLogin) { [void]$steps.Add('Login with credentials: ' + $appLogin) }
        [void]$steps.Add('Navigate to ' + $page)
        [void]$steps.Add('Perform page element validation')
        $stepsText = (@(for ($s = 0; $s -lt $steps.Count; $s++) { ([string]($s + 1)) + '. ' + $steps[$s] })) -join $NL

        $desc = New-Object System.Collections.ArrayList
        [void]$desc.Add('1. Environment Details')
        if ($appUrl)   { [void]$desc.Add('Application URL: ' + $appUrl) }
        if ($serverIp) { [void]$desc.Add('Server IP:       ' + $serverIp) }
        if ($appLogin) { [void]$desc.Add('Credentials:     ' + $appLogin) }
        if ($serverIp -and $hostName) { [void]$desc.Add('Host Entry:      ' + $serverIp + '  ' + $hostName) }
        foreach ($line in @(
            ('CI Version:      ' + $packageVer),
            ('Execution Env:   ' + $execEnv),
            ('Module / Page:   ' + $page),
            ('Test Name:       ' + $ft.test_name),
            ('Suite / Browser: ' + $execSuite + ' / ' + $execBrowser),
            ('Branch / Agent:  ' + $execBranch + ' / ' + $execAgent),
            ('Jenkins Build:   ' + $jobName + ' #' + $buildNumber),
            ('Build URL:       ' + $buildUrl),
            '',
            '---------------------------------------------------------------------------',
            '',
            '2. Issue Description',
            'Issue:',
            $realIssue,
            '',
            'Page / Location:',
            ($(if ($appUrl) { $appUrl } else { 'Application' }) + ' -> ' + $page),
            '',
            'Steps to Reproduce:',
            $stepsText,
            '',
            'Actual Result:',
            $ft.error
        )) { [void]$desc.Add($line) }
        if ($ft.trace) { [void]$desc.Add(''); [void]$desc.Add('Stack Trace (Top Frames):'); [void]$desc.Add($ft.trace) }

        [void]$rows.Add(@{
            test          = $ft.test_name
            title         = (Build-Summary $page $ft.test_name)
            component     = $mapping.component
            owner         = $mapping.name
            owner_email   = $mapping.email
            labels        = $labels
            ticket        = ''
            ticket_url    = ''
            action        = 'PENDING'
            issue_summary = ($mapping.component + ' | ' + $realIssue)
            priority      = $priority
            description   = ($desc -join $NL)
            error         = $ft.error
            screenshot    = $ft.screenshot
        })
    }
    Save-Rows

    # ==========================================================================
    # STEP-3 :: Create / Update Jira Ticket(s)
    # ==========================================================================
    Write-Host ''
    Write-Host ('=' * 75)
    Write-Host '>>> [JIRA_ENGINE] STEP-3 :: JIRA DEFECT CREATION & MAPPING'
    Write-Host ('>>> Jira URL        : ' + $jiraUrl)
    Write-Host ('>>> Project / Type  : ' + $projectKey + ' / ' + $issueType)
    Write-Host ('>>> CI Version      : ' + $packageVer + ' | Env: ' + $execEnv)
    Write-Host ('>>> Execution Mode  : ' + $(if ($isDryRun) { '[DRY RUN - SIMULATION ONLY]' } else { '[LIVE - CREATING REAL TICKETS]' }))
    Write-Host ('=' * 75)

    if ($isDryRun) {
        foreach ($row in $rows) {
            $row.ticket = 'DRY_RUN_SIMULATED'
            $row.action = 'DRY_RUN'
            Write-Host ('  [DRY RUN] ' + $row.title + '  ->  ' + $row.owner)
        }
        Save-Rows
        exit 0
    }

    if (-not $authHeader) { throw 'JIRA_USER / JIRA_TOKEN not available. Check the JIRA_CREDS_ID credential.' }
    $me = Invoke-Jira 'GET' '/rest/api/2/myself'
    if ($me.status -ne 200) { throw ('Jira authentication failed :: HTTP ' + $me.status + ' :: ' + ([string]$me.body).Substring(0, [Math]::Min(200, ([string]$me.body).Length))) }
    $callerId = [string]$me.body.accountId
    Write-Host ('>>> Authenticated to Jira as: ' + $me.body.displayName)

    $idx = 0
    foreach ($row in $rows) {
        $idx++
        Write-Host ''
        Write-Host '---------------------------------------------------------------------------'
        Write-Host ('[' + $idx + '/' + $rows.Count + '] Processing: ' + $row.test)
        Write-Host ('  Title          : ' + $row.title)
        Write-Host ('  Mapped Module  : ' + $row.component)
        Write-Host ('  Assigned Owner : ' + $row.owner + ' <' + $row.owner_email + '>')
        Write-Host ('  Labels         : ' + ($row.labels -join ', '))
        try {
            $existing = Find-Existing $row.title
            if ($existing) {
                $exKey = [string]$existing.key
                Write-Host ('  >>> EXISTING TICKET FOUND: ' + $exKey + '. Updating description & adding comment . . .')
                [void](Invoke-Jira 'PUT' ('/rest/api/2/issue/' + $exKey) @{ fields = @{ description = $row.description; labels = @($row.labels) } })
                $comment = '[Auto QA] Recurred in ' + $jobName + ' Build #' + $buildNumber + ' (' + $execEnv + ' / ' + $execBrowser + '). Error: ' + $row.error
                [void](Invoke-Jira 'POST' ('/rest/api/2/issue/' + $exKey + '/comment') @{ body = $comment })
                if ($row.screenshot) { [void](Add-Screenshot $exKey $row.screenshot) }
                $row.ticket     = $exKey
                $row.ticket_url = $jiraUrl + '/browse/' + $exKey
                $row.action     = 'UPDATED'
            } else {
                $fields = [ordered]@{
                    project     = @{ key = $projectKey }
                    summary     = $row.title
                    description = $row.description
                    issuetype   = @{ name = $issueType }
                    labels      = @($row.labels)
                }
                if ($callerId) { $fields['reporter'] = @{ id = $callerId } }
                $acc = Get-AccountId $row.owner_email
                if ($acc) { $fields['assignee'] = @{ id = $acc } }

                Write-Host ('  >>> Creating new Jira issue in project ' + $projectKey + ' . . .')
                $c = Invoke-Jira 'POST' '/rest/api/2/issue' @{ fields = $fields }
                if ($c.status -eq 400 -and $fields.Contains('reporter')) {
                    Write-Host '  >>> Retrying without reporter field . . .'
                    $fields.Remove('reporter')
                    $c = Invoke-Jira 'POST' '/rest/api/2/issue' @{ fields = $fields }
                }
                if (($c.status -eq 200 -or $c.status -eq 201) -and $c.body -and $c.body.key) {
                    $newKey = [string]$c.body.key
                    Write-Host ('  >>> SUCCESS: Created ticket ' + $newKey + ' -> ' + $jiraUrl + '/browse/' + $newKey)
                    if ($row.screenshot -and (Add-Screenshot $newKey $row.screenshot)) { Write-Host ('  >>> Attached screenshot: ' + [System.IO.Path]::GetFileName($row.screenshot)) }
                    $row.ticket     = $newKey
                    $row.ticket_url = $jiraUrl + '/browse/' + $newKey
                    $row.action     = 'CREATED'
                } else {
                    $bodyText = [string]$c.body
                    Write-Host ('  >>> ERROR creating ticket: HTTP ' + $c.status + ' - ' + $bodyText.Substring(0, [Math]::Min(500, $bodyText.Length)))
                    $row.action   = 'FAILED'
                    $row.priority = 'High'
                }
            }
        } catch {
            Write-Host ('  >>> ERROR processing ticket :: ' + $_.Exception.Message)
            $row.action = 'FAILED'
        }
        Save-Rows
    }

    Write-Host ''
    Write-Host ('=' * 75)
    Write-Host '                   JIRA TICKETING SUMMARY TABLE'
    Write-Host ('=' * 75)
    foreach ($row in $rows) {
        Write-Host ('  {0,-10} | {1,-18} | {2,-26} | {3}' -f $row.action, $row.ticket, $row.component, $row.owner)
    }
    Write-Host ('=' * 75)
    Write-Host ('>>> Saved Jira summary to: ' + $ticketsPath)
    exit 0
} catch {
    $msg = $_.Exception.Message
    Write-Host ('[JIRA_ENGINE] ERROR :: ' + $msg)
    try { [System.IO.File]::WriteAllText($errorPath, ($msg + $NL + ($_ | Out-String)), $Utf8NoBom) } catch {}
    try { Save-Rows } catch {}
    exit 1
}
'''
}