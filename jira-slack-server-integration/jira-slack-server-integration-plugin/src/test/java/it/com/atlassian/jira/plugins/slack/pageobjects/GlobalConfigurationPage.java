package it.com.atlassian.jira.plugins.slack.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

public class GlobalConfigurationPage extends AbstractJiraPage {

    @ElementBy(id = "slack-configuration-panel")
    private PageElement slackConfigurationPanel;

    @Override
    public String getUrl() {
        return "/plugins/servlet/slack/configure";
    }

    @Override
    public TimedCondition isAt() {
        return this.slackConfigurationPanel.timed().isVisible();
    }
}
