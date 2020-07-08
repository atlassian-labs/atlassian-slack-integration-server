package com.atlassian.plugins.slack.velocity;

import com.atlassian.templaterenderer.annotations.HtmlSafe;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.io.Writer;

import static com.atlassian.webresource.api.UrlMode.AUTO;

@Component
public class VelocityPageBuilderService {
    private final PageBuilderService pageBuilderService;

    @Autowired
    public VelocityPageBuilderService(PageBuilderService pageBuilderService) {
        this.pageBuilderService = pageBuilderService;
    }

    public void requireWebResource(String resourceKey) {
        pageBuilderService.assembler().resources().requireWebResource(resourceKey);
    }

    public void requireContext(String context) {
        pageBuilderService.assembler().resources().requireContext(context);
    }

    private void includeResources(Writer writer) {
        pageBuilderService.assembler().assembled()
                .drainIncludedResources()
                .writeHtmlTags(writer, AUTO);
    }

    @HtmlSafe
    public String getRequiredResources() {
        StringWriter stringWriter = new StringWriter();
        includeResources(stringWriter);
        return stringWriter.toString();
    }
}
