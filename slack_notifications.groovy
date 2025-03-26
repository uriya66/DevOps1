// Construct a Slack message with Git info and app links
def constructSlackMessage(buildNumber, buildUrl) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()  // Get last commit
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()  // Get message
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()  // Get branch
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"  // GitHub link
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"  // Build time
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()  // IP
        def appUrl = "http://${publicIp}:5000"  // Public app link

        // Build Slack message (without localhost)
        return """
        *Build:* #${buildNumber}
        *Branch:* ${branch}
        *Commit:* [${commitId}](${commitUrl})
        *Message:* ${commitMessage}
        *Duration:* ${duration}
        *App URL:* ${appUrl}
        *Pipeline:* ${buildUrl}
        """
    } catch (Exception e) {
        echo "Slack message error: ${e.message}"  // Log error
        return "Error generating Slack message"  // Fallback
    }
}

// Send Slack message to channel
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Slack channel
            tokenCredentialId: 'Jenkins-Slack-Token',  // Token
            message: message,  // Message text
            color: color  // Color (good/warning/danger)
        )
    } catch (Exception e) {
        echo "Slack notification failed: ${e.message}"  // Log failure
    }
}

return this  // Allow loading as module

