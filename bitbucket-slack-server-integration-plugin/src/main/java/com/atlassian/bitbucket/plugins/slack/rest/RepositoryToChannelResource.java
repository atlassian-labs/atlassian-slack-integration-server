package com.atlassian.bitbucket.plugins.slack.rest;

import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.plugins.slack.model.RepositoryDto;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositorySearchRequest;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Path("/repositories")
public class RepositoryToChannelResource {
    static final int MAX_RESULTS_SIZE = 75;
    private static final int MINIMUM_QUERY_LENGTH = 1;

    private final RepositoryService repositoryService;

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
