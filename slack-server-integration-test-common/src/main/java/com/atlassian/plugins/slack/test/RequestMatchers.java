package com.atlassian.plugins.slack.test;

import com.atlassian.plugins.slack.test.mockserver.RequestHistoryItem;
import com.github.seratch.jslack.api.methods.SlackApiRequest;
import lombok.experimental.UtilityClass;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultString;

@UtilityClass
public class RequestMatchers {
    public static Matcher<List<RequestHistoryItem>> hasHit(
            String endpoint,
            Matcher<? super List<RequestHistoryItem>> requestMatcher) {
        return new BaseMatcher<List<RequestHistoryItem>>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object history) {
                List<RequestHistoryItem> map = (List<RequestHistoryItem>) history;
                List<RequestHistoryItem> items = map.stream()
                        .filter(item -> Objects.equals(item.apiMethod(), endpoint))
                        .collect(Collectors.toList());
                return !items.isEmpty() && requestMatcher.matches(items);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Request history contains requests to endpoint '");
                description.appendValue(endpoint);
                description.appendText("' and such requests match ");
                description.appendDescriptionOf(requestMatcher);
            }
        };
    }

    public static Matcher<RequestHistoryItem> request(Matcher<SlackApiRequest> requestMatcher) {
        return new BaseMatcher<RequestHistoryItem>() {
            @Override
            public boolean matches(final Object item) {
                final SlackApiRequest request = ((RequestHistoryItem) item).parsedEntity();
                return request != null && requestMatcher.matches(request);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Request ");
                description.appendDescriptionOf(requestMatcher);
            }
        };
    }

    public static Matcher<RequestHistoryItem> body(Matcher<String> textMatcher) {
        return new BaseMatcher<RequestHistoryItem>() {
            @Override
            public boolean matches(final Object item) {
                return textMatcher.matches(((RequestHistoryItem) item).body());
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Request body ");
                description.appendDescriptionOf(textMatcher);
            }
        };
    }

    public static <T extends SlackApiRequest, V> Matcher<RequestHistoryItem> requestEntityProperty(Function<T, V> expectedEntity, Matcher<V> propertyMatcher) {
        return new BaseMatcher<RequestHistoryItem>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object item) {
                final T entity = (T) ((RequestHistoryItem) item).parsedEntity();
                return entity != null && propertyMatcher.matches(expectedEntity.apply(entity));
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Request property ");
                description.appendDescriptionOf(propertyMatcher);
            }
        };
    }

    public static Matcher<Response> codeAndBody(int code, Matcher<String> bodyMatcher) {
        return new BaseMatcher<Response>() {
            @Override
            public boolean matches(final Object response) {
                if (response instanceof Response) {
                    final Response typedResponse = (Response) response;
                    if (typedResponse.code() != code) {
                        return false;
                    }
                    try (ResponseBody body = typedResponse.body()) {
                        final String bodyStr = body != null ? defaultString(body.string()) : null;
                        final boolean matches = bodyMatcher.matches(bodyStr);
                        if (!matches) {
                            System.out.println("Body did not match: " + bodyStr);
                        }
                        return matches;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return false;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Response with status code ");
                description.appendValue(code);
                description.appendText(" and body ");
                description.appendDescriptionOf(bodyMatcher);
            }
        };
    }

    public static Matcher<Response> success(Matcher<String> bodyMatcher) {
        return codeAndBody(200, bodyMatcher);
    }
}
