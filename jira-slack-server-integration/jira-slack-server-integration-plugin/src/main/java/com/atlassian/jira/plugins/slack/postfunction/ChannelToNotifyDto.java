package com.atlassian.jira.plugins.slack.postfunction;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;

public class ChannelToNotifyDto {
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builderWithJackson2Defaults().build();
    private static final TypeReference<List<ChannelToNotifyDto>> listType = new TypeReference<List<ChannelToNotifyDto>>() {
    };

    static List<ChannelToNotifyDto> fromJson(String json) throws JacksonException {
        return OBJECT_MAPPER.readValue(json, listType);
    }

    static String toJson(List<ChannelToNotifyDto> list) throws JacksonException {
        return OBJECT_MAPPER.writeValueAsString(list);
    }

    private String teamId;
    private String channelId;

    public ChannelToNotifyDto() {
    }

    ChannelToNotifyDto(final String teamId, final String channelId) {
        this.teamId = teamId;
        this.channelId = channelId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(final String teamId) {
        this.teamId = teamId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(final String channelId) {
        this.channelId = channelId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ChannelToNotifyDto)) return false;
        final ChannelToNotifyDto that = (ChannelToNotifyDto) o;
        return Objects.equals(teamId, that.teamId) &&
                Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, channelId);
    }
}
