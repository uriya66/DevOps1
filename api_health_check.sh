#!/bin/bash
# API health check script to verify that the Flask API server is running correctly

set -e  # Stop script if any command fails

API_URL="http://localhost:5000/api/health"

echo "Checking if Flask API is running..."

# Perform the API health check
RESPONSE=$(curl -s -w "%{http_code}" -o response.txt "$API_URL")
STATUS_CODE="${RESPONSE: -3}"  # Extract last 3 characters (HTTP status code)

# If API is not available, print error and exit
if [ "$STATUS_CODE" -ne 200 ]; then
    echo "ERROR: API is not available! Received HTTP Status $STATUS_CODE"
    echo "Response content:"
    cat response.txt
    exit 1
fi

echo "Flask API is up and running (HTTP Status: 200)"
rm response.txt  # Clean up temporary file
