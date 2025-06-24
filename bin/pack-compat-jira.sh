#!/usr/bin/env bash

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
    atlas-mvn install --batch-mode -Dmaven.test.skip=true "$@" -pl com.atlassian.plugins:jira-service-desk-compat,com.atlassian.plugins:jira-service-desk-compat-common,com.atlassian.plugins:jira-service-desk-4-compat,com.atlassian.plugins:jira-service-desk-compat-main
)
