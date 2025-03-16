#!/bin/bash
# 📂 Script to create a backup of the project directory

echo "🛠 Running backup"

# 📂 Define backup directory inside project
BACKUP_DIR="/var/lib/jenkins/workspace/DevOps1/backup"

# 📦 Create backup directory if it doesn't exist
if [ ! -d "$BACKUP_DIR" ]; then
    echo "📂 Creating backup directory..."
    mkdir -p "$BACKUP_DIR"
fi

# 🗂 Create a backup file with timestamp
BACKUP_FILE="$BACKUP_DIR/backup_$(date +%Y%m%d_%H%M%S).tar.gz"

# 📦 Create compressed archive
tar -czf "$BACKUP_FILE" /var/lib/jenkins/workspace/DevOps1

echo "✅ Backup completed successfully: $BACKUP_FILE"
