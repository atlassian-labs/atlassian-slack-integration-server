package com.atlassian.plugins.slack.util;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;

@Slf4j
@UtilityClass
public class LinkHelper {
    public static final String ATL_LINK_ORIGIN = "atlLinkOrigin";

    public static final Pattern LINK_REGEX = Pattern.compile(
            "\\b(?<url>(?:(?:https?)://)[-A-Z0-9+&@#/%?=~_$!:,.;]*[-A-Z0-9+&@#/%=~_$])\\b" +
                    "|\\((?<purl>(?:(?:https?)://)[^)\\r\\n]+)\\)?" +
                    "|\"(?<dqurl>(?:(?:https?)://)[^\"\\r\\n]+)\"?" +
                    "|'(?<squrl>(?:(?:https?)://)[^'\\r\\n]+)'?",
            Pattern.CASE_INSENSITIVE);

    public static String absoluteUrl(final String relativeUrl, final ApplicationProperties applicationProperties) {
        final String baseUrl = applicationProperties.getBaseUrl(UrlMode.CANONICAL);
        return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + relativeUrl;
    }

    public static List<String> extractUrls(final String text) {
        final Matcher matcher = LINK_REGEX.matcher(text);
        final List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(ObjectUtils.firstNonNull(
                    matcher.group("url"),
                    matcher.group("purl"),
                    matcher.group("dqurl"),
                    matcher.group("squrl")));
        }
        return result;
    }

    public static String decorateWithOrigin(final String originalUri, final String type) {
        try {
            final String encodedOrigin = Base64.getEncoder().encodeToString(("slack-integration|" + type).getBytes(UTF_8));
            return UriBuilder.fromUri(originalUri)
                    .replaceQueryParam(ATL_LINK_ORIGIN, encodedOrigin)
                    .build()
                    .toString();
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return originalUri;
        }
    }

    public static Optional<String> decodeOriginType(final String value) {
        if (isBlank(value)) {
            return Optional.empty();
        }
        try {
            final String decodedValue = new String(Base64.getDecoder().decode(value), UTF_8);
            final String type = substringAfter(decodedValue, "|");
            return !isBlank(type) ? Optional.of(type) : Optional.empty();
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return Optional.empty();
        }
    }
}
