package com.atlassian.bitbucket.plugins.slack.notification;

import static com.atlassian.bitbucket.plugins.slack.notification.PullRequestNotificationTypes.RESCOPED;

import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class NotificationUtil {
    public static Set<String> ALL_NOTIFICATION_TYPE_KEYS = Collections.unmodifiableSet(Stream
            .of(
                    Stream.of(PullRequestNotificationTypes.values())
                        .filter(type -> RESCOPED != type)
                        .map(PullRequestNotificationTypes::getKey),
                    Stream.of(RepositoryNotificationTypes.values()).map(RepositoryNotificationTypes::getKey),
                    Stream.of(TaskNotificationTypes.values()).map(TaskNotificationTypes::getKey)
            )
            .flatMap(Function.identity())
            .collect(Collectors.toSet()));
}
