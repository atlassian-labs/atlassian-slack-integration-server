package com.atlassian.jira.plugins.slack.compat;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.atlassian.servicedesk.api.ServiceDesk;
import com.atlassian.servicedesk.api.portal.Portal;
import com.atlassian.servicedesk.api.requesttype.RequestType;
import com.atlassian.servicedesk.api.requesttype.RequestTypeQuery;
import com.atlassian.servicedesk.api.requesttype.RequestTypeService;
import com.atlassian.servicedesk.api.util.paging.PagedResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public abstract class ServiceDeskCompatibilityHelper {
    public Map<String, String> buildRequestTypeNameToKeyMap(final Project project) {
        Optional<RequestTypeService> requestTypeService = Optional.ofNullable(
                ComponentAccessor.getOSGiComponentInstanceOfType(RequestTypeService.class));

        Optional<PagedResponse<RequestType>> requestTypesPage = getServiceDeskForProject(project)
                .map(ServiceDesk::getId)
                .flatMap(sdId -> requestTypeService.map(reqService -> reqService.newQueryBuilder()
                        .serviceDesk(sdId)
                        .requestOverrideSecurity(true)
                        .build()))
                .flatMap(reqTypeQuery -> getRequestTypes(requestTypeService, null, reqTypeQuery));
        Optional<List<RequestType>> requestTypes = requestTypesPage.map(PagedResponse::getResults);
        Optional<Portal> portal = getPortalForProject(project);
        Optional<String> portalName = portal.map(Portal::getName);
        Optional<String> portalKey = portal.map(port -> invokeMethodNoException(port, "getKey"));

        Map<String, String> requestTypeNameToKey = requestTypes.map(reqTypes -> reqTypes.stream()
                .collect(Collectors.toMap(
                        type -> String.format("%s (%s)", type.getName(), portalName.orElse("")),
                        type -> String.format("%s/%s", portalKey.orElse(""), invokeMethodNoException(type, "getKey")),
                        (key1, key2) -> key1)
                )).orElse(Collections.emptyMap());
        return requestTypeNameToKey;
    }

    private <T> T invokeMethodNoException(final Object target, final String methodName, final Object... args) {
        try {
            return (T) MethodUtils.invokeMethod(target, methodName, args);
        } catch (Exception e) {
            log.error("An error occurred while calling method [{}] on object [{}]", methodName, target, e);
            return null;
        }
    }

    protected abstract Optional<ServiceDesk> getServiceDeskForProject(Project project);

    protected abstract Optional<Portal> getPortalForProject(Project project);

    protected abstract Optional<PagedResponse<RequestType>> getRequestTypes(Optional<RequestTypeService> requestTypeService,
                                                                            ApplicationUser user,
                                                                            RequestTypeQuery query);

    public abstract Object getTerminalClauseProperty(TerminalClause clause);

    public abstract TerminalClause createTerminalClause(String clauseName,
                                                        Operator operator,
                                                        Operand operand,
                                                        Object property);
}
