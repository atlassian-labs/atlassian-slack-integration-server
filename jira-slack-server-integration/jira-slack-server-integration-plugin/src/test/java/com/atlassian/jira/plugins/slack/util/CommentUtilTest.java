package com.atlassian.jira.plugins.slack.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

class CommentUtilTest {
    @ParameterizedTest
    @MethodSource("validTagsProvider")
    void testRemoveJiraTagsRemovesValidTags(String argument) {
        assertEquals("some text", CommentUtil.removeJiraTags(argument));
    }

    static Stream<String> validTagsProvider() {
        return Stream.of("{panel:title=My Title}some text{panel}",
                "{color:red}some text{color}",
                "{quote}some text{quote}",
                "{noformat}some text{noformat}",
                "{code:title=Bar.java|borderStyle=solid}some text{code}");
    }

    @ParameterizedTest
    @MethodSource("invalidTagsProvider")
    void testRemoveJiraTagsDoesNotRemoveInvalidTags(String argument) {
        assertEquals(argument, CommentUtil.removeJiraTags(argument));
    }

    static Stream<String> invalidTagsProvider() {
        return Stream.of("{panell:title=My Title}some text{panell}",
                "{colr:red}some text{colrr}",
                "{quote }some text{quotee}",
                "{noformat:}some text{noformat:}",
                "{code :title=Bar.java|borderStyle=solid}some text{cod}");
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void test_this() {
        assertEquals("{{{{{{{{{{{{{{{{{{{{{{{}}}}}}}}}}}}}}}}}}}}}}}}text",
                CommentUtil.removeJiraTags("{{{{{{{{{{{{{{{{{{{{{{{{color}}}}}}}}}}}}}}}}}}}}}}}}}text"));
        assertEquals("{quote{quote{quote{quote{quote{quote{quote{quote}}}}}text",
                CommentUtil.removeJiraTags("{quote{quote{quote{quote{quote{quote{quote{quote{quote}}}}}}text{quote}"));
    }

}