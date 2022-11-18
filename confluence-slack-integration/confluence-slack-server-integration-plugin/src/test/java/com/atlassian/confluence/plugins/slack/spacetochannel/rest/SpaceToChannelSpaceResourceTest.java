package com.atlassian.confluence.plugins.slack.spacetochannel.rest;

import com.atlassian.confluence.plugins.slack.spacetochannel.model.SpaceResult;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SpacesWithAdminPermissionProvider;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import io.atlassian.fugue.Either;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SpaceToChannelSpaceResourceTest {
    @Mock
    private SpacesWithAdminPermissionProvider defaultSpacesWithAdminPermissionProvider;
    @Mock
    private ConfluenceUser confluenceUser;

    @InjectMocks
    private SpaceToChannelSpaceResource target;

    @BeforeEach
    public void setUp() {
        AuthenticatedUserThreadLocal.set(confluenceUser);
    }

    @AfterEach
    public void tearDown() {
        AuthenticatedUserThreadLocal.reset();
    }

    @Test
    public void getSpaces_shouldReturnBadRequestIfQueryIsEmpty() {
        Response result = target.getSpaces("");

        assertThat(result.getStatus(), is(400));
        verify(defaultSpacesWithAdminPermissionProvider, never()).findSpacesMatchingName(any(), any(), anyInt());
    }

    @Test
    public void getSpaces_shouldReturnExpectedValue() {
        List<SpaceResult> queryResult = Collections.emptyList();
        when(defaultSpacesWithAdminPermissionProvider.findSpacesMatchingName(
                "n", confluenceUser, SpaceToChannelSpaceResource.MAX_RESULTS_SIZE)
        ).thenReturn(Either.right(queryResult));

        Response result = target.getSpaces("n");

        assertThat(result.getStatus(), is(200));
        assertThat(result.getEntity(), sameInstance(queryResult));
    }

    @Test
    public void getSpaces_shouldReturnExpectedValueWhenSpacesAreNotFound() {
        when(defaultSpacesWithAdminPermissionProvider.findSpacesMatchingName(
                "n", confluenceUser, SpaceToChannelSpaceResource.MAX_RESULTS_SIZE)
        ).thenReturn(Either.left(new Exception("msg")));

        Response result = target.getSpaces("n");

        assertThat(result.getStatus(), is(500));
        assertThat(result.getEntity(), is("msg"));
    }
}
