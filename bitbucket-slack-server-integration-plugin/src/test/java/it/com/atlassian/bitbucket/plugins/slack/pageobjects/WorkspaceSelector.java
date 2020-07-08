package it.com.atlassian.bitbucket.plugins.slack.pageobjects;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.bitbucket.element.AbstractElementPageObject;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class WorkspaceSelector extends AbstractElementPageObject {
    @ElementBy(cssSelector = "button.aui-dropdown2-trigger")
    private PageElement dropdownButton;

    public WorkspaceSelector(PageElement container) {
        super(container);
    }

    public String selectedTeamName() {
        return trimToEmpty(dropdownButton.getText());
    }
}
