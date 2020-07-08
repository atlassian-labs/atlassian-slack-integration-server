package com.atlassian.confluence.plugins.slack.spacetochannel.model;

import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@ExtendWith(MockitoExtension.class)
public class ModelGeneralTest {
    @Mock
    private AbstractPage abstractPage;
    @Mock
    private Space space;
    @Mock
    private ConfluenceUser confluenceUser;
    @Mock
    private EventInfo eventInfo;

    @Test
    public void testAbstractPageEvent() {
        AbstractPageEvent target1 = new AbstractPageEvent(abstractPage, EventType.CREATE, PageType.BLOG, "link");
        AbstractPageEvent target2 = new AbstractPageEvent(abstractPage, EventType.CREATE, PageType.BLOG, "link");
        AbstractPageEvent target3 = new AbstractPageEvent(abstractPage, EventType.CREATE, PageType.PAGE, "link");

        assertThat(target1.equals(target2), is(true));
        assertThat(target1.equals(target3), is(false));
        assertThat(target1.hashCode(), is(target2.hashCode()));
        assertThat(target1.hashCode(), not(is(target3.hashCode())));
        assertThat(target1.toString(), containsString("space="));
    }

    @Test
    public void testConfluenceCalendarEventCreatedEvent() {
        ConfluenceCalendarEventCreatedEvent target1 = new ConfluenceCalendarEventCreatedEvent(eventInfo, space, "link");
        ConfluenceCalendarEventCreatedEvent target2 = new ConfluenceCalendarEventCreatedEvent(eventInfo, space, "link");
        ConfluenceCalendarEventCreatedEvent target3 = new ConfluenceCalendarEventCreatedEvent(eventInfo, space, "link2");

        assertThat(target1.equals(target2), is(true));
        assertThat(target1.equals(target3), is(false));
        assertThat(target1.hashCode(), is(target2.hashCode()));
        assertThat(target1.hashCode(), not(is(target3.hashCode())));
        assertThat(target1.toString(), containsString("space="));
    }

    @Test
    public void testContentSharedEvent() {
        ContentSharedEvent target1 = new ContentSharedEvent(space, "T", "C", "ts", null);
        ContentSharedEvent target2 = new ContentSharedEvent(space, "T", "C", "ts", null);
        ContentSharedEvent target3 = new ContentSharedEvent(space, "T", "C", "ts2", null);

        assertThat(target1.equals(target2), is(true));
        assertThat(target1.equals(target3), is(false));
        assertThat(target1.hashCode(), is(target2.hashCode()));
        assertThat(target1.hashCode(), not(is(target3.hashCode())));
        assertThat(target1.toString(), containsString("space="));
    }

    @Test
    public void testQuestionEvent() {
        QuestionEvent target1 = new QuestionEvent(space, confluenceUser, QuestionType.ANSWER, "l", null);
        QuestionEvent target2 = new QuestionEvent(space, confluenceUser, QuestionType.ANSWER, "l", null);
        QuestionEvent target3 = new QuestionEvent(space, confluenceUser, QuestionType.QUESTION, "l", null);

        assertThat(target1.equals(target2), is(true));
        assertThat(target1.equals(target3), is(false));
        assertThat(target1.hashCode(), is(target2.hashCode()));
        assertThat(target1.hashCode(), not(is(target3.hashCode())));
        assertThat(target1.toString(), containsString("space="));
    }

    @Test
    public void testSpaceResult() {
        SpaceResult target1 = new SpaceResult("n", "k");
        SpaceResult target2 = new SpaceResult("n", "k");
        SpaceResult target3 = new SpaceResult("n", "k2");

        assertThat(target1.equals(target2), is(true));
        assertThat(target1.equals(target3), is(false));
        assertThat(target1.hashCode(), is(target2.hashCode()));
        assertThat(target1.hashCode(), not(is(target3.hashCode())));
        assertThat(target1.toString(), containsString("key="));
    }

    @Test
    public void testEventInfo() {
        EventInfo target1 = new EventInfo(confluenceUser, "s", "t", "c", "d", "n", null, 0L, 0L, "tz", true);
        EventInfo target2 = new EventInfo(confluenceUser, "s", "t", "c", "d", "n", null, 0L, 0L, "tz", true);
        EventInfo target3 = new EventInfo(confluenceUser, "s2", "t", "c", "d", "n", null, 0L, 0L, "tz", true);

        assertThat(target1.equals(target2), is(true));
        assertThat(target1.equals(target3), is(false));
        assertThat(target1.hashCode(), is(target2.hashCode()));
        assertThat(target1.hashCode(), not(is(target3.hashCode())));
        assertThat(target1.toString(), containsString("spaceKey="));
    }
}
