#!/usr/bin/env bash

# INPUT ENV VARS
version_type="${1:?Please set pass a version type. Examples: 'jira', 'confluence', 'bitbucket' or 'common'}"
# [-SNAPSHOT] part prevents matching version in a [parent] section
version_pattern='<version>[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT</version>'

case $version_type in
  jira)
    pl_path='jira-slack-server-integration/jira-slack-server-integration-plugin/pom.xml'
    ;;
  confluence)
    pl_path='confluence-slack-integration/confluence-slack-server-integration-plugin/pom.xml'
    ;;
  bitbucket)
    pl_path='bitbucket-slack-server-integration-plugin/pom.xml'
    ;;
  common)
    pl_path='pom.xml'
    version_pattern='<version>[0-9]+\.[0-9]+\.[0-9]+</version>'
    ;;
  *)
    echo "Invalid version type"
    exit 1
    ;;
esac

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
pl_version=$(cat "$SCRIPT_DIR/../../$pl_path" | grep -oE -m 1 $version_pattern | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
echo "${pl_version%.*.*}"