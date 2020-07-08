package it.com.atlassian.confluence.plugins.slack.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class ConnectionStatus extends ConfluenceAbstractPageComponent {
    private final PageElement container;

    public ConnectionStatus(PageElement container) {
        this.container = container;
    }

    public boolean isConnected() {
        return "connected".equalsIgnoreCase(trimToEmpty(getStatus().getText()));
    }

    public boolean isConnectedLimitedly() {
        String status = trimToEmpty(getStatus().getText());
        return "limited".equalsIgnoreCase(status);
    }

    private PageElement getStatus() {
        return container.find(By.className("aui-lozenge"));
    }
}
