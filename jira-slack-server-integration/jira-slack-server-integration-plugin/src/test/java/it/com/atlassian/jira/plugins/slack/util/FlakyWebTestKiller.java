package it.com.atlassian.jira.plugins.slack.util;

import com.atlassian.jira.functest.framework.backdoor.Backdoor;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.webtest.webdriver.util.AUIBlanket;
import com.atlassian.jira.webtest.webdriver.util.AUIFlags;
import com.atlassian.jira.webtest.webdriver.util.AUIHelpTip;
import com.atlassian.pageobjects.PageBinder;
import jakarta.inject.Inject;

/**
 * Flaky tests are those which unexpectedly fail sometimes. In general, it seems random but there's also a root cause.
 * For WebDriver tests in Jira, a very known source of flakiness are flags that other things that covers components
 * you're trying to click or interact with.
 * This class gathers some techniques to prevent common flakiness.
 */
public class FlakyWebTestKiller {
    @Inject
    private PageBinder pageBinder;
    @Inject
    private Backdoor backdoor;
    @Inject
    private JiraTestedProduct jira;

    /**
     * Disable flags that may cover our components in the UI.
     * It changes some Jira configurations and must be called BEFORE navigating to a page.
     */
    public void disableJiraFlags() {
        backdoor.flags().clearFlags();
    }

    /**
     * Even if flags and tips are disabled, sometimes plugins may display additional ones, like the healthcheck plugin,
     * that still interfere with our tests.
     * There fore, AFTER THE PAGE is loaded, is a good practice to close all flags before starting interacting
     * with the page.
     */
    public void closeAllOpenedFlagsAndTips() {
        pageBinder.bind(AUIFlags.class).closeAllFlags();
        new AUIHelpTip(jira).closeAll();
    }

    /**
     * Modal has a blanket that covers the entire page. When it's closing, it takes some time to complete the animation
     * and WebDriver by default does not wait for CSS transitions to complete.
     * This method mus be called whenever a modal dialog is closed before any further action on the page.
     */
    public void waitModalDisappear() {
        pageBinder.bind(AUIBlanket.class).waitUntilClosed();
    }
}
