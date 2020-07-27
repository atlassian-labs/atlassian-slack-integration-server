#!/usr/bin/env bash

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
    atlas-mvn install --batch-mode -Dmaven.test.skip=true "$@" -pl com.atlassian.plugins:jira-8-compat,com.atlassian.plugins:jira-service-desk-compat,com.atlassian.plugins:jira-service-desk-compat-common,com.atlassian.plugins:jira-service-desk-3-compat,com.atlassian.plugins:jira-service-desk-4-compat,com.atlassian.plugins:jira-service-desk-compat-main && \
    rm -rf jira-slack-server-integration/jira-slack-server-integration-plugin/target/classes/com/atlassian/plugin/slack/jira/compat && \
    rm -f jira-slack-server-integration/jira-slack-server-integration-plugin/target/dependency-maven-plugin-markers/com.atlassian.plugins-jira-8-compat-jar-*
)
