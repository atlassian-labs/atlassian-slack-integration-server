package com.atlassian.plugins.slack.soy;

import com.atlassian.soy.renderer.SoyServerFunction;
import com.atlassian.webresource.api.UrlMode;
import com.atlassian.webresource.api.WebResourceUrlProvider;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class PluginResourceUrlSlackFunction implements SoyServerFunction<String> {
    private final WebResourceUrlProvider webResourceUrlProvider;

    public PluginResourceUrlSlackFunction(final WebResourceUrlProvider webResourceUrlProvider) {
        this.webResourceUrlProvider = webResourceUrlProvider;
    }

    @Override
    public String getName() {
        return "pluginResourceUrl";
    }

    @Override
    public Set<Integer> validArgSizes() {
        return ImmutableSet.of(2, 3);
    }

    @Override
    public String apply(final Object... args) {
        final String plugin = String.valueOf(args[0]);
        final String resource = String.valueOf(args[1]);

        final UrlMode urlMode;
        if (args.length > 2) {
            urlMode = UrlMode.valueOf(String.valueOf(args[2]));
        } else {
            urlMode = UrlMode.RELATIVE;
        }

        return webResourceUrlProvider.getStaticPluginResourceUrl(plugin, resource, urlMode);
    }
}
