#!/usr/bin/env bash
set -e

# user credentials
USER=
PW=

# project key and
PROJECT_KEY=
TEAM_ID=
CHANNEL_ID=

# your Jira
JIRA_URL=

# represent one line of configuration in the configuration page table
# a timestamp will be used to make sure it creates a new group every time the script is ran
# if you want to update a configuration, set its group id here
GROUP_ID=$(date +%s)

# fetch project id automatically, make it easier to use
PROJECT_ID=$(curl -s --user ${USER}:${PW} "${JIRA_URL}/rest/api/2/project/${PROJECT_KEY}" | jq -r '.id')

# function that makes the request to check one of the configuration options
enable () {
  NAME_FIELD=$([ -z "$1" ] && echo "" || echo ",\"name\":\"${1}\"")
  VALUE_FIELD=$([ -z "$2" ] && echo "" || echo ",\"value\":\"${2}\"")

  curl --user ${USER}:${PW} "${JIRA_URL}/slack/mapping/${PROJECT_KEY}" \
    -H 'Content-Type: application/json' \
    --data-binary "{\"projectId\":${PROJECT_ID},\"projectKey\":\"${PROJECT_KEY}\",\"teamId\":\"${TEAM_ID}\",\"channelId\":\"${CHANNEL_ID}\",\"configurationGroupId\":\"${GROUP_ID}\"${NAME_FIELD}${VALUE_FIELD}}"
}

# create main configuration group
enable

# check notification types
enable "MATCHER:ISSUE_CREATED" "true"
enable "MATCHER:ISSUE_COMMENTED" "true"
enable "MATCHER:ISSUE_ASSIGNMENT_CHANGED" "true"

# optionally, one could pass a comma-separated status' ids in second parameter
enable "MATCHER:ISSUE_TRANSITIONED"

# if you want compact 2-line notifications
enable "VERBOSITY" "BASIC"

