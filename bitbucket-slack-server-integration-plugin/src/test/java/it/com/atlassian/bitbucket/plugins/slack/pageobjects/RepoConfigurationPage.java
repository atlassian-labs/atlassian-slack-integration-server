package it.com.atlassian.bitbucket.plugins.slack.pageobjects;

import com.atlassian.bitbucket.test.DefaultFuncTestData;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.bitbucket.page.BaseRepositoryPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

public class RepoConfigurationPage extends BaseRepositoryPage {
    private final String teamId;

    @ElementBy(className = "slack-configuration-page")
    private PageElement configurationSection;

    public RepoConfigurationPage(String projectKey, String slug, String teamId) {
        super(projectKey, slug);
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
        return "/plugins/servlet/repo-slack-settings"
                + DefaultFuncTestData.getRepositoryPath(projectKey, slug)
                + "?teamId=" + teamId;
    }
}
