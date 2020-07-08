package com.atlassian.bitbucket.plugins.slack.profile;

import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugins.slack.rest.model.SlackUserDto;
import com.atlassian.plugins.slack.user.SlackUserService;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

public class UserProfilePageContextProvider implements ContextProvider {
    private final SlackUserService slackUserService;

    public UserProfilePageContextProvider(final SlackUserService slackUserService) {
        this.slackUserService = slackUserService;
    }

    @Override
    public void init(Map<String, String> map) throws PluginParseException {
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> defaultContext) {
        ImmutableMap.Builder<String, Object> context = ImmutableMap.builder();
        context.putAll(defaultContext);

        ApplicationUser currentUser = (ApplicationUser) defaultContext.get("profileUser");
        String currentUserName = currentUser.getName();
        List<SlackUserDto> slackUsers = slackUserService.getSlackUsersByUsername(currentUserName);
        context.put("slackUsers", slackUsers);

        return context.build();
    }
}
