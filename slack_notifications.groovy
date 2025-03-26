// Construct a Slack message based on Git data and environment
def constructSlackMessage(buildNumber, buildUrl) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim() // Get latest commit ID
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim() // Get commit message
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim() // Get current branch name
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}" // GitHub commit link
        def duration = "${currentBuild.durationString.replace(' and counting', '')}" // Get build duration

        def jenkinsUrl = buildUrl.split('/job/')[0] // Extract Jenkins base URL
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim() // Get dynamic IP
        def appUrl1 = "http://${publicIp}:5000" // Public access URL
        def appUrl2 = "http://localhost:5000"   // Localhost access URL

        // Return formatted Slack message
        return """
        *Jenkins Build Completed!*
        *Pipeline:* #${buildNumber}
        *Branch:* ${branch}
        *Commit:* [${commitId}](${commitUrl})
        *Message:* ${commitMessage}
        *Duration:* ${duration}
        *Pipeline Link:* ${buildUrl}
        *Application Links:* 
        - ${appUrl1}
        - ${appUrl2}
        """
    } catch (Exception e) {
        echo "Slack message error: ${e.message}" // Log error
        return "Error constructing Slack message"
    }
}

// Send formatted Slack notification
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Slack channel
            tokenCredentialId: 'Jenkins-Slack-Token',  // Credentials for Slack bot
            message: message,
            color: color
        )
    } catch (Exception e) {
        echo "Slack notification failed: ${e.message}" // Log if Slack fails
    }
}

return this // Export for Jenkinsfile use

