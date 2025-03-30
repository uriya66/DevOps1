// Construct Slack message with Git commit info and deployment status
def constructSlackMessage(buildNumber, buildUrl, mergeSuccess = null, deploySuccess = null) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
        def branch = env.GIT_BRANCH ?: sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()
        def appUrl = "http://${publicIp}:5000"

        def message = """
:white_check_mark: Jenkins Build Completed!
*Pipeline:* #${buildNumber}
*Branch:* ${branch}
*Commit:* [${commitId}](${commitUrl})
*Message:* ${commitMessage}
*Duration:* ${duration}
"""

        if (mergeSuccess != null && deploySuccess != null) {
            message += mergeSuccess ? "*Merge:* ✅ Successful\n" : "*Merge:* ❌ Failed\n"
            message += deploySuccess ? "*Deploy:* ✅ Successful\n" : "*Deploy:* ❌ Failed\n"
        }

        message += "*Pipeline Link:* ${buildUrl}\n"
        message += "*Application Link:* ${appUrl}"

        return message

    } catch (Exception e) {
        echo "Slack message generation error: ${e.message}"
        return "*❌ Error constructing Slack message*\nReason: ${e.message}"
    }
}

// Send Slack notification to specific channel
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

