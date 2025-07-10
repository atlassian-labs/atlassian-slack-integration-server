package com.atlassian.confluence.plugins.slack.util;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Impersonates Confluence user to perform some action that requires user in a global auth context.
 * <p>
 * https://community.atlassian.com/t5/Answers-Developer-Questions/How-to-execute-web-actions-from-a-plugin-task-with-a-particular/qaq-p/545612
 */
@Component
@Slf4j
public class ConfluenceUserImpersonator {
    public <T> T impersonate(@Nonnull final ConfluenceUser user,
                             @Nonnull final Supplier<T> action,
                             @Nonnull final String cause) {
        // remember this value so it can be restored later
        ConfluenceUser originalCurrentUser = AuthenticatedUserThreadLocal.get();
        try {
            Optional<ConfluenceUser> originalCurrentUserOptional = Optional.ofNullable(originalCurrentUser);
            log.debug("Impersonating user [{}({})] to perform [{}]. Current user: [{}({})]", user.getKey(), user.getName(),
                    cause, originalCurrentUserOptional.map(ConfluenceUser::getKey).orElse(null),
                    originalCurrentUserOptional.map(ConfluenceUser::getName).orElse(null));
            AuthenticatedUserThreadLocal.set(user);

            return action.get();
        } finally {
            // restore previously logged in user
            AuthenticatedUserThreadLocal.set(originalCurrentUser);
        }
    }
}
