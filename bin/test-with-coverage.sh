#!/usr/bin/env bash

echo "Make sure you have releases common project and define proper versions of dependencies"
(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
    mvn --batch-mode verify -P jacoco
)
