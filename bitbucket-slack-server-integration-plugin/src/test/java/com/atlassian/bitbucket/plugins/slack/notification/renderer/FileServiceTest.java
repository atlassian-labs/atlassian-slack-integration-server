package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.content.ContentTreeNode.Type;
import com.atlassian.bitbucket.content.FileContentCallback;
import com.atlassian.bitbucket.plugins.slack.model.LineRange;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.util.PageRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FileServiceTest {
    public static final String COMMIT_ID = "commit-id";
    public static final String FILE_PATH = "file-path";

    @Mock
    Repository repository;
    @Mock
    ContentService contentService;

    @Captor
    ArgumentCaptor<PageRequest> pageRequestCaptor;
    @Captor
    ArgumentCaptor<FileContentHolder> contentHolderCaptor;

    @InjectMocks
    FileService target;

    @Test
    void getType_shouldDelegateCallToContentService() {
        when(contentService.getType(eq(repository), eq(COMMIT_ID), eq(FILE_PATH))).thenReturn(Type.FILE);

        Type type = target.getType(repository, COMMIT_ID, FILE_PATH);

        assertThat(type, Matchers.is(Type.FILE));
        verify(contentService).getType(repository, COMMIT_ID, FILE_PATH);
    }

    @Test
    void loadFileContent_shouldConstructContentHolderWithCorrectPageRequest() {
        int fromLine = 3;
        int toLine = 5;

        target.loadFileContent(repository, COMMIT_ID, FILE_PATH, Collections.singletonList(new LineRange(fromLine, toLine)));

        verify(contentService).streamFile(eq(repository), eq(COMMIT_ID), eq(FILE_PATH), pageRequestCaptor.capture(),
                eq(false), contentHolderCaptor.capture());
        PageRequest request = pageRequestCaptor.getValue();
        assertThat(request.getStart(), equalTo(fromLine - 1));
        assertThat(request.getLimit(), equalTo(toLine - fromLine + 2));

        FileContentHolder holder = contentHolderCaptor.getValue();
        assertThat(holder.getPageRequest().getStart(), equalTo(fromLine - 1));
        assertThat(holder.getPageRequest().getLimit(), equalTo(toLine - fromLine + 1));
    }

    @Test
    void loadFileContent_shouldMergeLineRanges() {
        target.loadFileContent(repository, COMMIT_ID, FILE_PATH, Arrays.asList(new LineRange(1, 1), new LineRange(3, 5),
                new LineRange(7, 5)));

        verify(contentService).streamFile(eq(repository), eq(COMMIT_ID), eq(FILE_PATH), pageRequestCaptor.capture(),
                eq(false), any(FileContentCallback.class));
        PageRequest request = pageRequestCaptor.getValue();
        assertThat(request.getStart(), equalTo(0));
        assertThat(request.getLimit(), equalTo(6));
    }
}
