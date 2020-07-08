package com.atlassian.jira.plugins.slack.model.ao.upgrade.v1;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import net.java.ao.Query;

@Slf4j
public class ProjectConfigurationUpgradeTask001 implements ActiveObjectsUpgradeTask {
    private final int limit;

    public ProjectConfigurationUpgradeTask001() {
        this(50);
    }

    @VisibleForTesting
    ProjectConfigurationUpgradeTask001(final int limit) {
        this.limit = limit;
    }

    @Override
    public ModelVersion getModelVersion() {
        return ModelVersion.valueOf("1");
    }

    @Override
    public void upgrade(final ModelVersion version, final ActiveObjects ao) {
        log.info("Running ProjectConfigurationUpgradeTask001");

        ao.migrate(ProjectConfigurationAOV001.class);

        int offset = 0;
        int configsProcessed = 0;

        Query query = baseQuery(limit).offset(offset);
        ProjectConfigurationAOV001[] projectConfigs = ao.find(ProjectConfigurationAOV001.class, query);
        log.info("Found {} configs to migrate", projectConfigs.length);

        while (projectConfigs.length > 0) {
            for (ProjectConfigurationAOV001 projectConfig : projectConfigs) {
                projectConfig.setValueUnlimited(projectConfig.getValue());
                projectConfig.save();

                configsProcessed++;
            }

            offset += limit;
            query = baseQuery(limit).offset(offset);
            projectConfigs = ao.find(ProjectConfigurationAOV001.class, query);

            log.info("Found {} more configs to migrate", projectConfigs.length);
        }

        log.info("Migrated {} configs", configsProcessed);
    }

    private Query baseQuery(final int limit) {
        return Query.select().order("ID").limit(limit);
    }
}
