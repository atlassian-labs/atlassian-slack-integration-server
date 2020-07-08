package com.atlassian.plugins.slack.util;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class LinkHelperTest {
    private static final String BASE_URL = "url";

    @Mock
    private ApplicationProperties applicationProperties;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void absoluteUrl_shouldReturnExpectedValue() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        String result = LinkHelper.absoluteUrl("/test", applicationProperties);
        assertThat(result, is(BASE_URL + "/test"));
    }

    @Test
    public void absoluteUrl_shouldReturnExpectedValueWithSlashInBaseUrl() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL + "/");
        String result = LinkHelper.absoluteUrl("/test", applicationProperties);
        assertThat(result, is(BASE_URL + "/test"));
    }

    @Test
    public void extractUrls_shouldReturnEmptyWhenNoUrlsAreProvided() {
        assertThat(LinkHelper.extractUrls(""), empty());
        assertThat(LinkHelper.extractUrls(" "), empty());
        assertThat(LinkHelper.extractUrls("xyz\\/[]"), empty());
        assertThat(LinkHelper.extractUrls("a.b.c"), empty());
    }

    @Test
    public void extractUrls_shouldReturnExpectedValues() {
        assertThat(LinkHelper.extractUrls("-> 'http://localhost'"),
                containsInAnyOrder("http://localhost"));
        assertThat(LinkHelper.extractUrls("var x=\"http://localhost:8080\""),
                containsInAnyOrder("http://localhost:8080"));
        assertThat(LinkHelper.extractUrls("see link (https://base/confluence/display/TEAM/2019/02/06/Simple+blog)"),
                containsInAnyOrder("https://base/confluence/display/TEAM/2019/02/06/Simple+blog"));
        assertThat(LinkHelper.extractUrls("http://conf-server.aws.com/display/TEAM/Page+1\n" +
                        "http://conf-server.aws.com/x/CAAN\n"),
                containsInAnyOrder(
                        "http://conf-server.aws.com/display/TEAM/Page+1",
                        "http://conf-server.aws.com/x/CAAN"));
        assertThat(LinkHelper.extractUrls("\thttp://http://confl-loadb-w15pplza9760-530579955.us-east-2.elb.amazonaws.com:1990/confluence/pages/viewpage.action?pageId=851986 some id"),
                containsInAnyOrder("http://http://confl-loadb-w15pplza9760-530579955.us-east-2.elb.amazonaws.com:1990/confluence/pages/viewpage.action?pageId=851986"));
        assertThat(
                LinkHelper.extractUrls(
                        "- http://localhost/confluence/display/TEAM/questions/1736742/what-do-i-do-now\n" +
                                "- http://localhost:1990/confluence/display/TEAM/questions/4259851/answers/4259853\n" +
                                "- https://con/confluence/display/TEAM/questions/1736742/what-do-i-do-now\n"
                ),
                containsInAnyOrder(
                        "http://localhost/confluence/display/TEAM/questions/1736742/what-do-i-do-now",
                        "http://localhost:1990/confluence/display/TEAM/questions/4259851/answers/4259853",
                        "https://con/confluence/display/TEAM/questions/1736742/what-do-i-do-now"));
        assertThat(LinkHelper.extractUrls("(https://base/TEAM/2019/02/06/Simple+blog#comment-123)"),
                containsInAnyOrder("https://base/TEAM/2019/02/06/Simple+blog#comment-123"));
        assertThat(LinkHelper.extractUrls("'https://base/TEAM/2019/02/06/Simple+blog?q=1%202&a=#comment-123'"),
                containsInAnyOrder("https://base/TEAM/2019/02/06/Simple+blog?q=1%202&a=#comment-123"));
        assertThat(LinkHelper.extractUrls("<http://hostname/path>'"),
                containsInAnyOrder("http://hostname/path"));
        assertThat(LinkHelper.extractUrls("<http://hostname/path|title>'"),
                containsInAnyOrder("http://hostname/path"));
        assertThat(LinkHelper.extractUrls("<https://wiki.server.tk/questions/884855/a%C3%A7a%C3%AD>'"),
                containsInAnyOrder("https://wiki.server.tk/questions/884855/a%C3%A7a%C3%AD"));
    }

    @Test
    public void decorateWithOrigin_shouldAddQueryParameter() {
        assertThat(LinkHelper.decorateWithOrigin("https://jira.com/jira/browse-123", "issue"),
                is("https://jira.com/jira/browse-123?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258aXNzdWU%3D"));
    }

    @Test
    public void decorateWithOrigin_shouldAddQueryParameterInComplexUrl() {
        assertThat(LinkHelper.decorateWithOrigin("https://jira.com/jira/browse-123?p=1&#123", "issue"),
                is("https://jira.com/jira/browse-123?p=1&atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258aXNzdWU%3D#123"));
    }

    @Test
    public void decorateWithOrigin_shouldReplaceQueryParameterIfAlreadyExists() {
        assertThat(LinkHelper.decorateWithOrigin("https://jira.com/jira/browse-123?p=1&atlLinkOrigin=1#123", "issue"),
                is("https://jira.com/jira/browse-123?p=1&atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258aXNzdWU%3D#123"));
    }

    @Test
    public void decodeOriginType_shouldExtractActualType() {
        assertThat(LinkHelper.decodeOriginType("c2xhY2staW50ZWdyYXRpb258aXNzdWU=").get(), is("issue"));
    }

    @Test
    public void decodeOriginType_shouldReturnEmptyIfParameterIsEmpty() {
        assertThat(LinkHelper.decodeOriginType("").isPresent(), is(false));
    }

    @Test
    public void decodeOriginType_shouldReturnEmptyIfParameterIsNull() {
        assertThat(LinkHelper.decodeOriginType(null).isPresent(), is(false));
    }

    @Test
    public void decodeOriginType_shouldReturnEmptyIfInvalidValue() {
        assertThat(LinkHelper.decodeOriginType("123").isPresent(), is(false));
    }

    @Test
    public void decodeOriginType_shouldReturnEmptyIfTypeNotPresent() {
        assertThat(LinkHelper.decodeOriginType("c2xhY2staW50ZWdyYXRpb258").isPresent(), is(false));
    }
}
