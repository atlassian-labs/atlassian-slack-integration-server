package it.com.atlassian.jira.plugins.slack.pageobjects;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class WorkspaceSelector {
    @ElementBy(cssSelector = "button.aui-dropdown2-trigger")
    private PageElement dropdownButton;
    private final PageElement container;

    public WorkspaceSelector(PageElement container) {
        this.container = container;
    }

    public String selectedTeamName() {
        // For some reason the dropdownButton component returns an empty text. This works better for Jira.
        return trimToEmpty(container.getText());
    }
}
