package com.atlassian.plugins.slack.test;

import com.github.seratch.jslack.api.model.Conversation;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class TestChannels {
    public static Conversation PUBLIC = Conversation.builder()
            .id("C654321")
            .name("simple-public-channel")
            .build();

    public static Conversation PRIVATE = Conversation.builder()
            .id("PC654321")
            .name("simple-private-channel")
            .isPrivate(true)
            .build();

    public static Conversation DIRECT = Conversation.builder()
            .id("DM654321")
            .name("simple-dm-channel")
            .user(TestUsers.USERS.get(0).getSlackUserId())
            .isPrivate(false)
            .isIm(true)
            .build();

    public static List<Conversation> CHANNELS = Arrays.asList(PUBLIC, PRIVATE, DIRECT);

    public static Map<String, Conversation> BY_ID = CHANNELS.stream().collect(Collectors
            .toMap(Conversation::getId, Function.identity()));

    public static Map<Set<String>, Conversation> DM_BY_USER_ID = CHANNELS.stream()
            .filter(Conversation::isIm)
            .collect(Collectors.toMap(c -> Collections.singleton(c.getUser()), Function.identity()));
}
