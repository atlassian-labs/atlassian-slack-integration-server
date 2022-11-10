#!/usr/bin/env bash

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
    atlas-mvn install --batch-mode -Dmaven.test.skip=true "$@" -pl \
com.atlassian.plugins:confluence-compat-common,\
com.atlassian.plugins:confluence-7-compat,\
com.atlassian.plugins:confluence-8-compat && \
    rm -rf confluence-slack-integration/confluence-slack-server-integration-plugin/target/classes/com/atlassian/confluence/plugins/slack/util/compat && \
    rm -f confluence-slack-integration/confluence-slack-server-integration-plugin/target/dependency-maven-plugin-markers/com.atlassian.plugins-confluence-compat-common-jar-*
    rm -f confluence-slack-integration/confluence-slack-server-integration-plugin/target/dependency-maven-plugin-markers/com.atlassian.plugins-confluence-7-compat-jar-*
    rm -f confluence-slack-integration/confluence-slack-server-integration-plugin/target/dependency-maven-plugin-markers/com.atlassian.plugins-confluence-8-compat-jar-*
)
