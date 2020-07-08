package it.com.atlassian.jira.plugins.slack.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

public class ProjectConfigurationPage extends AbstractJiraPage {
    private final String projectKey;
    private final String teamId;

    @ElementBy(className = "slack-channel-mappings")
    private PageElement configurationSection;

    public ProjectConfigurationPage(String projectKey, String teamId) {
        this.projectKey = projectKey;
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
        return "/secure/ConfigureSlack.jspa?projectKey=" + projectKey + "&teamId=" + teamId;
    }

    @Override
    public TimedCondition isAt() {
        return this.configurationSection.timed().isVisible();
    }
}
