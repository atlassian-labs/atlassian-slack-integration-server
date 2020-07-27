#!/usr/bin/env bash

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
    atlas-mvn install --batch-mode -Dmaven.test.skip=true \
        "$@" \
        -pl com.atlassian.plugins:atlassian-slack-server-integration-parent,jira-slack-server-integration,slack-server-integration-common,slack-server-integration-test-common && \
    rm -rf jira-slack-server-integration/jira-slack-server-integration-plugin/target/classes/com/atlassian/plugins/slack && \
    rm -f jira-slack-server-integration/jira-slack-server-integration-plugin/target/dependency-maven-plugin-markers/com.atlassian.plugins-slack-server-integration-common-jar-* && \
    rm -rf confluence-slack-server-integration-plugin/target/classes/com/atlassian/plugins/slack && \
    rm -f confluence-slack-server-integration-plugin/target/dependency-maven-plugin-markers/com.atlassian.plugins-slack-server-integration-common-jar-* && \
    rm -rf bitbucket-slack-server-integration-plugin/target/classes/com/atlassian/plugins/slack && \
    rm -f bitbucket-slack-server-integration-plugin/target/dependency-maven-plugin-markers/com.atlassian.plugins-slack-server-integration-common-jar-*
)
