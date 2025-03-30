def constructSlackMessage(buildNumber, buildUrl, mergeSuccess = null, deploySuccess = null) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
        def branch = env.GIT_BRANCH ?: "unknown-branch"
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"
        def duration = currentBuild.durationString.replace(' and counting', '')
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()
        def appUrl = "http://${publicIp}:5000"

        def resultNote = ""
        if (mergeSuccess != null && deploySuccess != null) {
            resultNote += mergeSuccess ? "*Merge:* ✅ Successful\n" : "*Merge:* ❌ Failed - Merge did not complete successfully\n"
            resultNote += deploySuccess ? "*Deploy:* ✅ Successful\n" : "*Deploy:* ❌ Failed - Deployment script encountered an issue\n"
        }

        return """
*✅ Jenkins Build Completed!*
*Pipeline:* #${buildNumber}
*Branch:* ${branch}
*Commit:* [${commitId}](${commitUrl})
*Message:* ${commitMessage}
*Duration:* ${duration}
${resultNote}
*Pipeline Link:* ${buildUrl}
*Application Link:* ${appUrl}
""".stripIndent()
    } catch (e) {
        echo "Slack message error: ${e.message}"
        return "*❌ Error constructing Slack message*\nReason: ${e.message}"
    }
}

def sendSlackNotification(String message, String color) {
    try {
        slackSend(
            channel: '#jenkis_alerts',
            tokenCredentialId: 'Jenkins-Slack-Token',
            message: message,
            color: color
        )
    } catch (e) {
        echo "Slack notification failed: ${e.message}"
    }
}

// Return this script object so it can be load to Jenkinsfile
return this  // Return this object to Jenkinsfile
