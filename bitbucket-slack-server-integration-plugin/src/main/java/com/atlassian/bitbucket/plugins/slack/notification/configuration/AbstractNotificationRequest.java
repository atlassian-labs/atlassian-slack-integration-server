package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.util.BuilderSupport;
import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Request for searching Slack notification settings
 */
@EqualsAndHashCode
public abstract class AbstractNotificationRequest {
    private final Optional<Repository> repository;

    protected AbstractNotificationRequest(AbstractBuilder builder) {
        repository = builder.repository;
    }

    /**
     * Retrieves the {@link Repository}
     *
     * @return the repository
     */
    @Nonnull
    public Optional<Repository> getRepository() {
        return repository;
    }

    public abstract static class AbstractBuilder<B extends AbstractBuilder<B>> extends BuilderSupport {

        protected Optional<Repository> repository = Optional.empty();

        @Nonnull
        public B repository(@Nonnull Repository value) {
            repository = Optional.of(checkNotNull(value, "repository"));
            return self();
        }

        @Nonnull
        public abstract B self();
    }
}
