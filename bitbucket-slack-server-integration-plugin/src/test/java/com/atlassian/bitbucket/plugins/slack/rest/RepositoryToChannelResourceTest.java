package com.atlassian.bitbucket.plugins.slack.rest;

import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.plugins.slack.model.RepositoryDto;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositorySearchRequest;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.util.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RepositoryToChannelResourceTest {
    @Mock
    RepositoryService repositoryService;
    @Mock
    Page<Repository> repositoriesPage;
    @Mock
    Repository repo1;
    @Mock
    Repository repo2;
    @Mock
    Project project;
    @Captor
    ArgumentCaptor<RepositorySearchRequest> searchRequestCaptor;

    @InjectMocks
    RepositoryToChannelResource resource;

    @Test
    void getRepositories_shouldReturnFilteredRepositoriesList() {
        String filter = "filter";
        when(repositoryService.search(searchRequestCaptor.capture(), any())).thenReturn(repositoriesPage);
        when(repositoriesPage.getValues()).thenReturn(Arrays.asList(repo1, repo2));
        when(repo1.getId()).thenReturn(11);
        when(repo1.getSlug()).thenReturn("slug-1");
        when(repo1.getName()).thenReturn("repo-name-1");
        when(repo1.getProject()).thenReturn(project);

        when(repo2.getId()).thenReturn(22);
        when(repo2.getSlug()).thenReturn("slug-2");
        when(repo2.getName()).thenReturn("repo-name-2");
        when(repo2.getProject()).thenReturn(project);
        when(project.getKey()).thenReturn("some-project-key");
        when(project.getName()).thenReturn("some-project-name");

        Response response = resource.getRepositories(filter);

        List<RepositoryDto> foundRepositories = (List) response.getEntity();
        assertThat(foundRepositories.size(), equalTo(2));
        assertThat(foundRepositories.get(0), equalTo(new RepositoryDto(11, "slug-1", "some-project-key", "repo-name-1", "some-project-name")));
        assertThat(foundRepositories.get(1), equalTo(new RepositoryDto(22, "slug-2", "some-project-key", "repo-name-2", "some-project-name")));
        RepositorySearchRequest searchRequest = searchRequestCaptor.getValue();
        assertThat(searchRequest.getName(), equalTo("filter"));
        assertThat(searchRequest.getPermission(), equalTo(Permission.REPO_ADMIN));
    }
}
