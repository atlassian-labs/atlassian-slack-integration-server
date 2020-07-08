package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import com.atlassian.bitbucket.util.PageRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FileContentHolderTest {
    @Mock
    PageRequest pageRequest;
    @InjectMocks
    FileContentHolder target;

    @BeforeEach
    void setUp() {
        when(pageRequest.getStart()).thenReturn(0);
        when(pageRequest.getLimit()).thenReturn(2);
    }

    @Test
    void onLine_shouldCollectLinesPassed() throws IOException {
        target.onLine(1, "line1", false);
        target.onLine(2, "line2", false);

        List<SimpleLine> lines = target.getLines();
        assertThat(lines, hasSize(2));
        assertThat(lines, containsInAnyOrder(new SimpleLine(1, "line1"), new SimpleLine(2, "line2")));
    }

    @Test
    void onLine_shouldNotCollectExtraLines() throws IOException {
        target.onLine(1, "line1", false);
        target.onLine(2, "line2", false);
        target.onLine(3, "line3", false);

        List<SimpleLine> lines = target.getLines();
        assertThat(lines, hasSize(2));
        assertThat(lines, Matchers.not(new SimpleLine(3, "line3")));
    }

    @Test
    void isBinary_shouldRememberBinaryFlag() throws IOException {
        target.onBinary();

        assertThat(target.isBinary(), is(true));
    }

    @Test
    void isFileStartHit_shouldDetectFirstLine() throws IOException {
        target.onLine(1, "line1", false);

        assertThat(target.isFileEndHit(), is(true));
    }

    @Test
    void isFileEndHit_shouldDetectFirstLine() throws IOException {
        target.onLine(1, "line1", false);
        target.onLine(2, "line2", false);
        target.onLine(3, "line3", false);

        assertThat(target.isFileEndHit(), is(true));
    }
}
