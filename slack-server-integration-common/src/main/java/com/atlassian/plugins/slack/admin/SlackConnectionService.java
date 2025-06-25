package com.atlassian.plugins.slack.admin;

import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackLinkDto;
import com.atlassian.plugins.slack.util.ErrorResponse;
import io.atlassian.fugue.Either;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

public interface SlackConnectionService {
    Either<ErrorResponse, InstallationCompletionData> connectTeam(SlackLink slackLink, String teamId);

    Either<ErrorResponse, Boolean> disconnectTeam(String teamId);

    void enrichSlackLink(SlackLinkDto dto) throws Exception;

    Either<ErrorResponse, InstallationCompletionData> connectTeamViaOAuth(String code);

    URI redirectFromOAuth2Installation(Either<ErrorResponse, InstallationCompletionData> result,
                                       HttpServletRequest request);
}
