package com.atlassian.plugins.slack.soy;

import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.descriptors.WebPanelModuleDescriptor;
import com.atlassian.soy.renderer.JsExpression;
import com.atlassian.soy.renderer.SoyClientFunction;
import com.atlassian.soy.renderer.SoyServerFunction;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WebPanelsSlackFunction implements SoyServerFunction<List<String>>, SoyClientFunction {
    private static final Logger LOG = LoggerFactory.getLogger(WebPanelsSlackFunction.class);

    private static final Set<Integer> VALID_ARG_SIZES = ImmutableSet.of(1, 2);

    private final WebInterfaceManager webInterfaceManager;

    public WebPanelsSlackFunction(final WebInterfaceManager webInterfaceManager) {
        this.webInterfaceManager = webInterfaceManager;
    }

    @Override
    public String getName() {
        return "getWebPanelsSlack";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> apply(final Object... args) {
        final String location = (String) args[0];
        final ImmutableMap.Builder<String, Object> contextBuilder = ImmutableMap.builder();

        if (args.length == 2) {
            contextBuilder.putAll((Map<String, ?>) args[1]);
        }

        final Map<String, Object> context = contextBuilder.build();

        final List<String> webPanels = new ArrayList<>();
        for (WebPanelModuleDescriptor webPanelModuleDescriptor : webInterfaceManager.getDisplayableWebPanelDescriptors(location, context)) {
            try {
                webPanels.add(webPanelModuleDescriptor.getModule().getHtml(context));
            } catch (RuntimeException e) {
                LOG.warn(String.format("An error occurred rendering %s. Ignoring", webPanelModuleDescriptor.getCompleteKey()), e);
            }
        }

        return webPanels;
    }

    @Override
    public JsExpression generate(final JsExpression... jsExpressions) {
        return new JsExpression("'Client-side web panels are not plugged in yet'");
    }

    @Override
    public Set<Integer> validArgSizes() {
        return VALID_ARG_SIZES;
    }
}
