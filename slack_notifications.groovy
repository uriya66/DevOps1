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

        // Retrieve the public IP address from the Jenkins environment
        def jenkinsUrl = buildUrl.split('/job/')[0] // Extract the Jenkins address from the existing message
        def publicIp = jenkinsUrl.replace("http://", "").replace(":8080", "") // Extract the IP from the Jenkins URL
        def appUrl = "http://${publicIp}:5000" // Flask address
        
        // Construct the Slack message
        return """
        ✅ *Jenkins Build Completed!*
        *Pipeline:* #${buildNumber}
        *Branch:* ${branch}
        *Commit:* [${commitId}](${commitUrl})
        *Message:* ${commitMessage}
        *Duration:* ${duration}
        *Pipeline Link:* [View Pipeline](${buildUrl})
        *Application Link:* [Open Flask App](${appUrl})
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
            channel: 'C08J6TVUX5E',
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
