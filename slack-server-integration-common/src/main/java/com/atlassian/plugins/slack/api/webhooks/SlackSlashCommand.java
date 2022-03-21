package com.atlassian.plugins.slack.api.webhooks;

import com.atlassian.plugins.slack.api.SlackLink;
import lombok.Value;
import lombok.experimental.NonFinal;

/*
https://api.slack.com/slash-commands
token:                 dr7HGN2lp78sLKMNBVfEsd0u
team_id:               TT0EEPP4R
team_domain:           domain
channel_id:            C10P0P00303
channel_name:          name-channel
user_id:               U11PIPLPPSM
user_name:             username
command:               /commane
text:
api_app_id:            A00T0R11P66
is_enterprise_install: false
response_url:          https://hooks.slack.com/commands/TC3NTTN4R/2839125455296/L0Tb3Bh7qvoVQECQt3jEQ3TE
trigger_id:            2768768122361.413245683425.c7h9k2vd47xl478df2sa4kf333lkj193
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
