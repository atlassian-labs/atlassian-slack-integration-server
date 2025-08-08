#!/usr/bin/env bash

# Override Maven from Plugin SDK with the one bundled into Github Actions image;

BUNDLED_MVN_HOME=$(mvn -v | grep "Maven home" | sed 's/Maven home: //')
echo "ATLAS_MVN=$BUNDLED_MVN_HOME/bin/mvn" >> $GITHUB_ENV