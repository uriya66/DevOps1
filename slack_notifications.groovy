// Build Slack message using Git commit and build info
def constructSlackMessage(buildNumber, buildUrl) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"

        def jenkinsUrl = buildUrl.split('/job/')[0]
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()
        def appUrl = "http://${publicIp}:5000"  // Flask app address using dynamic IP

        return """
        Jenkins Build Completed
        Pipeline: #${buildNumber}
        Branch: ${branch}
        Commit: [${commitId}](${commitUrl})
        Message: ${commitMessage}
        Duration: ${duration}
        Pipeline Link: [View Pipeline](${buildUrl})
        Application Link: [Open Flask App](${appUrl})
        """
    } catch (Exception e) {
        return "Error generating Slack message: ${e.message}"
    }
}

// Send formatted Slack message
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

