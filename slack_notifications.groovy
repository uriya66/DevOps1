// Retrieves Git commit details and constructs a formatted Slack message
def constructSlackMessage(buildNumber, buildUrl) {
    try {
        // Retrieve commit ID
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
        // Retrieve commit message
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
        // Retrieve the current branch name
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
        // Generate GitHub commit URL
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"
        // Retrieve build duration
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"

        // Construct the Slack message
        return """
        ✅ *Jenkins Build Completed!*
        *Pipeline:* #${buildNumber}
        *Branch:* ${branch}
        *Commit:* [${commitId}](${commitUrl})
        *Message:* ${commitMessage}
        *Duration:* ${duration}
        *Pipeline Link:* [View Pipeline](${buildUrl})
        """
    } catch (Exception e) {
        echo "⚠️ Failed to construct Slack message: ${e.message}"
        return "⚠️ Error generating Slack message."
    }
}

// Sends a Slack notification with the given message and color
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkins-alerts',
            tokenCredentialId: 'Jenkins-Slack-Token',
            message: message,
            color: color
        )
    } catch (Exception e) {
        echo "⚠️ Slack notification failed: ${e.message}"
    }
}

// Return this script so Jenkinsfile can use it
return this
