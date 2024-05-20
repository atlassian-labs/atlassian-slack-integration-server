package com.atlassian.plugins.slack.rest;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.client.DefaultSlackClient;
import com.atlassian.plugins.slack.api.events.SigningSecretVerificationFailedEvent;
import com.atlassian.plugins.slack.api.events.SigningSecretVerificationFailedEvent.Cause;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static com.github.seratch.jslack.app_backend.SlackSignature.HeaderNames.X_SLACK_REQUEST_TIMESTAMP;
import static com.github.seratch.jslack.app_backend.SlackSignature.HeaderNames.X_SLACK_SIGNATURE;

@Slf4j
@Provider
@SlackSignatureVerifying
public class SlackSignatureVerifyingFilter implements ContainerRequestFilter {
    public static final String TEAM_ID = "team_id";
    public static final String SLACK_ACTION_PAYLOAD = "payload";
    public static final int MAX_REQUEST_DELAY_MINUTES = 6;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SlackLinkManager slackLinkManager;
    private final EventPublisher eventPublisher;
    private final RequestHolder requestHolder;
    private final SlackSettingService slackSettingService;
    private final AnalyticsContextProvider analyticsContextProvider;

    private final boolean skipVerification;

    @Inject
    public SlackSignatureVerifyingFilter(final SlackLinkManager slackLinkManager,
                                         final EventPublisher eventPublisher,
                                         final RequestHolder requestHolder,
                                         final SlackSettingService slackSettingService,
                                         final AnalyticsContextProvider analyticsContextProvider) {
        this.slackLinkManager = slackLinkManager;
        this.eventPublisher = eventPublisher;
        this.requestHolder = requestHolder;
        this.slackSettingService = slackSettingService;
        this.analyticsContextProvider = analyticsContextProvider;
        this.skipVerification = "true".equals(System.getProperty("slack.addon.skip.signing"));
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        if (skipVerification) {
            return;
        }

        String contentTypeStr = request.getHeaderString(HttpHeaders.CONTENT_TYPE);
        MediaType contentType = parseContentType(contentTypeStr);

        String requestTimestamp = request.getHeaderString(X_SLACK_REQUEST_TIMESTAMP);
        String actualSignature = request.getHeaderString(X_SLACK_SIGNATURE);
        validateTimestamp(requestTimestamp);

        // fail verification immediately if cached request not found
        CachingServletRequestWrapper cachedRequest = requestHolder.getAndRemove(actualSignature).orElse(null);
        if (cachedRequest == null) {
            failVerification(new SecurityException("Cached request is not found."), null, Cause.OTHER);
        }

        // teamId and payload are extracted differently for each content type
        if (MediaType.APPLICATION_JSON_TYPE.isCompatible(contentType)) {
            String requestPayload = new String(cachedRequest.getBody(), StandardCharsets.UTF_8);
            Optional<JsonNode> payloadJson = parseJson(requestPayload);
            Optional<String> teamId = payloadJson
                    .map(node -> node.path(TEAM_ID).asText())
                    .filter(StringUtils::isNotBlank);

            // it's dummy team from integration tests; skip verification for it
            boolean isDummyTeam = teamId.map(id -> id.startsWith(DefaultSlackClient.TEAM_DUMMY_PREFIX)).orElse(false);

            // do not check signature on 'url_verification' request since it is triggered during
            // the team connection process; so database may not have corresponding SlackLink yet
            Optional<String> type = payloadJson.map(node -> node.path(SlackWebHookResource.EVENT_TYPE).asText());
            boolean isInitialRequest = type.map(SlackWebHookResource.TYPE_URL_VERIFICATION::equals).orElse(false);
            if (!isInitialRequest && !isDummyTeam) {
                if (teamId.isPresent()) {
                    validateSignature(teamId.get(), requestPayload, requestTimestamp, actualSignature);
                } else {
                    failVerification(new SecurityException("Team ID is not found."), null, Cause.NO_TEAM_ID);
                }
            }
        } else if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(contentType)) {
            Map<String, String[]> formParams = cachedRequest.getFormParams();
            Optional<String> teamId = getTeamIdFromFormPayload(formParams);
            if (teamId.isPresent()) {
                String requestPayload = formParams.keySet().stream()
                        .map(key -> key + "=" + encodeParam(getFirst(formParams, key).orElse(null)))
                        .collect(Collectors.joining("&"));
                validateSignature(teamId.get(), requestPayload, requestTimestamp, actualSignature);
            } else {
                failVerification(new SecurityException("Team ID is not found."), null, Cause.NO_TEAM_ID);
            }
        } else {
            String msg = String.format("Invalid content type: %s.", contentTypeStr);
            failVerification(new SecurityException(msg), null, Cause.INVALID_CONTENT_TYPE);
        }

