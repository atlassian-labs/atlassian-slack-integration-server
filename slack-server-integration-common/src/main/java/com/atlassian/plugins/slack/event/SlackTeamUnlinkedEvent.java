package com.atlassian.plugins.slack.event;

import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class SlackTeamUnlinkedEvent {
    String teamId;
}
