package com.atlassian.bitbucket.plugins.slack.notification.configuration.ao;

import net.java.ao.Accessor;
import net.java.ao.Preload;
import net.java.ao.RawEntity;
import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.Table;

//We need to page notifications based on repository IDs, so we issue a 'select REPO_ID distinct' query to
// get the list of distinct repository IDs. This @Preload("REPO_ID") has a side effect, that all the select queries
// now should specify the columns. ALL_FIELDS can be used to retrieve all the fields
@Preload("REPO_ID")
@Table("SLACK_NOTIFICATION")
public interface AoNotificationConfiguration extends RawEntity<Long> {
    String ID_COLUMN = "ID";
    String NOTIFICATION_TYPE_COLUMN = "NOTIFICATION_TYPE";
    String REPO_ID_COLUMN = "REPO_ID";
    String TEAM_ID_COLUMN = "TEAM_ID";
    String CHANNEL_ID_COLUMN = "CHANNEL_ID";
    //@Preload has been set to "REPO_ID", because of this AO will not load all the columns when we issue a
    //select query, so we need to pass on ALL_FIELDS to all the queries where we expect to get all the columns
    String ALL_FIELDS = ID_COLUMN + ',' + NOTIFICATION_TYPE_COLUMN + ',' + REPO_ID_COLUMN + ',' + TEAM_ID_COLUMN + ',' + CHANNEL_ID_COLUMN;

    @AutoIncrement
    @NotNull
    @PrimaryKey(ID_COLUMN)
    long getId();

    @Accessor(NOTIFICATION_TYPE_COLUMN)
    @Indexed
    @NotNull
    String getNotificationType();

    @Accessor(REPO_ID_COLUMN)
    @Indexed
    @NotNull
    int getRepoId();

    @Accessor(TEAM_ID_COLUMN)
    @NotNull
    String getTeamId();

    @Accessor(CHANNEL_ID_COLUMN)
    @NotNull
    String getChannelId();
}
