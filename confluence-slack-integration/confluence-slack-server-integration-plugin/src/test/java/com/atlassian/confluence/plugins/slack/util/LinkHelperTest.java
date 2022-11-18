package com.atlassian.confluence.plugins.slack.util;

import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.TinyUrl;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LinkHelperTest {
    private static final String BASE_URL = "url";

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private AbstractPage abstractPage;

    @Test
    public void tinyLink_shouldReturnExpectedValue() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL + "/");
        when(abstractPage.getId()).thenReturn(123L);

        String result = TinyLinkHelper.tinyLink(abstractPage, applicationProperties);
        assertThat(result, is(BASE_URL + "/x/" + new TinyUrl(abstractPage).getIdentifier()));
    }

    @Test
    public void fromTinyLink_shouldReturnExpectedValue() {
        when(abstractPage.getId()).thenReturn(123L);
        String idStr = new TinyUrl(abstractPage).getIdentifier();

        long id = TinyLinkHelper.fromTinyLink(idStr);

        assertThat(id, is(123L));
    }
}
