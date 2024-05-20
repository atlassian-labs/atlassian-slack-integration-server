#!/usr/bin/env bash

# TODO: Remove -Denforcer.skip=true after moving from milestone versions

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
    atlas-mvn confluence:debug \
        -Denforcer.skip=true \
        -Datlassian.dev.mode=true \
        -Dmaven.test.skip=true \
        "$@" \
        -pl confluence-slack-integration/confluence-slack-server-integration-plugin \
        | tee confluence.log
)
