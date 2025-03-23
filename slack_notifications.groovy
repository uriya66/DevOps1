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

// Function to build Slack message indicating merge and deployment status
def constructSlackResultMessage(buildNumber, buildUrl, mergeSuccess, deploySuccess) {
    try {
        def branch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()  // Get current branch name
        def jenkinsUrl = buildUrl.split('/job/')[0]  // Extract base Jenkins URL
        def publicIp = jenkinsUrl.replace("http://", "").replace(":8080", "")  // Extract public IP
        def appUrl = "http://${publicIp}:5000"  // Construct Flask app URL

        // Determine human-readable status for merge
        def mergeStatus = mergeSuccess ? "Merge to main completed successfully." : "Merge to main failed."

        // Determine human-readable status for deploy
        def deployStatus = deploySuccess ? "Deployment completed successfully. Application is live." : "Deployment failed. Please check the Jenkins logs."

        // Return formatted Slack message for post-merge/deploy status
        return """
        Jenkins Post Actions Summary
        Pipeline: #${buildNumber}
        Branch: ${branch}
        ${mergeStatus}
        ${deployStatus}
        Pipeline Link: [View Pipeline](${buildUrl})
        Application Link: [Open Flask App](${appUrl})
        """
    } catch (Exception e) {
        echo "Failed to construct Slack result message: ${e.message}"  // Log error during message creation
        return "Error generating result Slack message."  // Fallback message
    }
}

// Function to send a Slack message using Jenkins credentials
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Slack channel to send the message to
            tokenCredentialId: 'Jenkins-Slack-Token',  // Credential ID stored in Jenkins for Slack token
            message: message,  // The actual Slack message content
            color: color  // Color indicator for the Slack message (good, danger, warning)
        )
    } catch (Exception e) {
        echo "ERROR: Slack notification failed: ${e.message}"  // Log error if Slack send fails
    }
}

// Return the functions in this script for use in Jenkinsfile
return this  // Allow loading this file in Jenkinsfile via 'load'

