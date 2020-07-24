#!/usr/bin/env bash

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
    atlas-mvn jira:debug \
        -Datlassian.dev.mode=true \
        -Dmaven.test.skip=true \
        "$@" \
        -pl jira-slack-server-integration/jira-slack-server-integration-plugin \
        | tee jira.log
)
