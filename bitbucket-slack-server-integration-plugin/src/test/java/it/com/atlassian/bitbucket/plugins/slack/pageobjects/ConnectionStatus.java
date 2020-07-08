package it.com.atlassian.bitbucket.plugins.slack.pageobjects;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.bitbucket.element.AbstractElementPageObject;
import org.openqa.selenium.By;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class ConnectionStatus extends AbstractElementPageObject {
    public ConnectionStatus(PageElement container) {
        super(container);
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
