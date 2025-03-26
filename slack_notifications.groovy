// Construct a Slack message with Git info and app links
def constructSlackMessage(buildNumber, buildUrl) {
    try {
	// Retrieve commit ID from Git
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()

        // Retrieve commit message from latest commit
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()

        // Retrieve the current branch name
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()

        // Generate GitHub commit URL for direct reference
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"  

        // Get pipeline duration in readable format
        def duration = "${currentBuild.durationString.replace(' and counting', '')}" 

        // Extract Jenkins base URL from full build URL
        def jenkinsUrl = buildUrl.split('/job/')[0]  

        // Extract public IP from Jenkins base URL (assumes format: http://<ip>:8080)
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()  

        // Generate application link using extracted IP and Flask port
        def appUrl = "http://${publicIp}:5000"  // Target Flask app link


        // Build full Slack message
        return """
        *✅ Jenkins Build Completed, And the Result:*
        *Build:* #${buildNumber}
        *Branch:* ${branch}
        *Commit:* [${commitId}](${commitUrl})
        *Message:* ${commitMessage}
        *Duration:* ${duration}
        *App Links:*
        - ${appUrl}
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

// Return this script object so it can be loaded from Jenkinsfile
return this
