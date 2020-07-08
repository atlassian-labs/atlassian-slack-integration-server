package com.atlassian.plugins.slack.api.webhooks;

import com.atlassian.plugins.slack.api.SlackLink;
import lombok.Value;
import lombok.experimental.NonFinal;

/*
https://api.slack.com/slash-commands
token=gIkuvaNzQIHg97ATvDxqgjtO
&team_id=T0001
&team_domain=example
&enterprise_id=E0001
&enterprise_name=Globular%20Construct%20Inc
&channel_id=C2147483705
&channel_name=test
&user_id=U2147483697
&user_name=Steve
&command=/weather
&text=94070
&response_url=https://hooks.slack.com/commands/1234/5678
&trigger_id=13345224609.738474920.8088930838d88f008e0
*/
@Value
@NonFinal
public class SlackSlashCommand {
    String verificationToken;
    String teamId;
    String teamDomain;
    String enterpriseId;
    String enterpriseName;
    String channelId;
    String channelName;
    String userId;
    String userName;
    String commandName;
    String text;
    String responseUrl;
    String triggerId;
    SlackLink slackLink;
}
