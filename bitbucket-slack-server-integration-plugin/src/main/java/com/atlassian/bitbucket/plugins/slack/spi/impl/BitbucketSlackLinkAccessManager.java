package com.atlassian.bitbucket.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.spi.impl.AbstractSlackLinkAccessManager;
import com.atlassian.sal.api.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class BitbucketSlackLinkAccessManager extends AbstractSlackLinkAccessManager {
    @Autowired
    public BitbucketSlackLinkAccessManager(@Qualifier("salUserManager") final UserManager userManager) {
        super(userManager);
    }
}
