package com.atlassian.plugins.slack.user;

import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.SlackUserDto;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SlackUserService {
    private final UserManager userManager;
    private final SlackUserManager slackUserManager;
    private final SlackLinkManager slackLinkManager;

    @Autowired
    public SlackUserService(final UserManager userManager,
                            final SlackUserManager slackUserManager,
                            final SlackLinkManager slackLinkManager) {
        this.userManager = userManager;
        this.slackUserManager = slackUserManager;
        this.slackLinkManager = slackLinkManager;
    }

    public List<SlackUserDto> getSlackUsersByUsername(final String username) {
        Optional<UserProfile> profile = Optional.ofNullable(userManager.getUserProfile(username));
        Optional<List<SlackUserDto>> slackUsers = profile.map(UserProfile::getUserKey)
                .map(slackUserManager::getByUserKey)
                .map(users -> users.stream()
                        .map(user -> {
                            String teamId = user.getSlackTeamId();
                            String teamName = slackLinkManager.getLinkByTeamId(teamId)
                                    .map(SlackLink::getTeamName)
                                    .rightOr(error -> teamId);
                            return new SlackUserDto(user.getUserKey(), user.getSlackUserId(), teamId, teamName);
                        })
                        .collect(Collectors.toList()));
        return slackUsers.orElse(Collections.emptyList());
    }
}
