#!/bin/bash
set -e  # Exit script on any error

# Define backup directory inside the GitHub repository
BACKUP_DIR="./Backup"

echo "Running backup..."

# Create backup directory if it doesn't exist
if [ ! -d "$BACKUP_DIR" ]; then
    echo "Creating backup directory..."
    mkdir -p "$BACKUP_DIR"
fi

# Create a backup file with timestamp
BACKUP_FILE="$BACKUP_DIR/backup_$(date +%Y%m%d_%H%M%S).tar.gz"

# Exclude the Backup folder itself to avoid recursion
tar --exclude='./Backup' -czf "$BACKUP_FILE" .

echo "Backup completed successfully: $BACKUP_FILE"

