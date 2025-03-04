#!/bin/bash
echo 'Running backup...'
tar -czf backup_$(date +%Y%m%d).tar.gz /path/to/data
echo 'Backup completed.'
