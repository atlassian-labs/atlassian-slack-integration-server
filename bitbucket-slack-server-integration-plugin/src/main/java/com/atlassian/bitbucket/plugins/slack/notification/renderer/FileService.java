package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.content.ContentTreeNode;
import com.atlassian.bitbucket.plugins.slack.model.LineRange;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageRequestImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {
    private static final int DEFAULT_MAX_LINES_TO_LOAD = 5;

    private final ContentService contentService;

    private final int maxLinesToLoad;

    @Autowired
    public FileService(final ContentService contentService) {
        this.contentService = contentService;
        this.maxLinesToLoad = Integer.getInteger("slack.addon.unfurl.file.max.lines", DEFAULT_MAX_LINES_TO_LOAD);
    }

    public FileContentHolder loadFileContent(final Repository repository, final String commitId, final String filePath,
                                             final List<LineRange> ranges) {
        LineRange unitedRange = getUnitedRange(ranges);
        int linesToLoad;
        if (unitedRange.isSingleLine()) {
            linesToLoad = 1;
        } else {
            // +1 to amount of lines to load because range borders are inclusive
            linesToLoad = Math.min(maxLinesToLoad, unitedRange.getToLine() - unitedRange.getFromLine() + 1);
        }

        int fromLineInternal = clientLineNumberToInternalLineIndex(unitedRange.getFromLine());
        PageRequest pageRequest = new PageRequestImpl(fromLineInternal, linesToLoad);
        FileContentHolder contentHolder = new FileContentHolder(pageRequest);

        // load one more line to see if range covers file by its end
        // it is used later to put ellipsis to the end of the code fragment
        PageRequestImpl extendedPageRequest = new PageRequestImpl(pageRequest.getStart(), pageRequest.getLimit() + 1);
        contentService.streamFile(repository, commitId, filePath, extendedPageRequest, false, contentHolder);

        return contentHolder;
    }

    private LineRange getUnitedRange(final List<LineRange> ranges) {
        int startLine = 0;
        int endLine = startLine;
        for (LineRange range : ranges) {
            int fromLine = range.getFromLine();
            int toLine = range.getToLine();

            if (startLine == 0) {
                startLine = Math.min(fromLine, toLine);
            } else {
                startLine = Math.min(startLine, Math.min(fromLine, toLine));
            }

            endLine = Math.max(endLine, Math.max(fromLine, toLine));
        }

        return new LineRange(startLine, endLine);
    }

    private int clientLineNumberToInternalLineIndex(final int clientLineNumber) {
        // internally lines are counted starting from 0, while user sees them starting from 1
        return clientLineNumber - 1;
    }

    public ContentTreeNode.Type getType(final Repository repository, final String commitId, final String path) {
        return contentService.getType(repository, commitId, path);
    }
}
