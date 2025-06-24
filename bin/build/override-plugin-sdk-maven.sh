#!/usr/bin/env bash

# override Maven from Plugin SDK with the one bundled into Github Actions image;
# Maven from Plugin SDK is incompatible with newest AMPS plugins used to run Confluence 8 and Jira 9.5

BUNDLED_MVN_HOME=$(mvn -v | grep "Maven home" | sed 's/Maven home: //')
mvn -v
atlas-version
echo "ATLAS_MVN=$BUNDLED_MVN_HOME/bin/mvn" >> $GITHUB_ENV