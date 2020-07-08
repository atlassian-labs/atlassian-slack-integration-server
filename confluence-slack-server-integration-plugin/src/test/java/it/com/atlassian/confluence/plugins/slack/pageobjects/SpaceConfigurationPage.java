package it.com.atlassian.confluence.plugins.slack.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.page.ConfluenceAbstractPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

public class SpaceConfigurationPage extends ConfluenceAbstractPage {
    private final String spaceKey;
    private final String teamId;

    @ElementBy(className = "slack-configuration-page")
    private PageElement configurationSection;

    public SpaceConfigurationPage(String spaceKey, String teamId) {
        this.spaceKey = spaceKey;
        this.teamId = teamId;
    }

    public ConfigurationSection geConfigurationSection() {
        return pageBinder.bind(ConfigurationSection.class, configurationSection);
    }

    public WorkspaceSelector workspaceSelector() {
        return pageBinder.bind(WorkspaceSelector.class, configurationSection.find(By.className("slack-team-selector-box")));
    }

    public ConnectionStatus workspaceConnection() {
        return pageBinder.bind(ConnectionStatus.class, configurationSection.find(By.className("slack-connection-status-container")));
    }

    public ConnectionStatus userConnection() {
        return pageBinder.bind(ConnectionStatus.class, configurationSection.find(By.className("slack-user-connection-status-container")));
    }

    public boolean isLinked() {
        try {
            driver.findElement(By.className("slack-not-linked"));
            return false;
        } catch (NoSuchElementException e) {
            return true;
        }
    }

    @Override
    public String getUrl() {
        return "/spaces/slack2.action?key=" + spaceKey + "&teamId=" + teamId;
    }
}
