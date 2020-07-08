package com.atlassian.plugins.slack.test;

import com.atlassian.plugins.slack.api.ImmutableSlackLink;
import com.atlassian.plugins.slack.api.SlackLink;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class TestTeams {
    public static String DUMMY_TEAM_ID = "DUMMY-T123456";

    public static SlackLink DUMMY_TEAM = new ImmutableSlackLink(
            "DUMMY-CLIENT-ID-123456",
            "secret.123456",
            "verification.token.123456",
            "app.id.123456",
            "app.bp.id.123456",
            "U112233",
            "token.U112233",
            "Test Slack Team",
            DUMMY_TEAM_ID,
            "https://example.com/123456",
            "signing.secret.123456",
            "BU123456",
            "Server Bot",
            "bot.access.token.123456",
            "",
            "");

    public static List<SlackLink> WORKSPACES = Collections.singletonList(DUMMY_TEAM);

    public static Map<String, SlackLink> BY_BOT_TOKEN = WORKSPACES.stream().collect(Collectors
            .toMap(SlackLink::getBotAccessToken, Function.identity()));

    public static Map<String, SlackLink> BY_CLIENT_ID = WORKSPACES.stream().collect(Collectors
            .toMap(SlackLink::getClientId, Function.identity()));

    public static Map<String, SlackLink> BY_TEAM_ID = WORKSPACES.stream().collect(Collectors
            .toMap(SlackLink::getTeamId, Function.identity()));
}
