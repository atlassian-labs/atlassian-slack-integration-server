package com.atlassian.jira.plugins.slack.model.ao;

import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import net.java.ao.Accessor;
import net.java.ao.Mutator;
import net.java.ao.RawEntity;
import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

/**
 * This object will represent a configuration item that maps a project to a channel.
 */
@Table("PROJECT_CONFIG")
public interface ProjectConfigurationAO extends ProjectConfiguration, RawEntity<Integer> {
    // https://developer.atlassian.com/display/DOCS/Column+names
    String COLUMN_ID = "ID";
    String COLUMN_PROJECT_ID = "PROJECT_ID";
    String COLUMN_TEAM_ID = "TEAM_ID";
    String COLUMN_CHANNEL_ID = "CHANNEL_ID";
    String COLUMN_CONFIGURATION_GROUP_ID = "CONFIGURATION_GROUP_ID";
    String COLUMN_NAME = "NAME";
    String COLUMN_NAME_UNIQUE_CONSTRAINT = "NAME_UNIQUE_CONSTRAINT";
    String COLUMN_VALUE = "VALUE_UNLIMITED";

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

    @StringLength(StringLength.UNLIMITED)
    @Accessor(COLUMN_VALUE)
    String getValue();

    @Mutator(COLUMN_VALUE)
    void setValue(String value);

    @AutoIncrement
    @NotNull
    @PrimaryKey
    int getId();
}
