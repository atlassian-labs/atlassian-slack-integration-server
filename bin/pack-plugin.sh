#!/usr/bin/env bash

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
    atlas-mvn package --batch-mode \
        -Dmaven.test.skip=true \
        --activate-profiles include-common \
        "$@" \
        -pl ${PLUGIN}
)


