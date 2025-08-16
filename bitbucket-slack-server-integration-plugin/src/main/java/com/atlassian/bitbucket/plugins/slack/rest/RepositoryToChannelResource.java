package com.atlassian.bitbucket.plugins.slack.rest;

import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.plugins.slack.model.RepositoryDto;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositorySearchRequest;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Path("/repositories")
public class RepositoryToChannelResource {
    static final int MAX_RESULTS_SIZE = 75;
    private static final int MINIMUM_QUERY_LENGTH = 1;

    private final RepositoryService repositoryService;

    @Inject
    public RepositoryToChannelResource(final RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRepositories(@QueryParam("name") final String name) {
        if (StringUtils.isBlank(name) || name.length() < MINIMUM_QUERY_LENGTH) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parameter 'name' must have at least 2 characters")
                    .build();
        }

        RepositorySearchRequest searchRequest = new RepositorySearchRequest.Builder()
                .name(name)
                .permission(Permission.REPO_ADMIN)
                .build();
        Page<Repository> resultPage = repositoryService.search(searchRequest, PageUtils.newRequest(0, MAX_RESULTS_SIZE));
        Iterable<Repository> repositories = resultPage.getValues();
        List<RepositoryDto> dtos = StreamSupport.stream(repositories.spliterator(), false)
                .map(repo -> new RepositoryDto(repo.getId(), repo.getSlug(), repo.getProject().getKey(), repo.getName(), repo.getProject().getName()))
                .collect(Collectors.toList());

        return Response.ok(dtos).build();
    }
}
