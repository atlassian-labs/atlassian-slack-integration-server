
# Atlassian Data Center Integrations for Slack

[![Atlassian license](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)](LICENSE) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](CONTRIBUTING.md)

Official plugins for Jira Data Center, Confluence Data Center, and Bitbucket Date Center that integrate with [Slack](https://slack.com/).


# Usage

Jira, Confluence or Bitbucket administrator can install Slack plugin to their system via embedded Marketplace (**\<Configuration gear\>** -> **Find new apps/plugins**)
or by manually downloading plugin JAR files from Marketplace pages for [Jira](https://marketplace.atlassian.com/apps/1220099/jira-server-for-slack-official?hosting=datacenter&tab=overview), 
[Confluence](https://marketplace.atlassian.com/apps/1220186/confluence-server-for-slack-official?hosting=datacenter&tab=overview) 
or [Bitbucket](https://marketplace.atlassian.com/apps/1220729/bitbucket-server-for-slack-official?hosting=datacenter&tab=overview) plugins.
Links to the official documentation are specified on Marketplace pages.

Supported products. See [EOL policy](https://confluence.atlassian.com/support/atlassian-support-end-of-life-policy-201851003.html).
* `master`/`dev` branch
  * Jira: 10+ (not released yet) on JDK 17.
  * Confluence: 9+ on JDK 17.
  * Bitbucket: 9+ on JDK 17.

* `release-1.x` branch
  * Jira: 8.15.0 (EOL date: 2 Feb 2023) JDK 8, 11 - 9.5.0 (EOL date: 6 Dec 2024) on JDK 8, 11, 17.
  * Confluence: 7.10 (EOL date: Dec 15, 2022) JDK 8, 11 - 8.0.0-m90 (EOL date: TBD - end of 2024) JDK 8, 11.
  * Bitbucket: 7.6.0 (EOL date: Q1 calendar year 2023) on JDK 8, 11 - 8.8.0 (EOL date: 7 Feb 2025) on JDK 8, 11, 17.

## A note on future development plans

In order to [accelerate our journey to the cloud, together](https://www.atlassian.com/blog/announcements/journey-to-cloud), Atlassian will continue to maintain these apps' _compatibility_ with our Server products. However, we will not be creating new features or matching feature parity with our Cloud integrations. If you would like to add your own customizations/features to the integration, we encourage you to fork this repository and customize it as you wish like we’ve seen many customers do.

When Jira, Confluence, or Bitbucket Data Center release new versions, we will validate the compatibility of these apps and release new versions within four weeks of their public release.

# Installation

1. Download and install [JDK 17](https://adoptium.net/en-GB/temurin/releases/?version=17). After installation `
java` command should be available in the terminal and `JAVA_HOME` environment variable should point to JDK installation directory.
Running `$JAVA_HOME/bin/java -version` should print a JDK version.
> **Note:** Use [JDK 8](https://adoptium.net/en-GB/temurin/releases/?version=8) / [JDK 11](https://adoptium.net/en-GB/temurin/releases/?version=11) if working with the `release-1.x` branch
>
> **Note:** Plugin can be compiled and run using **JDK 11**, but some old products may not support it.
2. Download and install [Atlassian Plugin SDK](https://developer.atlassian.com/server/framework/atlassian-sdk/install-the-atlassian-sdk-on-a-linux-or-mac-system/). 
After successful installation running `atlas-version` should print SDK version.
3. (Optional) Install [ngrok](https://ngrok.com/) to enable Slack -> product features (slash commands, unfurling). 
If you don't have ngrok, the plugin still can send notification to Slack in uni-directional way. 
Slack demands HTTPS OAuth redirect URLs, so you also need to add your ngrok host to domain allowlist at
http://localhost:2990/jira/plugins/servlet/whitelist. 
4. Install Maven 3.8.6 or later and define path to its executable: `export ATLAS_MVN: /usr/share/apache-maven-3.8.6/bin/mvn`.
It's needed because current versions of AMPS plugin isn't compatible with Maven bundled into the Atlassian Plugin SDK.
5. If you are setting up the project for the first time run `./jira.sh common` from the project root directory to install 
all common modules to local Maven repository.
6. Go to **\<product> Plugin Development** section for further steps. 

# Jira Plugin Development

Use tool `./jira.sh` for all dev cycle:

```bash
# getting help
./jira.sh help

# compiling and running Jira
./jira.sh run

# recompiling common project and plugin for quick reloading
./jira.sh

# recompile common, compatibility modules and the plugin, and reinstall fresh plugin version
./jira.sh common compat pack

# clean compilied code, but keep Jira database
./jira.sh clean

# clean complied along with Jira database
./jira.sh purge
```

# Confluence Plugin Development

Use tool `./confluence.sh` for all dev cycle. It has a similar set of commands to `./jira.sh` (see **Jira Server Plugin Development** section).

# Bitbucket Plugin Development

Use tool `./bitbucket.sh` for all dev cycle. It has a similar set of commands to `./jira.sh` (see **Jira Server Plugin Development** section).

# Documentation

Links to the official documentation are specified on plugin Marketplace pages (see **Usage** section). 
This is [the same link](https://confluence.atlassian.com/slack/atlassian-for-slack-integrations-967327515.html).

## Development References

- [Google Closure Templates / Soy Templates](https://developers.google.com/closure/templates/docs/concepts)
- [slackdown](https://github.com/blockmar/slackdown): parses slack messages to HTML. See also: https://api.slack.com/methods/emoji.list
- [Jira 8: be careful when using JQL from background](https://community.developer.atlassian.com/t/migrating-apps-to-jira-8-be-careful-when-using-jql-from-background-threads/25686)
- [Slack emoji table](https://unicodey.com/emoji-data/table.htm)

# Tests

Run all unit tests (from the root project folder):
```bash
atlas-mvn clean test
```
Run unit tests in a specific module:
```bash
cd <that-module>
atlas-mvn clean test
```

## Integration tests
Approaches:
1. **Recommended:** launch the product with `./<product-script>.sh run` command and run separate tests from your IDE. 
A Firefox window where you can easily locate and debug test errors.
2. By default in CI integration tests are run in [xvfb](https://en.wikipedia.org/wiki/Xvfb). You may install it locally 
and run tests with it to get a behaviour consistent with CI results.
3. You can also run test in real Firefox, for that specify `XVFB_ENABLE=false` environment variable for before running the script.

Run integration tests for Jira plugin:
```bash
bin/build/run-jira-its.sh
```
Run integration tests for Confluence plugin:
```bash
bin/build/run-confluence-its.sh
```

Run integration tests for Bitbucket plugin:
```bash
bin/build/run-bitbucket-its.sh
```

# CI/CD
## Tests
Unit and integration tests against the oldest and newest supported product versions are run on each commit by Github Actions.
See [all-tests.yml](.github/workflows/all-tests.yml) for more details. A specific test job may be run manually on any branch from 
**"Actions"** tab on the repo page by specifying `ref` and `jobs` arguments.

Note: all integration test jobs in default workflow (`all-tests.yml`) are dependent on unit tests, so to run integration tests
for specific product pass the respective job's name along with `unit-test` to `jobs` parameter. For example: `unit-tests,integration-tests-jira-8`.

Integration tests for arbitrary versions of the product and JVM may be run manually using 
[jira-int-tests.yml](.github/workflows/jira-int-tests.yml), [confluence-int-tests.yml](.github/workflows/confluence-int-tests.yml)
and [bitbucket-int-tests.yml](.github/workflows/bitbucket-int-tests.yml).

## Releasing
Release workflow allows to publish new releases to [GitHub Packages](https://github.com/orgs/atlassian-labs/packages?repo_name=atlassian-slack-integration-server). 
This action should be usually be run by repo maintainer only. See workflow configuration in [release.yml](.github/workflows/release.yml).

# Contributions

Contributions to Atlassian Data Center Integrations for Slack project are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

# License

Copyright (c) 2018 - 2020 Atlassian and others.
Apache 2.0 licensed, see [LICENSE](LICENSE) file.

[![With love from Atlassian](https://raw.githubusercontent.com/atlassian-internal/oss-assets/master/banner-cheers.png)](https://www.atlassian.com)
