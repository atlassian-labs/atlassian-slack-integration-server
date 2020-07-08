package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import com.atlassian.annotations.VisibleForTesting;
import com.atlassian.bitbucket.content.AbstractFileContentCallback;
import com.atlassian.bitbucket.util.PageRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileContentHolder extends AbstractFileContentCallback {
    private final PageRequest pageRequest;

    private boolean binary = false;
    private boolean fileEndHit = false;
    private final List<SimpleLine> lines = new ArrayList<>();

    public FileContentHolder(final PageRequest pageRequest) {
        this.pageRequest = pageRequest;
    }

    @VisibleForTesting
    PageRequest getPageRequest() {
        return pageRequest;
    }

    @Override
    public void onBinary() throws IOException {
        binary = true;
    }

    @Override
    public boolean onLine(int lineNumber, String line, boolean truncated) throws IOException {
        boolean loadMore = true;
        if (lineNumber <= getClientToLineInclusive()) {
            lines.add(new SimpleLine(lineNumber, line));
        } else {
            fileEndHit = true;
            loadMore = false;
        }

        return loadMore;
    }

    private int getClientToLineInclusive() {
        // internally lines are counted starting from 0, but ContentService.streamFile() returns them starting from 1
        // so I need to add 1 to get correct line number of the end of the range
        return pageRequest.getStart() + pageRequest.getLimit();
    }

    public boolean isBinary() {
        return binary;
    }

    public List<SimpleLine> getLines() {
        return lines;
    }

    public boolean isFileStartHit() {
        // internally lines are counted starting from 0
        return pageRequest.getStart() == 0;
    }

    public boolean isFileEndHit() {
        return fileEndHit || lines.size() < pageRequest.getLimit();
    }
}
