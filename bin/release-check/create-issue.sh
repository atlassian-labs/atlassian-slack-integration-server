#!/usr/bin/env bash

# INPUT ENV VARS
product_type="${PRODUCT:?Please set PRODUCT. Examples: 'jira', 'confluence', or 'bitbucket'}"
release_label="${RELEASE_LABEL:?Please set RELEASE_LABEL.}"
workflow_links="${WORKFLOW_LINKS:?Please set WORKFLOW_LINKS.}"

# CREATE NEW ISSUE FOR RELEASE
product_first_upper="$(tr '[:lower:]' '[:upper:]' <<< "${product_type:0:1}")${product_type:1}"
create_issue_payload=$(cat <<EOF
{
  "title": "Prepare for new release of $product_first_upper $version",
  "body": "Workflow links: $workflow_links",
  "labels": ["release-check", "$release_label"]
}
EOF
)

# RUN CREATION REQUEST
echo "Creating issue with payload=[$create_issue_payload]" 1>&2
create_issue_response=$(curl -X POST -s -u "$GH_USER:$GH_TOKEN" \
  -H 'Content-Type: application/json' -d "$create_issue_payload" \
  "https://api.github.com/repos/atlassian-labs/atlassian-slack-integration-server/issues")

# EXTRACT NEW KEY FROM RESPONSE
new_issue_url=$(echo "$create_issue_response" | jq -r '.html_url')

# RETURN NEW ISSUE KEY
echo "$new_issue_url"
