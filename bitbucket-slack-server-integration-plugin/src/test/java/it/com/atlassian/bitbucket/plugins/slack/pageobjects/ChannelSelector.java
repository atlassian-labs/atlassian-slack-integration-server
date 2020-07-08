package it.com.atlassian.bitbucket.plugins.slack.pageobjects;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.bitbucket.element.IdSelect2;

import javax.annotation.Nonnull;

public class ChannelSelector extends IdSelect2<ChannelSelector, ChannelOption> {
    /**
     * @param container element that encapsulates the Select2 input and current selections. Does not contain the
     *                  dropdown list
     */
    public ChannelSelector(@Nonnull PageElement container) {
        super(container, ChannelOption.class);
    }

    @Nonnull
    @Override
    protected ChannelSelector self() {
        return this;
    }
}
