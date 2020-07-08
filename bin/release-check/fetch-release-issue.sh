#!/usr/bin/env bash

# INPUT ENV VARS
jira_base_url="${JIRA_BASE_URL:?Please set JIRA_BASE_URL. Example: 'https://pi-dev-sandbox.atlassian.net'}"
jira_project="${JIRA_PROJECT:?Please set JIRA_PROJECT. Examples: 'JSS', 'CSS', 'BBSS'}"
atl_api_user_email="${ATL_API_USER_EMAIL:?Please set ATL_API_USER_EMAIL.}"
atl_api_token="${ATL_API_TOKEN:?Please set ATL_API_TOKEN. Obtain yours at https://id.atlassian.com/manage-profile/security/api-tokens}"
release_label="${RELEASE_LABEL:?Please set RELEASE_LABEL.}"

# CHECK AN ISSUE LABELED WITH VERSION ALREADY EXISTS

# QUERY JIRA FOR LABEL
issue_for_version_response=$(curl --user "${atl_api_user_email}:${atl_api_token}" -s \
  "$jira_base_url/rest/api/3/search?jql=project=$jira_project%20AND%20labels=$release_label")

# CHECK WHETHER AT LEAST ONE RESULT ITEM IS FOUND
has_issue_for_version=$(echo "$issue_for_version_response" | jq -r '.total > 0')

# RETURN ISSUE KEY, IF ANY
if [ "$has_issue_for_version" = "true" ] ; then
  issue_key=$(echo "$issue_for_version_response" | jq -r '.issues[0].key')
  echo "$issue_key"
else
  echo ""
fi

