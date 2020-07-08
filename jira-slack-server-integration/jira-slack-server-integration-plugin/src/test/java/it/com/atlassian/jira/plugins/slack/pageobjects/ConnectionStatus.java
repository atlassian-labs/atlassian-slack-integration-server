package it.com.atlassian.jira.plugins.slack.pageobjects;

import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class ConnectionStatus {
    private final PageElement container;

    public ConnectionStatus(PageElement container) {
        this.container = container;
    }

    public boolean isConnectedLimitedly() {
        String status = trimToEmpty(getStatus().getText());
        return "limited".equalsIgnoreCase(status);
    }

    public boolean isConnected() {
        String status = trimToEmpty(getStatus().getText());
        return "connected".equalsIgnoreCase(status);
    }

    private PageElement getStatus() {
        return container.find(By.className("aui-lozenge"));
    }
}
