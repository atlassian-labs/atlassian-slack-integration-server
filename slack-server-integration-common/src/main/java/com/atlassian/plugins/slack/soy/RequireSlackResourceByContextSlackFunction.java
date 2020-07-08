package com.atlassian.plugins.slack.soy;

import com.atlassian.soy.renderer.SoyServerFunction;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * A generic Soy function providing this functionality could not be found. This function
 * has 'Slack' in it to make it obvious to any potential clients that it is bundled in
 * the Slack integration plugin.
 */
public class RequireSlackResourceByContextSlackFunction implements SoyServerFunction<String> {
    private final PageBuilderService pageBuilderService;

    private static final Set<Integer> VALID_ARG_SIZES = ImmutableSet.of(1);

    public RequireSlackResourceByContextSlackFunction(final PageBuilderService pageBuilderService) {
        this.pageBuilderService = pageBuilderService;
    }


    @Override
    public String apply(final Object... args) {
        String context = (String) args[0];
        pageBuilderService.assembler().resources().requireContext(context);
        return "";
    }

    @Override
    public String getName() {
        return "requireSlackResourcesByContext";
    }

    @Override
    public Set<Integer> validArgSizes() {
        return VALID_ARG_SIZES;
    }
}
