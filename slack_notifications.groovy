// Construct Slack notification message
def constructSlackMessage(buildNumber, buildUrl, mergeSuccess = null, deploySuccess = null) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
        def branch = env.GIT_BRANCH ?: sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"
        def duration = currentBuild.durationString.replace(' and counting', '')
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()
        def appUrl = "http://${publicIp}:5000"

        def resultNote = ""
        if (mergeSuccess != null && deploySuccess != null) {
            resultNote += mergeSuccess ? "*Merge:* ✅ Successful\n" : "*Merge:* ❌ Failed\n"
            resultNote += deploySuccess ? "*Deploy:* ✅ Successful\n" : "*Deploy:* ❌ Failed\n"
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

// Send Slack notification
def sendSlackNotification(String message, String color) {
    slackSend(
        channel: '#jenkis_alerts',
        tokenCredentialId: 'Jenkins-Slack-Token',
        message: message,
        color: color
    )
}

return this

