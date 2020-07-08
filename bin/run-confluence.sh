#!/usr/bin/env bash

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
    mvn confluence:debug \
        -Datlassian.dev.mode=true \
        -Dmaven.test.skip=true \
        "$@" \
        -pl confluence-slack-server-integration-plugin \
        | tee confluence.log
)
