#!/bin/bash
# Author: Justin Bankes
# Last Modified: 8/7/2017 
# Description: 
#   Validates git tags and verifies 

# Check for git tag
if [ -z ${GIT_TAG_NAME} ]; then
  echo "ERROR: No git tag for version, exiting failure"
  exit 1
else 
  echo "Git tag: ${GIT_TAG_NAME}"
fi

# Validate git format
echo "checking version format..."
if [[ "${GIT_TAG_NAME}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "version format accepted"
else 
  echo "version format incorrect; exiting failure"
  exit 1
fi


