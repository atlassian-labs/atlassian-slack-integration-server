package com.atlassian.jira.plugins.slack.model.ao.upgrade.v1;

import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import net.java.ao.RawEntity;
import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

/**
 * Temporary model for upgrade to version 1. Its main purpose - make 'value' field of unlimited length.
 * Since documentation does not recommend to use in-place type change, the migration will be done by adding a new field
 * 'value_unlimited' and copying there values from 'value' column using this temporary model that contains both fields.
 *
 * https://developer.atlassian.com/server/framework/atlassian-sdk/upgrading-your-plugin-and-handling-data-model-updates/
 */
@Table("PROJECT_CONFIG")
public interface ProjectConfigurationAOV001 extends ProjectConfiguration, RawEntity<Integer> {
    @Indexed
    @NotNull
    long getProjectId();

    void setProjectId(long projectId);

    @Indexed
    @NotNull
    String getTeamId();

    void setTeamId(String teamId);

    @Indexed
    @NotNull
    String getChannelId();

    void setChannelId(String channelId);

    @Indexed
    @NotNull
    String getConfigurationGroupId();

    void setConfigurationGroupId(String configurationGroupId);

    @Indexed
    String getName();

    void setName(String name);

    @NotNull
    @Unique
    /**
     * This property is the combination of name and configuration group ID, to ensure uniqueness of name in a
     * configuration group. What it means is we can't define a particular configuration item more than once in a group.
     *
     * We have done this because AO doesn't allow us to easily mark a combination of columns as unique.
     * https://ecosystem.atlassian.net/browse/AO-96
     * This field's value is set by the DAO on insert and update, and it is not exposed anywhere else.
     */
    String getNameUniqueConstraint();

    void setNameUniqueConstraint(String nameUniqueConstraint);

    String getValue();

    void setValue(String value);

    @StringLength(StringLength.UNLIMITED)
    String getValueUnlimited();

    void setValueUnlimited(String value);

    @AutoIncrement
    @NotNull
    @PrimaryKey
    int getId();
}
