#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

install_dir="/opt/atlassian"
sdk_dir="atlassian-plugin-sdk"
target_dir=${install_dir}/${sdk_dir}

echo "Downloading https://marketplace.atlassian.com/download/plugins/atlassian-plugin-sdk-tgz ..."
wget --no-verbose -O "atlassian-plugin-sdk.tar.gz" "https://marketplace.atlassian.com/download/plugins/atlassian-plugin-sdk-tgz"

echo "Extracting atlassian-plugin-sdk.tar.gz to ${install_dir} ..."
mkdir -p ${install_dir}
tar -xzf atlassian-plugin-sdk.tar.gz -C ${install_dir}
# It unpacks to atlassian-plugin-sdk-<version> so rename it to atlassian-plugin-sdk
mv ${install_dir}/atlassian-plugin-sdk* ${target_dir}
chmod -R +x ${target_dir}

${target_dir}/bin/atlas-version

# Adding to the system PATH
echo "${target_dir}/bin" >> "$GITHUB_PATH"
