#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

MAVEN_HOME=$(atlas-version | grep 'ATLAS Maven Home' | grep -oE '/.+$')

# Add atlassian maven servers to deploy releases there
# maven-atlassian-com is defined in parent POM: com.atlassian.pom:public-pom:5.0.26
sudo sed -i'backupServer' '/<servers>/ a\
<server><id>maven-atlassian-com</id><username>${env.ARTIFACTORY_USER}</username><password>${env.ARTIFACTORY_PASSWORD}</password></server>' $MAVEN_HOME/conf/settings.xml
