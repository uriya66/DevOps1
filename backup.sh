#!/bin/bash
set -e  # Exit on error

# Define backup dir OUTSIDE the repo
BACKUP_DIR="/var/lib/jenkins/backups/DevOps1"

# Create backup dir if not exists
if [ ! -d "$BACKUP_DIR" ]; then
    echo "Creating backup directory at $BACKUP_DIR"
    mkdir -p "$BACKUP_DIR"
fi

# Get Jenkins build number and timestamp
BUILD_NUM="${BUILD_NUMBER:-manual}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Backup file path
BACKUP_FILE="$BACKUP_DIR/build_${BUILD_NUM}_$TIMESTAMP.tar.gz"

echo "Creating backup file: $BACKUP_FILE"

# Create compressed archive (exclude .git, venv, and backups)
tar --exclude='./.git' --exclude='./venv' --exclude='./backup' \
    -czf "$BACKUP_FILE" .

echo " Backup completed: $BACKUP_FILE"


