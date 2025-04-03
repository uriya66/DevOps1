#!/bin/bash

# ----------------------------------------
# Professional Git merge script for DevOps pipelines
# ----------------------------------------

set -e  # Exit on error

# Fetch latest changes
echo "[INFO] Fetching all branches..."
git fetch origin

# Checkout main and update
echo "[INFO] Checking out main and pulling latest changes..."
git checkout main
git pull origin main

# Merge feature-test into main
echo "[INFO] Merging 'feature-test' into 'main'..."
git merge feature-test

# Push updated main branch to GitHub
echo "[INFO] Pushing 'main' to remote repository..."
git push origin main

# Return to feature-test
echo "[INFO] Switching back to 'feature-test'..."
git checkout feature-test

echo "[SUCCESS] Merge completed. Main is now up-to-date with feature-test."

