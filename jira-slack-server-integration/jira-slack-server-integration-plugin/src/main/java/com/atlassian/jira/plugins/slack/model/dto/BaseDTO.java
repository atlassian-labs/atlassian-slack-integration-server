package com.atlassian.jira.plugins.slack.model.dto;

import java.io.Serializable;

/**
 * This object will define our DTO classes and provide one piece of functionality that is provided by the persistence
 * layer, which should technically be put in {@link com.atlassian.jira.plugins.slack.model.BaseObject}.
 */
public abstract class BaseDTO implements Serializable {
    static final int NON_EXISTENT_ID = -1;

    private int id;

    public BaseDTO() {
        this(NON_EXISTENT_ID);
    }

    public BaseDTO(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
