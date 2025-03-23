// Retrieves Git commit details and constructs a formatted Slack message
// Function to construct Slack message with Git details, including merge/deploy status if available
def constructSlackMessage(buildNumber, buildUrl, Boolean mergeSuccess = null, Boolean deploySuccess = null) {
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
        def jenkinsUrl = buildUrl.split('/job/')[0]  // Get base Jenkins URL

        // Extract public IP from Jenkins base URL (assumes format: http://<ip>:8080)
        def publicIp = jenkinsUrl.replace("http://", "").replace(":8080", "")  // Get public IP

        // Generate application link using extracted IP and Flask port
        def appUrl = "http://${publicIp}:5000"  // Target Flask app link

        // Define extra status section (optional)
        def extraStatus = ""  // Default empty section
        if (mergeSuccess != null && deploySuccess != null) {
            // Add merge result
            extraStatus += mergeSuccess ? "\n✅ Merge succeeded." : "\n❌ Merge failed."
            // Add deploy result
            if (!mergeSuccess) {
                extraStatus += "\n⏭️ Deploy skipped due to merge failure."
            } else {
                extraStatus += deploySuccess ? "\n✅ Deploy succeeded." : "\n❌ Deploy failed."
            }
        }

        // Construct a rich Slack message using Markdown formatting
        return """
         Jenkins Build Completed!
        *Pipeline:* #${buildNumber}
        *Branch:* ${branch}
        *Commit:* [${commitId}](${commitUrl})
        *Message:* ${commitMessage}
        *Duration:* ${duration}
        *Pipeline Link:* [View Pipeline](${buildUrl})
        *Application Link:* [Open Flask App](${appUrl})${extraStatus}
        """
    } catch (Exception e) {
        echo "Failed to construct Slack message: ${e.message}"  // Log message error
        return "Error generating Slack message."  // Return fallback message
    }
}

// Sends a Slack notification with the given message and color indicator
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Slack channel to send the message to
            tokenCredentialId: 'Jenkins-Slack-Token',  // Slack API token from Jenkins credentials
            message: message,  // Message body to send
            color: color  // Color bar on Slack (good/warning/danger)
        )
    } catch (Exception e) {
        echo "ERROR: Slack notification failed: ${e.message}"  // Log Slack error
    }
}

// Return this script object so it can be loaded from Jenkinsfile
return this
