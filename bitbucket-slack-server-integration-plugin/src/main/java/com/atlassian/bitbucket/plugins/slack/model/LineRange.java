package com.atlassian.bitbucket.plugins.slack.model;

import lombok.Value;

@Value
public class LineRange {
    int fromLine;
    int toLine;

    public boolean isSingleLine() {
        return fromLine == toLine;
    }
}
