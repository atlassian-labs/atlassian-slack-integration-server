#!/usr/bin/env bash

# INPUT ENV VARS
jira_base_url="${JIRA_BASE_URL:?Please set JIRA_BASE_URL. Example: 'https://pi-dev-sandbox.atlassian.net'}"
jira_project="${JIRA_PROJECT:?Please set JIRA_PROJECT. Examples: 'JSS', 'CSS', 'BBSS'}"
product_type="${PRODUCT:?Please set PRODUCT. Examples: 'jira', 'confluence', or 'bitbucket'}"
atl_api_user_email="${ATL_API_USER_EMAIL:?Please set ATL_API_USER_EMAIL.}"
atl_api_token="${ATL_API_TOKEN:?Please set ATL_API_TOKEN. Obtain yours at https://id.atlassian.com/manage-profile/security/api-tokens}"
version="${VERSION:?Please set VERSION.}"
release_label="${RELEASE_LABEL:?Please set RELEASE_LABEL.}"

# CREATE NEW ISSUE FOR RELEASE
product_first_upper="$(tr '[:lower:]' '[:upper:]' <<< "${product_type:0:1}")${product_type:1}"
create_issue_payload=$(cat <<EOF
{
  "fields": {
    "summary": "Prepare for new release of $product_first_upper $version",
    "project": {
      "key": "$jira_project"
    },
    "issuetype": {
      "id": 10001
    },
    "labels": ["$release_label"]
  }
}
EOF
)

# RUN CREATION REQUEST
create_issue_response=$(curl --user "${atl_api_user_email}:${atl_api_token}" -X POST -H "Content-Type: application/json" \
  -d "$create_issue_payload" -s "$jira_base_url/rest/api/3/issue")

# EXTRACT NEW KEY FROM RESPONSE
new_issue_key=$(echo "$create_issue_response" | jq -r '.key')

# RETURN NEW ISSUE KEY
echo "$new_issue_key"
