#!/bin/bash
# Script to create a backup of the specified directory

echo 'Running backup...'

# Create a compressed tar archive with the current date in its name
tar -czf backup_$(date +%Y%m%d).tar.gz /path/to/data

echo 'Backup completed.'
