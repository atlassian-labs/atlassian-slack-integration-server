#!/usr/bin/env bash

start_pipeline () {
  local PIPELINE_NAME=${1}
  local pipeline_link
  pipeline_link=$(. ./start-pipeline.sh)
  echo "Pipeline '$PIPELINE_NAME' --> $pipeline_link"
}

# INPUT ENV VARS
jira_base_url="${JIRA_BASE_URL:?Please set JIRA_BASE_URL. Example: 'https://pi-dev-sandbox.atlassian.net'}"
jira_project="${JIRA_PROJECT:?Please set JIRA_PROJECT. Examples: 'JSS', 'CSS', 'BBSS'}"
product_type="${PRODUCT:?Please set PRODUCT. Examples: 'jira', 'confluence', or 'bitbucket'}"
version="${VERSION:?Please set VERSION.}"

# CHECK AN ISSUE LABELED WITH VERSION ALREADY EXISTS
release_label="$product_type-$version-release"
echo "Checking release for $product_type $version... checking if we have a ticket labeled $release_label for it in ${jira_base_url}/browse/${jira_project}"

issue_key=$(RELEASE_LABEL="$release_label" . ./fetch-release-issue.sh)

if [ ! "$issue_key" = "" ] ; then
  echo "Ticket already exists --> ${jira_base_url}/browse/${issue_key}"
  return
fi

# CREATE NEW ISSUE FOR RELEASE
echo "No ticket found... creating a new one"

new_issue_key=$(RELEASE_LABEL="$release_label" . ./create-issue.sh)

echo "New ticket created --> ${jira_base_url}/browse/${new_issue_key}"

# RUN TESTS AGAINST SPECIFIC VERSIONS
case $product_type in
  jira)
    start_pipeline "Jira IT JDK 8"
    start_pipeline "Jira IT JDK 11"
    ;;

  confluence)
    start_pipeline "Confluence IT JDK 8"
    ;;

  bitbucket)
    start_pipeline "Bitbucket IT JDK 8"
    start_pipeline "Bitbucket IT JDK 11"
    ;;

  *)
    echo "Invalid product"
    exit 1
    ;;
esac
