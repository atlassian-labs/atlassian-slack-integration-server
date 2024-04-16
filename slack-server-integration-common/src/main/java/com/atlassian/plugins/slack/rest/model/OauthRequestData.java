package com.atlassian.plugins.slack.rest.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

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
