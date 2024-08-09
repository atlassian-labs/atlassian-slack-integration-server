#!/usr/bin/env bash

start_workflow () {
  local workflow_name=${1}
  local java_version=${2}
  local product_version=${3}
  local workflow_link

  workflow_link=$(WORKFLOW_NAME=$workflow_name JAVA_VERSION=$java_version PRODUCT_VERSION=$product_version . ./start-workflow.sh)
  echo $workflow_link
}

# INPUT ENV VARS
product_type="${PRODUCT:?Please set PRODUCT. Examples: 'jira', 'confluence', or 'bitbucket'}"
product_version="${PRODUCT_VERSION:?Please set PRODUCT_VERSION.}"

# CHECK AN ISSUE LABELED WITH VERSION ALREADY EXISTS
release_label="$product_type-$product_version-release"
echo "Checking release for $product_type $product_version... checking if we have a ticket labeled $release_label for it in https://github.com/atlassian-labs/atlassian-slack-integration-server"

issue_url=$(RELEASE_LABEL="$release_label" . ./fetch-release-issue.sh)

if [ ! "$issue_url" = "" ] ; then
  echo "Issue already exists -> ${issue_url}."
  return
else
  echo "No issue found for label $release_label. Running test workflows..."
fi

# DETERMINE WORKFLOW NAME
case $product_type in
  jira)
    workflow_name=jira-int-tests.yml
    ;;
  confluence)
    workflow_name=confluence-int-tests.yml
    ;;
  bitbucket)
    workflow_name=bitbucket-int-tests.yml
    ;;
  *)
    echo "Invalid product"
    exit 1
    ;;
esac

echo "Determined workflow name: $workflow_name"

# RUN TESTS AGAINST SPECIFIC VERSIONS
workflow_links=()
# Bitbucket 9+ does not support Java 21
if [ $product_type != "bitbucket" ]; then
  echo "Running workflow with params: workflow-name=$workflow_name java-version=21 product-version=$product_version"
  first_workflow_link=$(start_workflow $workflow_name 21 $product_version)
  echo "Pipeline URL: $first_workflow_link"
  workflow_links+=("$first_workflow_link")
fi

echo "Running workflow with params: workflow-name=$workflow_name java-version=17 product-version=$product_version"
second_workflow_link=$(start_workflow $workflow_name 17 $product_version)
echo "Pipeline URL: $second_workflow_link"
workflow_links+=("$second_workflow_link")

# Join elements with a multi-character delimiter
function join_by {
  local d=${1-} f=${2-}
  if shift 2; then
    printf %s "$f" "${@/#/$d}"
  fi
}

# CREATE NEW ISSUE FOR RELEASE
echo "Creating a new issue"
new_issue_url=$(RELEASE_LABEL="$release_label" WORKFLOW_LINKS="$(join_by ', ' ${workflow_links[@]})" . ./create-issue.sh)

echo "New ticket created: $new_issue_url"
