#!/usr/bin/env bash

atlas-mvn install --batch-mode \
    --projects \
com.atlassian.plugins:atlassian-slack-server-integration-parent,\
slack-server-integration-common,\
jira-slack-server-integration,\
jira-slack-server-integration/jira-8-compat,\
jira-slack-server-integration/jira-service-desk-compat,\
jira-slack-server-integration/jira-service-desk-compat/jira-service-desk-compat-common,\
jira-slack-server-integration/jira-service-desk-compat/jira-service-desk-4-compat,\
jira-slack-server-integration/jira-service-desk-compat/jira-service-desk-compat-main,\
confluence-slack-integration,\
confluence-slack-integration/confluence-compat-common,\
confluence-slack-integration/confluence-7-compat,\
confluence-slack-integration/confluence-8-compat,\
slack-server-integration-test-common \
    -Dmaven.test.skip=true
