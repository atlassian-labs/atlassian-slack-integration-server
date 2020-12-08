#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

case "$1" in
  jira)
    echo "PLUGIN=jira-slack-server-integration/jira-slack-server-integration-plugin" >> $GITHUB_ENV
    ;;
  confluence)
    echo "PLUGIN=confluence-slack-server-integration-plugin" >> $GITHUB_ENV
    ;;
  bitbucket)
    echo "PLUGIN=bitbucket-slack-server-integration-plugin" >> $GITHUB_ENV
    ;;
  *)
    echo "No plugin found for product [$1]"
    exit 1
esac
