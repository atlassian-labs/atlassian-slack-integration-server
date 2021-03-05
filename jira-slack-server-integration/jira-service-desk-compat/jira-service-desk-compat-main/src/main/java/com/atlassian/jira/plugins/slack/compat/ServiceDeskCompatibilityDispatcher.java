package com.atlassian.jira.plugins.slack.compat;

import com.atlassian.annotations.VisibleForTesting;
import com.atlassian.jira.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.util.Optional;

@Slf4j
public class ServiceDeskCompatibilityDispatcher {
    private static final String SERVICE_DESK_MANAGER_CLASS_NAME = "com.atlassian.servicedesk.api.ServiceDeskManager";

    private static final ServiceDeskCompatibilityDispatcher INSTANCE = new ServiceDeskCompatibilityDispatcher();

    public static ServiceDeskCompatibilityDispatcher getInstance() {
        return INSTANCE;
    }

    @VisibleForTesting
    ServiceDeskCompatibilityDispatcher() {
    }

    public boolean isServiceDeskInstalled() {
        boolean isServiceDeskInstalled;
        try {
            Class.forName(SERVICE_DESK_MANAGER_CLASS_NAME);
            Class.forName("com.atlassian.servicedesk.api.portal.PortalManager");
            Class.forName("com.atlassian.servicedesk.api.requesttype.RequestTypeService");

            isServiceDeskInstalled = true;
        } catch (ClassNotFoundException e) {
            // no need to print a long stacktrace here; absence of Service Desk on Jira instance isn't an error
            log.debug("Service Desk isn't detected. Cause: {}", e.getMessage());
            isServiceDeskInstalled = false;
        }

        return isServiceDeskInstalled;
    }

    public Optional<ServiceDeskCompatibilityHelper> getHelper() {
        ServiceDeskCompatibilityHelper helper = null;
        try {
            Class<?> sdClass = Class.forName(SERVICE_DESK_MANAGER_CLASS_NAME);
            String methodName = "getServiceDeskForProject";
            Method getServiceDeskForProjectMethod = MethodUtils.getAccessibleMethod(sdClass, methodName, Project.class);
            String returnedClassName = getServiceDeskForProjectMethod.getReturnType().getName();

            if ("com.atlassian.servicedesk.api.ServiceDesk".equals(returnedClassName)) {
                helper = ServiceDesk4CompatibilityHelper.getInstance();
            } else {
                log.error("Failed to create a ServiceDeskCompatibilityHelper because of unexpected method signature [{}]",
                        getServiceDeskForProjectMethod);
            }
        } catch (ClassNotFoundException e) {
            log.error("Failed to load ServiceDeskManager", e);
        }

        return Optional.ofNullable(helper);
    }
}
