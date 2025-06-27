package com.atlassian.jira.plugins.slack.servicedesk;

import com.atlassian.annotations.VisibleForTesting;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.clause.Property;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.clause.TerminalClauseImpl;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.atlassian.servicedesk.api.ServiceDesk;
import com.atlassian.servicedesk.api.ServiceDeskManager;
import com.atlassian.servicedesk.api.ServiceDeskServiceException;
import com.atlassian.servicedesk.api.portal.Portal;
import com.atlassian.servicedesk.api.portal.PortalManager;
import com.atlassian.servicedesk.api.requesttype.RequestType;
import com.atlassian.servicedesk.api.requesttype.RequestTypeQuery;
import com.atlassian.servicedesk.api.requesttype.RequestTypeService;
import com.atlassian.servicedesk.api.util.paging.PagedResponse;
import io.atlassian.fugue.Option;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class DefaultServiceDeskCompatibilityHelper extends ServiceDeskCompatibilityHelper {
    private static final DefaultServiceDeskCompatibilityHelper INSTANCE = new DefaultServiceDeskCompatibilityHelper();

    public static DefaultServiceDeskCompatibilityHelper getInstance() {
        return INSTANCE;
    }

    @VisibleForTesting
    DefaultServiceDeskCompatibilityHelper() {
    }

    @Override
    protected Optional<ServiceDesk> getServiceDeskForProject(final Project project) {
        ServiceDeskManager serviceDeskManager = ComponentAccessor.getOSGiComponentInstanceOfType(ServiceDeskManager.class);
        ServiceDesk serviceDesk = null;
        try {
            serviceDesk = serviceDeskManager.getServiceDeskForProject(project);
        } catch (ServiceDeskServiceException e) {
            log.error("Unable to load service desk", e);
        }

        return Optional.ofNullable(serviceDesk);
    }

    @Override
    protected Optional<Portal> getPortalForProject(final Project project) {
        PortalManager portalManager = ComponentAccessor.getOSGiComponentInstanceOfType(PortalManager.class);
        Portal portal = null;
        try {
            portal = portalManager.getPortalForProject(project);
        } catch (ServiceDeskServiceException e) {
            log.error("Unable to load portal", e);
        }

        return Optional.ofNullable(portal);
    }

    @Override
    protected Optional<PagedResponse<RequestType>> getRequestTypes(final Optional<RequestTypeService> requestTypeService,
                                                                   final ApplicationUser user,
                                                                   final RequestTypeQuery query) {
        Optional<PagedResponse<RequestType>> requestTypes = Optional.empty();
        try {
            requestTypes = requestTypeService.map(service -> service.getRequestTypes(user, query));
        } catch (ServiceDeskServiceException e) {
            log.error("Unable to load portal", e);
        }
        return requestTypes;
    }

    @Override
    public Object getTerminalClauseProperty(final TerminalClause clause) {
        return clause.getProperty();
    }

    @Override
    public TerminalClause createTerminalClause(final String clauseName,
                                               final Operator operator,
                                               final Operand operand,
                                               final Object property) {
        return new TerminalClauseImpl(clauseName, operator, operand, (Option<Property>) property);
    }
}
