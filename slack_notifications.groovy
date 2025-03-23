// Function to build a basic Slack message with commit and pipeline info
def constructSlackMessage(buildNumber, buildUrl) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()  // Retrieve latest commit ID from Git
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()  // Get last commit message
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()  // Get current branch name
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"  // Create GitHub link to the commit
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"  // Format Jenkins build duration
        def jenkinsUrl = buildUrl.split('/job/')[0]  // Extract base Jenkins URL from full pipeline URL
        def publicIp = jenkinsUrl.replace("http://", "").replace(":8080", "")  // Extract IP from Jenkins URL
        def appUrl = "http://${publicIp}:5000"  // Construct URL to the Flask app

        // Return formatted Slack message with Markdown links
        return """
        Jenkins Build Completed!
        Pipeline: #${buildNumber}
        Branch: ${branch}
        Commit: [${commitId}](${commitUrl})
        Message: ${commitMessage}
        Duration: ${duration}
        Pipeline Link: [View Pipeline](${buildUrl})
        Application Link: [Open Flask App](${appUrl})
        """
    } catch (Exception e) {
        echo "Failed to construct Slack message: ${e.message}"  // Log any error during message construction
        return "Error generating Slack message."  // Fallback message on error
    }
}


// Constructs a Slack message with merge/deploy status

def constructSlackResultMessage(buildNumber, buildUrl, mergeSuccess, deploySuccess) {
    def mergeStatus = mergeSuccess ? "✅ Merge succeeded." : "❌ Merge failed."
    def deployStatus = deploySuccess ? "✅ Deploy succeeded." : (mergeSuccess ? "❌ Deploy failed after merge." : "⏭️ Deploy skipped due to merge failure.")
    def baseMessage = constructSlackMessage(buildNumber, buildUrl)  // Reuse existing info message
    return "${baseMessage}\n${mergeStatus}\n${deployStatus}"  // Append result statuses
}

// Sends a Slack notification

def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Target Slack channel
            tokenCredentialId: 'Jenkins-Slack-Token',  // Slack API credential ID
            message: message,  // Message body
            color: color  // Message color: good, warning, danger
        )
    } catch (Exception e) {
        echo "ERROR: Slack notification failed: ${e.message}"  // Log Slack failure
    }
}

return this  // Return this object to be used in Jenkinsfile

