// Construct and return a formatted Slack message with dynamic app links
def constructSlackMessage(buildNumber, buildUrl) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()  // Get latest commit hash
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()  // Get commit message
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()  // Get branch name
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"  // GitHub commit link
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"  // Build duration
        def jenkinsUrl = buildUrl.split('/job/')[0]  // Extract Jenkins base URL
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()  // Get public IP
        def appUrl1 = "http://${publicIp}:5000"  // Public link
        def appUrl2 = "http://localhost:5000"  // Localhost link

        return """
        *Jenkins Pipeline Result*
        *Build:* #${buildNumber}
        *Branch:* ${branch}
        *Commit:* [${commitId}](${commitUrl})
        *Message:* ${commitMessage}
        *Duration:* ${duration}
        *App URLs:*
        - ${appUrl1}
        - ${appUrl2}
        *Pipeline:* ${buildUrl}
        """
    } catch (Exception e) {
        echo "Slack message error: ${e.message}"  // Handle errors gracefully
        return "Error generating Slack message"
    }
}

// Send a Slack message with specified color
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Slack channel
            tokenCredentialId: 'Jenkins-Slack-Token',  // Slack bot token
            message: message,
            color: color
        )
    } catch (Exception e) {
        echo "Slack notification failed: ${e.message}"  // Log Slack failure
    }
}

return this  // Make functions available to Jenkinsfile

