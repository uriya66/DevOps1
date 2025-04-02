def constructSlackMessage(buildNumber, buildUrl, mergeSuccess = null, deploySuccess = null) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
        def branch = "feature-${buildNumber}"  // Use consistent branch naming
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()
        def appUrl = "http://${publicIp}:5000"

        def resultNote = ""
        if (mergeSuccess != null && deploySuccess != null) {
            resultNote += mergeSuccess ? "*Merge:* ✅ Successful - merged to main on GitHub\n" : "*Merge:* ❌ Failed - merge blocked or error occurred\n"
            resultNote += deploySuccess ? "*Deploy:* ✅ Successful - Flask app deployed on server\n" : "*Deploy:* ❌ Failed - deployment script error\n"
        }

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
        echo "Slack message error: ${e.message}"
        return "*❌ Error constructing Slack message*\nReason: ${e.message}"
    }
}

def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',
            tokenCredentialId: 'Jenkins-Slack-Token',
            message: message,
            color: color
        )
    } catch (Exception e) {
        echo "Slack notification failed: ${e.message}"
    }
}

return this

