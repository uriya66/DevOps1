// Build Slack message from Jenkins data and Git
def constructSlackMessage(buildNumber, buildUrl, mergeSuccess = null, deploySuccess = null) {
    try {
        // Retrieve commit ID from Git
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()

        // Retrieve commit message from latest commit
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()

        // Retrieve the current branch name
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()

        // Generate GitHub commit URL for direct reference
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"

        // Get pipeline duration in readable format
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"

        // Get dynamic public IP of the server
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()

        // Construct Flask app URL
        def appUrl = "http://${publicIp}:5000"

        // Build result summary (merge/deploy)
        def resultNote = ""
        if (mergeSuccess != null && deploySuccess != null) {
            resultNote += mergeSuccess ? "*Merge:* ✅ Successful - merged to main\n" : "*Merge:* ❌ Failed - merge error occurred\n"
            resultNote += deploySuccess ? "*Deploy:* ✅ Successful - app deployed to server\n" : "*Deploy:* ❌ Failed - deployment script error\n"
        }

        // Build full Slack message with clean format (no localhost
        return """
*✅ Jenkins Build Completed!*
*Pipeline:* #${buildNumber}
*Branch:* feature-${buildNumber}
*Commit:* [${commitId}](${commitUrl})
*Message:* ${commitMessage}
*Duration:* ${duration}
${resultNote}
*Pipeline Link:* ${buildUrl}
*Application Links:*
- ${appUrl}
        """.stripIndent()

    } catch (e) {
        echo "Slack message error: ${e.message}"  // Log error if message construction fails
        return "*❌ Error building Slack message*\nReason: ${e.message}" // Return fallback message
    }
}

// Send Slack alert via token and color code
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Slack channel name
            tokenCredentialId: 'Jenkins-Slack-Token',  // Credential ID for Slack bot
            message: message,  // Slack message content
            color: color  // Slack color code: good, warning, danger
        )
    } catch (e) {
        echo "Slack send failed: ${e.message}"  // Log if Slack sending fails
    }
}

// Return this script object so it can be load to Jenkinsfile
return this  // Return this object to Jenkinsfil
