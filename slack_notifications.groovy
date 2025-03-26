// Construct a Slack message with Git info and app links
def constructSlackMessage(buildNumber, buildUrl) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()  // Get last commit ID
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()  // Get commit message
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()  // Get current branch
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"  // Create commit URL
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"  // Build duration
        def jenkinsUrl = buildUrl.split('/job/')[0]  // Jenkins root URL
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()  // Get current dynamic IP
        def appUrl1 = "http://${publicIp}:5000"  // External app URL
        def appUrl2 = "http://localhost:5000"  // Localhost URL

        // Build full Slack message
        return """
        *Jenkins Pipeline Result*
        *Build:* #${buildNumber}
        *Branch:* ${branch}
        *Commit:* [${commitId}](${commitUrl})
        *Message:* ${commitMessage}
        *Duration:* ${duration}
        *App Links:*
        - ${appUrl1}
        - ${appUrl2}
        *Pipeline:* ${buildUrl}
        """
    } catch (Exception e) {
        echo "Slack message error: ${e.message}"  // Print error if message build fails
        return "Error generating Slack message"  // Return fallback message
    }
}

// Send a message to Slack channel using Jenkins credentials
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Slack channel name
            tokenCredentialId: 'Jenkins-Slack-Token',  // Credential ID for Slack bot
            message: message,  // Slack message body
            color: color  // Slack color: good, warning, danger
        )
    } catch (Exception e) {
        echo "Slack notification failed: ${e.message}"  // Print if Slack post fails
    }
}

return this  // Make functions available to Jenkinsfile

