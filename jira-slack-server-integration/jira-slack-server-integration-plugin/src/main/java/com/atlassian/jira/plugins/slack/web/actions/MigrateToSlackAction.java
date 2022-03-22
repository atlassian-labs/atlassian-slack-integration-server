package com.atlassian.jira.plugins.slack.web.actions;

import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.web.action.ActionViewDataMappings;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Provides context for migrate to Slack channel selection dialog.
 */
@SupportedMethods(RequestMethod.GET)
public class MigrateToSlackAction extends JiraWebActionSupport {
    private String roomName;

    @Override
    protected String doExecute() {
        return SUCCESS;
    }

    @ActionViewDataMappings({SUCCESS})
    public Map<String, Object> getData() {
        return ImmutableMap.<String, Object>builder()
                .put("roomName", roomName)
                .build();
    }

    @SuppressWarnings("unused")
    public String getRoomName() {
        return roomName;
    }

    @SuppressWarnings("unused")
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}
