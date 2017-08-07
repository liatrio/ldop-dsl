#!/bin/bash
# Description:
#   Validates changelog is changed. 

echo "CHANGELOG.md has been changed."
if [ -z $(git diff-tree --no-commit-id --name-only HEAD | grep CHANGELOG.md)]; then
  echo "CHANGELOG not updated for this tag, exiting failure."
else
  echo "CHANGELOG included in commit"
fi

