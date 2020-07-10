#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

case "$1" in
  jira)
    echo "::set-env name=PLUGIN::jira-slack-server-integration/jira-slack-server-integration-plugin"
    ;;
  confluence)
    echo "::set-env name=PLUGIN::confluence-slack-server-integration-plugin"
    ;;
  bitbucket)
    echo "::set-env name=PLUGIN::bitbucket-slack-server-integration-plugin"
    ;;
  *)
    echo "No plugin found for product [$1]"
    exit 1
esac
