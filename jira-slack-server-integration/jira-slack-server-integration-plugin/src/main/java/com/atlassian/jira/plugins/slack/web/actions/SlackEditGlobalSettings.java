package com.atlassian.jira.plugins.slack.web.actions;

import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.web.action.ActionViewDataMappings;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Page of advanced settings for global stuff
 */
@SupportedMethods(RequestMethod.GET)
@RequiredArgsConstructor
public class SlackEditGlobalSettings extends JiraWebActionSupport {
    public static final String ALLOW_GLOBAL_AUTOCONVERT = "allowGlobalAutoConvert";
    public static final String ALLOW_AUTOCONVERT_FOR_GUEST_CHANNELS = "allowAutoConvertInGuestChannels";
    public static final String HIDE_ISSUE_PANEL = "hideIssuePanel";

    protected final PluginConfigurationManager pluginConfigurationManager;
    private final PageBuilderService pageBuilderService;

    @ActionViewDataMappings({SUCCESS})
    public Map<String, Object> getDataMap() {
        return ImmutableMap.<String, Object>builder()
                .put(ALLOW_GLOBAL_AUTOCONVERT, pluginConfigurationManager.isGlobalAutoConvertEnabled())
                .put(ALLOW_AUTOCONVERT_FOR_GUEST_CHANNELS, pluginConfigurationManager.isIssuePreviewForGuestChannelsEnabled())
                .put(HIDE_ISSUE_PANEL, pluginConfigurationManager.isIssuePanelHidden())
                .build();
    }

    @Override
    public String execute() throws Exception {
        // Not present in older versions of Jira, so we require it here to avoid errors in soy templates, see https://jira.atlassian.com/browse/HC-12820
        pageBuilderService.assembler().resources().requireContext("com.atlassian.auiplugin:aui-flag");

        return super.execute();
    }
}