        // validation passed, so it's original Slack request; it means that instance is accessible
        // from the internet and it is safe to enable interactive features like in-Slack comment dialog
        if (!slackSettingService.isInstancePublic()) {
            slackSettingService.setInstancePublic(true);
        }
    }

    private MediaType parseContentType(final String contentTypeStr) {
        try {
            return MediaType.valueOf(contentTypeStr);
        } catch (IllegalArgumentException e) {
            failVerification(new SecurityException("Failed to parse content type: " + contentTypeStr), null, Cause.OTHER);
            return null; // will never be reached because the preceding method throws an exception
        }
    }

    private Optional<String> getTeamIdFromFormPayload(final Map<String, String[]> formParams) {
        Optional<String> teamId = getFirst(formParams, TEAM_ID);
        if (!teamId.isPresent()) {
            Optional<String> payload = getFirst(formParams, SLACK_ACTION_PAYLOAD);
            teamId = payload.flatMap(this::parseJson)
                    .map(node -> node.path("team").path("id").asText())
                    .filter(StringUtils::isNotBlank);
        }
        return teamId;
    }

    private void validateSignature(final String teamId,
                                   final String requestPayload,
                                   final String requestTimestamp,
                                   final String actualSignature) {
        slackLinkManager.getLinkByTeamId(teamId).toOptional()
                .ifPresent(link -> {
                    String signingSecret = link.getSigningSecret();
                    // https://api.slack.com/docs/verifying-requests-from-slack
                    // string to hash - v0:timestamp:payload
                    String payloadToHash = new StringJoiner(":")
                            .add("v0")
                            .add(requestTimestamp)
                            .add(requestPayload)
                            .toString();
                    String expectedSignature = "v0=" + encodeHmacSha256(signingSecret, payloadToHash);
                    if (!Objects.equals(expectedSignature, actualSignature)) {
                        failVerification(new SecurityException("Request signature verification failed."),
                                teamId, Cause.BAD_SIGNATURE);
                    }
                });
    }

    private void validateTimestamp(final String requestTimestampString) {
        try {
            Instant requestTimestamp = Instant.ofEpochSecond(Long.parseLong(requestTimestampString));
            Instant now = Instant.now();
            Instant oldestAllowedTimestamp = now.minus(MAX_REQUEST_DELAY_MINUTES, ChronoUnit.MINUTES);
            if (requestTimestamp.isBefore(oldestAllowedTimestamp)) {
                failVerification(new SecurityException("Request has expired."), null, Cause.REQUEST_EXPIRED);
            }
        } catch (NumberFormatException | DateTimeException e) {
            failVerification(new SecurityException("Request timestamp verification failed", e), null, Cause.OTHER);
        }
    }

    private String encodeParam(final String param) {
        String encoded = param;
        try {
            encoded = URLEncoder.encode(param, "utf-8")
                    // java encoder does not replace asterisk with escape code, but Slack - does
                    // https://docs.oracle.com/javase/8/docs/api/java/net/URLEncoder.html
                    .replace("*", "%2A");
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
        return encoded;
    }

    private Optional<JsonNode> parseJson(final String requestPayload) {
        JsonNode payloadNode = null;
        try {
            payloadNode = OBJECT_MAPPER.readTree(requestPayload);
        } catch (IOException e) {
            // ignore
        }

        return Optional.ofNullable(payloadNode);
    }

    private Optional<String> getFirst(final Map<String, String[]> map, final String key) {
        String[] valueArray = map.get(key);
        String value = null;
        if (!ArrayUtils.isEmpty(valueArray)) {
            value = valueArray[0];
        }
        return Optional.ofNullable(value);
    }

    private String encodeHmacSha256(final String key, final String data) {
        String encodedPayloadHex = null;
        try {
            Mac encoder = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            encoder.init(secretKey);
            byte[] encodedBytes = encoder.doFinal(data.getBytes(StandardCharsets.UTF_8));
            encodedPayloadHex = Hex.encodeHexString(encodedBytes);
        } catch (Exception e) {
            log.error("Failed to encode payload", e);
        }
        return encodedPayloadHex;
    }

    private void failVerification(final SecurityException exception,
                                  final String teamId,
                                  final Cause cause) throws SecurityException {

        AnalyticsContext analyticsContext = analyticsContextProvider.byTeamId(teamId);
        eventPublisher.publish(new SigningSecretVerificationFailedEvent(analyticsContext, cause));
        throw exception;
    }
}
