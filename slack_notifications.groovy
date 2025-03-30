// Construct a Slack message with Git info and app links
def constructSlackMessage(buildNumber, buildUrl, mergeSuccess = null, deploySuccess = null) {
    try {
        // Retrieve commit ID from Git
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()

        // Retrieve commit message from latest commit
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()

        // Retrieve the current branch name accurately from Jenkins environment
        def branch = env.GIT_BRANCH ?: sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()

        // Generate GitHub commit URL for direct reference
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"

        // Get pipeline duration in readable format
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"

        // Get dynamic public IP of the server
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()

        // Construct Flask app URL (without localhost)
        def appUrl = "http://${publicIp}:5000"

        // Build result summary clearly indicating success or failure
        def resultNote = ""
        if (mergeSuccess != null && deploySuccess != null) {
            resultNote += mergeSuccess ? "*Merge:* :white_check_mark: Successful\n" : "*Merge:* :x: Failed\n"
            resultNote += deploySuccess ? "*Deploy:* :white_check_mark: Successful\n" : "*Deploy:* :x: Failed\n"
        }

        // Build full Slack message with professional formatting
        return """
:white_check_mark: *Jenkins Build Completed!*
*Pipeline:* #${buildNumber}
*Branch:* ${branch}
*Commit:* [${commitId}](${commitUrl})
*Message:* ${commitMessage}
*Duration:* ${duration}
${resultNote}
*Pipeline Link:* ${buildUrl}
*Application Links:*
- ${appUrl}
        """.stripIndent()

    } catch (Exception e) {
        echo "Slack message error: ${e.message}"  // Log error if message construction fails
        return "*:x: Error constructing Slack message*\nReason: ${e.message}"  // Fallback Slack message
    }
}

// Send a message to Slack channel using Jenkins credentials
def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',  // Slack channel name
            tokenCredentialId: 'Jenkins-Slack-Token',  // Slack token credential ID
            message: message,  // Slack message content
            color: color  // Slack message color (good, warning, danger)
        )
    } catch (Exception e) {
        echo "Slack notification failed: ${e.message}"  // Log Slack sending failure
    }
}

// Return this script object so it can be loaded into Jenkinsfile
return this  // Return the script object to Jenkinsfile

