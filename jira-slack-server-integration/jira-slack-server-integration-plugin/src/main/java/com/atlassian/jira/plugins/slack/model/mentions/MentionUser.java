package com.atlassian.jira.plugins.slack.model.mentions;

import com.atlassian.jira.plugins.slack.model.UserId;
import com.atlassian.jira.plugins.slack.model.UserIdImpl;
import com.atlassian.jira.plugins.slack.storage.cache.CacheableEntity;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.seratch.jslack.api.model.User;
import jakarta.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;
import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = PUBLIC_ONLY)
public class MentionUser implements CacheableEntity<UserId> {
    static final MentionUser DELETED_USER = new MentionUser(new User());

    private final UserId userId;
    private final User user;

    public MentionUser(@Nonnull User user) {
        this.user = checkNotNull(user);
        this.userId = new UserIdImpl(user.getTeamId(), user.getId());
    }

    @Override
    public UserId getKey() {
        return userId;
    }

    public String getId() {
        return user.getId();
    }

    public String getName() {
        return user.getName();
    }

    public String getTeamId() {
        return user.getTeamId();
    }

    public boolean isBot() {
        return user.isBot();
    }

    public String getDisplayName() {
        return user.getRealName();
    }

    public String getAvatarHash() {
        return user.getProfile().getAvatarHash();
    }

    public String getIcon() {
        return user.getProfile().getImage48();
    }
}
