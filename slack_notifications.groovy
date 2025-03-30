// Construct a Slack message with Git info and app links
def constructSlackMessage(buildNumber, buildUrl, mergeSuccess = null, deploySuccess = null) {
    try {
        // Retrieve commit ID from Git
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()

        // Retrieve commit message from latest commit
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()

        // Retrieve the custom branch name from env
        def branch = env.GIT_BRANCH ?: "unknown-branch"  // Use fallback if not set

        // Generate GitHub commit URL for direct reference
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"

        // Get pipeline duration in readable format
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"

        // Extract Jenkins base URL from full build URL
        def jenkinsUrl = buildUrl.split('/job/')[0]

        // Get dynamic public IP of the server
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()

        // Construct Flask app URL
        def appUrl = "http://${publicIp}:5000"

        // Build result summary (merge/deploy)
        def resultNote = ""
        if (mergeSuccess != null && deploySuccess != null) {
            resultNote += mergeSuccess ? "*Merge:* ✅ Successful\n" : "*Merge:* ❌ Failed - Merge did not complete successfully\n"
            resultNote += deploySuccess ? "*Deploy:* ✅ Successful\n" : "*Deploy:* ❌ Failed - Deployment script encountered an issue\n"
        }

        // Build full Slack message
        return """
*✅ Jenkins Build Completed!*
*Pipeline:* #${buildNumber}
*Branch:* ${branch}
*Commit:* [${commitId}](${commitUrl})
*Message:* ${commitMessage}
*Duration:* ${duration}
${resultNote}
*Pipeline Link:* ${buildUrl}
*Application Links:*
- ${appUrl}
        """.stripIndent()

    } catch (Exception e) {
        echo "Slack message error: ${e.message}"  // Log error if message construction fails
        return "*❌ Error constructing Slack message*\nReason: ${e.message}"  // Return fallback message
    }
}

// Send a message to Slack channel using Jenkins credentials
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Slack channel name
            tokenCredentialId: 'Jenkins-Slack-Token',  // Credential ID for Slack bot
            message: message,  // Slack message content
            color: color  // Slack color code: good, warning, danger
        )
    } catch (Exception e) {
        echo "Slack notification failed: ${e.message}"  // Log if Slack sending fails
    }
}

return this  // Return this script so it can be used in Jenkinsfile

