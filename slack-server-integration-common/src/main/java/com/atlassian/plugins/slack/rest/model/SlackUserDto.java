package com.atlassian.plugins.slack.rest.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY)
@Value
public class SlackUserDto {
    String userKey;
    String slackUserId;
    String teamId;
    String teamName;
}
