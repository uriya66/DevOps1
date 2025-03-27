// Construct Slack message with Git commit, app info, and result
def constructSlackMessage(buildNumber, buildUrl, mergeSuccess = null, deploySuccess = null) {
    try {
        // Get current commit ID
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()

        // Get commit message
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()

        // Get branch name
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()

        // Create GitHub commit URL
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"

        // Get build duration
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"

        // Extract base Jenkins URL
        def jenkinsUrl = buildUrl.split('/job/')[0]

        // Get current public IP
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()

        // Build app URL
        def appUrl = "http://${publicIp}:5000"

        // Optional merge/deploy status
        def resultNote = ""
        if (mergeSuccess != null && deploySuccess != null) {
            resultNote += mergeSuccess ? "*Merge:* Successful\n" : "*Merge:* Failed\n"
            resultNote += deploySuccess ? "*Deploy:* Successful\n" : "*Deploy:* Failed\n"
        }

        // Compose full Slack message
        return """
*Jenkins Build Completed!*
*Pipeline:* #${buildNumber}
*Branch:* ${branch}
*Commit:* [${commitId}](${commitUrl})
*Message:* ${commitMessage}
*Duration:* ${duration}
${resultNote}
*Pipeline Link:* ${buildUrl}
*Application Link:* ${appUrl}
        """.stripIndent()

    } catch (Exception e) {
        echo "Slack message error: ${e.message}"  // Log Slack error
        return "*Error constructing Slack message*\nReason: ${e.message}"  // Fallback message
    }
}

// Send message to Slack using Jenkins token
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Slack channel
            tokenCredentialId: 'Jenkins-Slack-Token',  // Slack bot token
            message: message,  // Message content
            color: color  // Slack message color (good/danger)
        )
    } catch (Exception e) {
        echo "Slack notification failed: ${e.message}"  // Log send failure
    }
}

// Return object for Jenkinsfile usage
return this

