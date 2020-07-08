package com.atlassian.jira.plugins.slack.web.panels;

import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.webresource.api.assembler.PageBuilderService;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

public class IssueWebPanel implements WebPanel {

    private final PageBuilderService pageBuilderService;

    public IssueWebPanel(PageBuilderService pageBuilderService) {
        this.pageBuilderService = pageBuilderService;
    }

    @Override
    public String getHtml(Map<String, Object> map) {
        StringWriter sw = new StringWriter();
        try {
            writeHtml(sw, map);
            return sw.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error getting HTML for IssueWebPanel", e);
        }
    }

    @Override
    public void writeHtml(Writer writer, Map<String, Object> map) throws IOException {
        pageBuilderService.assembler().resources().requireContext("slack-issue-panel");
        writer.append("<div id=\"slack-issue-panel-spinner\"></div><div id=\"slack-issue-panel\" class=\"slack-issue-panel\"><div id=\"slack-issue-panel-errors\" class=\"errors\"></div></div>");
    }
}
