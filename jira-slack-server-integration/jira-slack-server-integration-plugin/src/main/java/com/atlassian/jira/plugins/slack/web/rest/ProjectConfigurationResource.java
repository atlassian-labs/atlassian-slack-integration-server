package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.dto.ConfigurationMigrationDTO;
import com.atlassian.jira.plugins.slack.model.dto.ProjectConfigurationDTO;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Strings;
import io.atlassian.fugue.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.function.Function;

@Path("/mapping/{projectKey}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class ProjectConfigurationResource {
    private static final Logger logger = LoggerFactory.getLogger(ProjectConfigurationResource.class);

    private final ProjectConfigurationManager projectConfigurationManager;
    private final ProjectManager projectManager;
    private final PermissionManager permissionManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final JiraAuthenticationContext authenticationContext;

    @Inject
    @Autowired
    public ProjectConfigurationResource(final ProjectConfigurationManager projectConfigurationManager,
                                        final ProjectManager projectManager,
                                        final PermissionManager permissionManager,
                                        final GlobalPermissionManager globalPermissionManager,
                                        final JiraAuthenticationContext authenticationContext) {
        this.projectConfigurationManager = projectConfigurationManager;
        this.projectManager = projectManager;
        this.permissionManager = permissionManager;
        this.globalPermissionManager = globalPermissionManager;
        this.authenticationContext = authenticationContext;
    }

    @POST
    @Path("migrate")
    public Response migrate(@PathParam("projectKey") final String projectKey,
                            final ConfigurationMigrationDTO migrationData) {
        final Either<Response, ProjectConfigurationDTO> responseProjectConfigurationDTOEither =
                checkPermissionsAndConfigToPersist(
                        projectKey,
                        ProjectConfigurationDTO.builder(migrationData).build(),
                        true);
        return responseProjectConfigurationDTOEither.fold(
                Function.identity(),
                dto -> {
                    final ProjectConfigurationDTO newConfig = projectConfigurationManager.insertProjectConfiguration(
                            dto, authenticationContext.getLoggedInUser());

                    migrationData.getValues().forEach((key, value) ->
                            checkPermissionsAndConfigToPersist(
                                    projectKey,
                                    ProjectConfigurationDTO.builder(newConfig).setName(key).setValue(value).build(),
                                    false
                            ).fold(
                                    error -> {
                                        logger.error("Could not create project configuration", error);
                                        return null;
                                    },
                                    fieldDto -> projectConfigurationManager.insertProjectConfiguration(
                                            fieldDto, authenticationContext.getLoggedInUser())

                            ));

                    return Response.ok(newConfig).build();
                }
        );
    }

    @POST
    public Response enableNotification(@PathParam("projectKey") final String projectKey,
                                       final ProjectConfigurationDTO input) {
        return checkPermissionsAndConfigToPersist(projectKey, input, true).fold(
                Function.identity(),
                dto -> {
                    final ProjectConfigurationDTO newConfig = projectConfigurationManager.insertProjectConfiguration(
                            dto, authenticationContext.getLoggedInUser());
                    return Response.ok(newConfig).build();
                }
        );
    }

    @PUT
    public Response updateNotification(@PathParam("projectKey") final String projectKey,
                                       final ProjectConfigurationDTO input) {
        return checkPermissionsAndConfigToPersist(projectKey, input, true).fold(
                Function.identity(),
                configuration -> {
                    projectConfigurationManager.updateProjectConfiguration(configuration);
                    return Response.ok(configuration).build();
                }
        );
    }

    @DELETE
    public Response disableNotification(@PathParam("projectKey") final String projectKey,
                                        final ProjectConfigurationDTO input) {
        final boolean isDeletingGroup = Strings.isNullOrEmpty(input.getName());
        return checkPermissionsAndConfigToPersist(projectKey, input, !isDeletingGroup).fold(
                Function.identity(),
                dto -> {
                    if (isDeletingGroup) {
                        projectConfigurationManager.deleteProjectConfigurationGroup(dto,
                                authenticationContext.getLoggedInUser());
                    } else {
                        projectConfigurationManager.deleteProjectConfiguration(dto,
                                authenticationContext.getLoggedInUser());
                    }
                    return Response.ok().build();
                }
        );
    }

    private Either<Response, ProjectConfigurationDTO> checkPermissionsAndConfigToPersist(
            final String projectKey,
            final ProjectConfigurationDTO projectConfigurationDTO,
            final boolean updateOwner) {
        final Project project = projectManager.getProjectByCurrentKey(projectKey);
        if (project == null) {
            return Either.left(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("A project with this key doesn't exist.")
                    .build());
        }

        final ApplicationUser user = authenticationContext.getLoggedInUser();
        if (!isProjectAdmin(user, project)) {
            return Either.left(Response
                    .status(Response.Status.FORBIDDEN)
                    .build());
        }

        final EventMatcherType eventMatcherType = EventMatcherType.fromName(projectConfigurationDTO.getName());
        final EventFilterType eventFilterType = EventFilterType.fromName(projectConfigurationDTO.getName());

        final ProjectConfigurationDTO newConfig = ProjectConfigurationDTO.builder(projectConfigurationDTO)
                .setProjectId(project.getId())
                .setProjectKey(project.getKey())
                .setProjectName(project.getName())
                .setMatcher(eventMatcherType, projectConfigurationDTO.getValue())
                .setFilter(eventFilterType, projectConfigurationDTO.getValue())
                .build();

        if (updateOwner) {
            projectConfigurationManager.updatedOwnerIfNeeded(newConfig, user.getKey());
        }

        return Either.right(newConfig);
    }

    private boolean isProjectAdmin(final ApplicationUser user, final Project project) {
        return permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, user) ||
                globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }
}
