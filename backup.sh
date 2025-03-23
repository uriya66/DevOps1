#!/bin/bash
set -e  # Exit script on any error

# Define backup directory (outside of Git repo)
BACKUP_DIR="/var/lib/jenkins/backups/DevOps1"

echo "Running backup..."

# Create backup directory if it doesn't exist
if [ ! -d "$BACKUP_DIR" ]; then
    echo "Creating backup directory at: $BACKUP_DIR"
    mkdir -p "$BACKUP_DIR"
fi

# Create a backup file with timestamp
BACKUP_FILE="$BACKUP_DIR/backup_$(date +%Y%m%d_%H%M%S).tar.gz"

# Create compressed archive of the project folder (excluding .git and backup folders)
tar --exclude='./.git' --exclude='./backup' -czf "$BACKUP_FILE" .

echo "✅ Backup completed successfully: $BACKUP_FILE"

