package com.atlassian.plugins.slack.rest.model;

import lombok.Value;
import org.codehaus.jackson.annotate.JsonAutoDetect;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY)
@Value
public class SlackUserDto {
    String userKey;
    String slackUserId;
    String teamId;
    String teamName;
}
