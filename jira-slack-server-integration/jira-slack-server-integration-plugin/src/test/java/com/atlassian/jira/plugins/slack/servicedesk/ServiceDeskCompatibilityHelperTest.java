package com.atlassian.jira.plugins.slack.servicedesk;

import com.atlassian.jira.junit.rules.AvailableInContainer;
import com.atlassian.jira.junit.rules.MockitoContainer;
import com.atlassian.jira.project.Project;
import com.atlassian.servicedesk.api.ServiceDesk;
import com.atlassian.servicedesk.api.portal.Portal;
import com.atlassian.servicedesk.api.requesttype.RequestType;
import com.atlassian.servicedesk.api.requesttype.RequestTypeGroup;
import com.atlassian.servicedesk.api.requesttype.RequestTypeQuery;
import com.atlassian.servicedesk.api.requesttype.RequestTypeService;
import com.atlassian.servicedesk.api.util.paging.PagedResponse;
import com.atlassian.servicedesk.api.util.paging.PagedResponseImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class ServiceDeskCompatibilityHelperTest {
    @Mock
    Project project;
    @Mock
    ServiceDesk serviceDesk;
    @Mock
    ServiceDeskCompatibilityHelper target;
    @Mock
    RequestTypeQuery.Builder queryBuilder;
    @Mock
    RequestTypeQuery requestTypeQuery;
    @Mock
    @AvailableInContainer
    RequestTypeService requestTypeService;
    @Rule
    public MockitoContainer mockitoContainer = new MockitoContainer(this);

    @Test
    public void buildRequestTypeNameToKeyMapReturnsExpectedMap() {
        int sdId = 4;
        String portalName = "somePortalName";
        String portalKey = "somePortalKey";
        String reqTypeName = "someReqTypeName";
        String reqTypeKey = "someReqTypeKey";

        when(target.buildRequestTypeNameToKeyMap(project)).thenCallRealMethod();
        when(target.getServiceDeskForProject(project)).thenReturn(Optional.of(serviceDesk));
        when(serviceDesk.getId()).thenReturn(sdId);
        when(requestTypeService.newQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.serviceDesk(sdId)).thenReturn(queryBuilder);
        when(queryBuilder.requestOverrideSecurity(anyBoolean())).thenReturn(queryBuilder);
        when(queryBuilder.build()).thenReturn(requestTypeQuery);
        PagedResponse<RequestType> pagedResponse = PagedResponseImpl.fromSingle(false,
                (RequestType) new RequestTypeMock(34, 7, 3L, 0L, reqTypeKey, reqTypeName, "", "", null)).build();
        when(target.getRequestTypes(any(Optional.class), isNull(), eq(requestTypeQuery)))
                .thenReturn(Optional.of(pagedResponse));
        when(target.getPortalForProject(project))
                .thenReturn(Optional.of(new PortalMock(7, portalKey, portalName, "someDesk")));

        Map<String, String> map = target.buildRequestTypeNameToKeyMap(project);

        assertThat(map, notNullValue());
        assertThat(map.get(String.format("%s (%s)", reqTypeName, portalName)), equalTo(String.format("%s/%s", portalKey, reqTypeKey)));
    }

    @Data
    @AllArgsConstructor
    public static class PortalMock implements Portal {
        int id;
        String key;
        String name;
        String description;
    }

    @Data
    @AllArgsConstructor
    public static class RequestTypeMock implements RequestType {
        int id;
        int portalId;
        long issueTypeId;
        long iconId;
        String key;
        String name;
        String description;
        String helpText;
        List<RequestTypeGroup> groups;
    }
}
