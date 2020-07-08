package com.atlassian.plugins.slack.rest.model;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class OauthRequestData {
    private final String redirect;
    private final String redirectQuery;
    private final String redirectFragment;

    @JsonCreator
    public OauthRequestData(@JsonProperty("redirect") final String redirect,
                            @JsonProperty("redirectQuery") final String redirectQuery,
                            @JsonProperty("redirectFragment") final String redirectFragment) {
        this.redirect = redirect;
        this.redirectQuery = redirectQuery;
        this.redirectFragment = redirectFragment;
    }

    public String getRedirect() {
        return redirect;
    }

    public String getRedirectQuery() {
        return redirectQuery;
    }

    public String getRedirectFragment() {
        return redirectFragment;
    }
}
