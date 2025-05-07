#!/usr/bin/env bash

# INPUT ENV VARS
product_type="${PRODUCT:?Please set PRODUCT. Examples: 'jira', 'confluence', or 'bitbucket'}"

# PRODUCT ARTIFACT
# THIS ASSUMES THESE ARTIFACTS WILL NOT CHANGE - NEW MAJOR VERSIONS MAY REQUIRE UPDATING THIS SCRIPT
case $product_type in
  jira)
    GROUP=com/atlassian/jira
    ARTIFACT=jira-api
    ;;

  confluence)
    GROUP=com/atlassian/confluence
    ARTIFACT=confluence
    ;;

  bitbucket)
    GROUP=com/atlassian/bitbucket/server
    ARTIFACT=bitbucket-model
    ;;

  *)
    echo "Invalid product"
    exit 1
    ;;
esac

# SEARCH PAC
pac_search_query="https://maven.artifacts.atlassian.com/${GROUP}/${ARTIFACT}/maven-metadata.xml" #get xml
search_response=$(curl -s "${pac_search_query}")

# PARSE RESPONSE AND GET AN ARRAY OF VERSIONS THAT CONTAIN ONLY NUMBERS (EXCLUDES SNAPSHOTS AND MILESTONE RELEASES)
latest_releases=$(echo "$search_response" | grep -oE '<version>[0-9][0-9]?\.[0-9]+\.[0-9]+</version>' | grep -oE '[0-9][0-9]?\.[0-9]+\.[0-9]+')

echo "$latest_releases"

# OPTIONALLY, GROUP VERSION BY MINOR NUMBER AND TAKE ONLY LATEST REVISIONS
# latest_minor_releases=$(echo "$latest_releases" | jq -r 'group_by(. | split(".") | .[:2] | join(".")) | map(.[0]) | sort_by(. | split(".") | map(tonumber)) | reverse')
#echo "$latest_minor_releases"
