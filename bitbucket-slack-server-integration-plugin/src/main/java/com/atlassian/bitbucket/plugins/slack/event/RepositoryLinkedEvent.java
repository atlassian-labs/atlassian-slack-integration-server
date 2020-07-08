package com.atlassian.bitbucket.plugins.slack.event;

import com.atlassian.bitbucket.event.repository.RepositoryEvent;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.event.api.AsynchronousPreferred;
import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Event that is raised when a {@link Repository} is linked to a Slack channel.
 */
@AsynchronousPreferred
public class RepositoryLinkedEvent extends RepositoryEvent {
    @Getter
    private final String teamId;
    @Getter
    private final String channelId;

    public RepositoryLinkedEvent(@Nonnull Object source,
                                 @Nonnull Repository repository,
                                 @Nonnull String teamId,
                                 @Nonnull String channelId) {
        super(source, repository);
        this.teamId = teamId;
        this.channelId = channelId;
    }
}
