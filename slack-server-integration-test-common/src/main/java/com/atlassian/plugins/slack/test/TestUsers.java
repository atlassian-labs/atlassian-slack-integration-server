package com.atlassian.plugins.slack.test;

import com.atlassian.plugins.slack.api.ImmutableSlackUser;
import com.atlassian.plugins.slack.api.SlackUser;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM_ID;

@UtilityClass
public class TestUsers {
    public static SlackUser ADMIN_USER = new ImmutableSlackUser(
            "U112233",
            "1",
            DUMMY_TEAM_ID,
            "token.U112233",
            "");

    public static SlackUser REGULAR_USER = new ImmutableSlackUser(
            "U445566",
            "2",
            DUMMY_TEAM_ID,
            "token.U445566",
            "");

    public static List<SlackUser> USERS = Arrays.asList(ADMIN_USER, REGULAR_USER);

    public static Map<String, SlackUser> BY_TOKEN = USERS.stream().collect(Collectors
            .toMap(SlackUser::getUserToken, Function.identity()));

    public static Map<String, SlackUser> BY_ID = USERS.stream().collect(Collectors
            .toMap(SlackUser::getSlackUserId, Function.identity()));

    public static Map<String, String> REAL_NAME_BY_ID = USERS.stream().collect(Collectors
            .toMap(SlackUser::getSlackUserId, u -> "John " + u.getUserKey()));
}
