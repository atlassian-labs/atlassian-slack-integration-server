package com.atlassian.jira.plugins.slack.compat;

import com.atlassian.fugue.Either;
import com.atlassian.fugue.Option;
import com.atlassian.jira.junit.rules.AvailableInContainer;
import com.atlassian.jira.junit.rules.MockitoContainer;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.clause.Property;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.clause.TerminalClauseImpl;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.atlassian.servicedesk.api.ServiceDesk;
import com.atlassian.servicedesk.api.ServiceDeskManager;
import com.atlassian.servicedesk.api.portal.Portal;
import com.atlassian.servicedesk.api.portal.PortalManager;
import com.atlassian.servicedesk.api.requesttype.RequestType;
import com.atlassian.servicedesk.api.requesttype.RequestTypeQuery;
import com.atlassian.servicedesk.api.requesttype.RequestTypeService;
import com.atlassian.servicedesk.api.util.paging.PagedResponse;
import com.atlassian.servicedesk.api.util.paging.PagedResponseImpl;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class ServiceDesk3CompatibilityHelperTest {
    @Mock
    @AvailableInContainer
    ServiceDeskManager serviceDeskManager;
    @Mock
    @AvailableInContainer
    PortalManager portalManager;
    @Mock
    Project project;
    @Mock
    ServiceDesk serviceDesk;
    @Mock
    Portal portal;
    @Mock
    RequestTypeService requestTypeService;
    @Mock
    ApplicationUser user;
    @Mock
    RequestTypeQuery query;
    @Mock
    RequestType requestType;
    @Mock
    Property property;
    @Mock
    Operator operator;
    @Mock
    Operand operand;

    @Rule
    public MockitoContainer mockitoContainer = new MockitoContainer(this);

    @InjectMocks
    ServiceDesk3CompatibilityHelper target;

    @Test
    public void getServiceDeskForProjectReturnsServiceDesk() {
        when(serviceDeskManager.getServiceDeskForProject(project)).thenReturn(Either.right(serviceDesk));

        Optional<ServiceDesk> actualServiceDesk = target.getServiceDeskForProject(project);

        assertThat(actualServiceDesk.get(), sameInstance(serviceDesk));
    }

    @Test
    public void getPortalForProjectReturnsPortal() {
        when(portalManager.getPortalForProject(project)).thenReturn(Either.right(portal));

        Optional<Portal> actualPortal = target.getPortalForProject(project);

        assertThat(actualPortal.get(), sameInstance(portal));
    }

    @Test
    public void getRequestTypesReturnRequestTypes() {
        when(requestTypeService.getRequestTypes(user, query)).thenReturn(Either.right(PagedResponseImpl.fromSingle(false, requestType).build()));

        Optional<PagedResponse<RequestType>> requestTypes = target.getRequestTypes(Optional.of(requestTypeService), user, query);

        List<RequestType> results = requestTypes.get().getResults();
        assertThat(results.size(), equalTo(1));
        assertThat(results.get(0), sameInstance(requestType));
    }

    @Test
    public void getTerminalClausePropertyReturnsProperty() {
        TerminalClauseImpl clause = new TerminalClauseImpl("", operator, operand, Option.option(property));

        Option<Property> actualProperty = target.getTerminalClauseProperty(clause);

        assertThat(actualProperty.get(), sameInstance(property));
    }

    @Test
    public void createTerminalClauseReturnsClause() {
        String clauseName = "someClauseName";
        TerminalClause clause = target.createTerminalClause(clauseName, operator, operand, Option.option(property));

        assertThat(clause.getName(), equalTo(clauseName));
        assertThat(clause.getOperator(), sameInstance(operator));
        assertThat(clause.getOperand(), sameInstance(operand));
        assertThat(clause.getProperty().get(), sameInstance(property));
    }
}
