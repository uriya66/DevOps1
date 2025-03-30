def constructSlackMessage(buildNumber, buildUrl, mergeSuccess, deploySuccess, branchName) {
    try {
        def commitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
        def commitUrl = "https://github.com/uriya66/DevOps1/commit/${commitId}"
        def duration = "${currentBuild.durationString.replace(' and counting', '')}"
        def publicIp = sh(script: "curl -s http://checkip.amazonaws.com", returnStdout: true).trim()
        def appUrl = "http://${publicIp}:5000"

        def resultNote = ""
        resultNote += mergeSuccess == 'true' ? "*Merge:* ✅ Successful\n" : "*Merge:* ❌ Failed\n"
        resultNote += deploySuccess == 'true' ? "*Deploy:* ✅ Successful\n" : "*Deploy:* ❌ Failed\n"

        return """
✅ *Jenkins Build Completed!*
*Pipeline:* #${buildNumber}
*Branch:* ${branchName}
*Commit:* [${commitId}](${commitUrl})
*Message:* ${commitMessage}
*Duration:* ${duration}

${resultNote}
*Pipeline Link:* ${buildUrl}
*App Link:* ${appUrl}
        """.stripIndent()

    } catch (Exception e) {
        echo "Slack message error: ${e.message}"
        return "❌ Slack message construction error\nReason: ${e.message}"
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
    } catch (Exception e) {
        echo "Slack notification failed: ${e.message}"
    }
}

return this

