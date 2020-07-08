package com.atlassian.plugins.slack.ao;

import com.atlassian.plugins.slack.api.SlackUser;
import net.java.ao.Preload;
import net.java.ao.RawEntity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.Table;
import org.codehaus.jackson.annotate.JsonIgnore;

@Table("AOSLACK_USER")
@Preload
public interface AOSlackUser extends SlackUser, RawEntity<String> {
    @NotNull
    @PrimaryKey
    String getSlackUserId();

    void setSlackUserId(String userId);

    @Indexed
    String getUserKey();

    void setUserKey(String userKey);

    @Indexed
    String getSlackTeamId();

    void setSlackTeamId(String slackTeamId);

    @JsonIgnore
    String getUserToken();

    void setUserToken(String userToken);

    @Override
    String getConnectionError();

    void setConnectionError(String connectionError);
}
