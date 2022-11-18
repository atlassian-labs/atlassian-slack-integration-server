package it.com.atlassian.confluence.plugins.slack.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class WorkspaceSelector extends ConfluenceAbstractPageComponent {
    private final PageElement container;

    public WorkspaceSelector(PageElement container) {
        this.container = container;
    }

    public String selectedTeamName() {
        return trimToEmpty(container.find(By.cssSelector("button.aui-dropdown2-trigger")).getText());
    }
}
