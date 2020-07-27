#!/usr/bin/env bash

# INPUT ENV VARS
release_label="${RELEASE_LABEL:?Please set RELEASE_LABEL.}"

# CHECK AN ISSUE LABELED WITH VERSION ALREADY EXISTS

# QUERY GITHUB FOR ISSUE WITH LABEL (INCLUDING CLOSED ISSUES)
issue_for_version_response=$(curl -s \
  "https://api.github.com/repos/atlassian-labs/atlassian-slack-integration-server/issues?labels=$release_label&state=all")

# CHECK WHETHER AT LEAST ONE RESULT ITEM IS FOUND
has_issue_for_version=$(echo "$issue_for_version_response" | jq -r 'length > 0')

# RETURN ISSUE KEY, IF ANY
if [ "$has_issue_for_version" = "true" ] ; then
  issue_url=$(echo "$issue_for_version_response" | jq -r '.[0].html_url')
  echo "$issue_url"
else
  echo ""
fi

