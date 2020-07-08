#!/usr/bin/env bash

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
    mvn package --batch-mode \
        -Dmaven.test.skip=true \
        --activate-profiles include-common \
        "$@" \
        -pl ${PLUGIN}
)


