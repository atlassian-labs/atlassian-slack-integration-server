package com.atlassian.jira.plugins.slack.util;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommentUtilTest {

    @Test
    void testRemoveJiraTagsRemovesValidTags() {
        assertEquals("Some text with a title",
                CommentUtil.removeJiraTags("{panel:title=My Title}Some text with a title{panel}"));
        assertEquals("look ma, red text!",
                CommentUtil.removeJiraTags("{color:red}look ma, red text!{color}"));
        assertEquals("here is quotable content to be quoted",
                CommentUtil.removeJiraTags("{quote}here is quotable content to be quoted{quote}"));
        assertEquals("preformatted piece",
                CommentUtil.removeJiraTags("{noformat}preformatted piece{noformat}"));
        assertEquals("some text",
                CommentUtil.removeJiraTags("{code:title=Bar.java|borderStyle=solid}some text{code}"));
    }


    @Test
    void testRemoveJiraTagsDoesNotRemoveInvalidTags() {
        assertEquals("{panell:title=My Title}Some text with a title",
                CommentUtil.removeJiraTags("{panell:title=My Title}Some text with a title{panel}"));
        assertEquals("{color }look ma, red text!",
                CommentUtil.removeJiraTags("{color }look ma, red text!{color}"));
        assertEquals("here is quotable content to be quoted{q1uote}",
                CommentUtil.removeJiraTags("{quote}here is quotable content to be quoted{q1uote}"));
        assertEquals("{noformat:}preformatted piece",
                CommentUtil.removeJiraTags("{noformat:}preformatted piece{noformat}"));
        assertEquals("some text{coode}",
                CommentUtil.removeJiraTags("{code:title=Bar.java|borderStyle=solid}some text{coode}"));
        assertEquals("{notsupperted}message{notsupported}",
                CommentUtil.removeJiraTags("{notsupperted}message{notsupported}"));
        assertEquals("", CommentUtil.removeJiraTags(null));
    }

}