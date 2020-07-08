package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.AuthorisationException;
import com.atlassian.bitbucket.comment.Comment;
import com.atlassian.bitbucket.comment.CommentService;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.plugins.slack.model.FileDto;
import com.atlassian.bitbucket.plugins.slack.model.LineRange;
import com.atlassian.bitbucket.plugins.slack.model.Unfurl;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackUnfurlRenderer;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Branch;
import com.atlassian.bitbucket.repository.RefService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.repository.ResolveRefRequest;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.repository.Tag;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.EscalatedSecurityContext;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.github.seratch.jslack.api.model.Attachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SuppressWarnings("RedundantThrows")
@ExtendWith(MockitoExtension.class)
public class UnfurlLinkExtractorTest {
    private static final String URL = "https://url.com";
    private static List<String> links = Arrays.asList(
            URL + "/projects/PRJ/repos/repo-1",
            URL + "/projects/PRJ/repos/repo-1/browse",
            URL + "/projects/PRJ/repos/repo-1/pull-requests/123455/diff",
            URL + "/projects/PRJ/repos/repo-1/pull-requests/123455/overview?commentId=1",
            URL + "/projects/PRJ/repos/repo-1/pull-requests/123455/commits/aabbcc123",
            URL + "/projects/PRJ/repos/repo-1/commits/aabbcc123",
            URL + "/projects/PRJ/repos/repo-1/commits/aabbcc123?commentId=1",
            URL + "/projects/PRJ/repos/repo-1/browse?at=refs%2Fheads%2Fbranch",
            URL + "/projects/PRJ/repos/repo-1/browse?at=tag",
            URL + "/projects/PRJ/repos/repo-1/commits/unknownCommit",
            URL + "/projects/PRJ/repos/repo-1/browse/some-directory/nested-file.txt",
            URL + "/projects/PRJ/repos/repo-1/browse/some-directory/nested-file.txt?at=refs%2Fheads%2Fbranch",
            URL + "/projects/PRJ/repos/repo-1/browse/some-directory/nested-file.txt?at=refs%2Ftags%2Ftag",
            URL + "/projects/PRJ/repos/repo-1/browse/some-directory/nested-file.txt?at=refs%2Ftags%2Ftag#1-3,5",
            URL + "/projects/PRJ/repos/repo-1/commits/aabbcc123#some-directory/nested-file.txt",
            URL + "/projects/PRJ/repos/repo-1/browse/some-directory/nested-file.txt?at=aabbcc123",
            URL + "/projects/PRJ/repos/repo-1/pull-requests/123455/diff#some-directory/nested-file.txt",
            URL + "/projects/PRJ/repos/repo-1/pull-requests/123455/commits/aabbcc123#some-directory/nested-file.txt"
    );

    @Mock
    private SlackUnfurlRenderer slackUnfurlRenderer;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private CommentService commentService;
    @Mock
    private CommitService commitService;
    @Mock
    private PullRequestService pullRequestService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private SecurityService securityService;
    @Mock
    private RefService refService;

    @Mock
    private AuthorisationException exception;
    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private PullRequest pullRequest;
    @Mock
    private Comment comment;
    @Mock
    private Commit commit;
    @Mock
    private Branch branch;
    @Mock
    private Tag tag;
    @Mock
    private PullRequestRef pullRequestRef;
    @Mock
    private Repository repository;
    @Mock
    private Attachment repositoryAttachment;
    @Mock
    private Attachment prAttachment;
    @Mock
    private Attachment prCommentAttachment;
    @Mock
    private Attachment prCommitAttachment;
    @Mock
    private Attachment commitAttachment;
    @Mock
    private Attachment commitCommentAttachment;
    @Mock
    private Attachment branchAttachment;
    @Mock
    private Attachment tagAttachment;
    @Mock
    private Attachment fileAttachment;
    @Mock
    private EscalatedSecurityContext escalatedSecurityContext;
    @Captor
    private ArgumentCaptor<FileDto> fileDtoCaptor;

    @InjectMocks
    private UnfurlLinkExtractor target;

    @Test
    void findLinksToUnfurl_shouldFindLinksToUnfurl() throws Throwable {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(URL);

        when(repositoryService.getBySlug("PRJ", "repo-1")).thenReturn(repository);
        when(repository.getId()).thenReturn(123);
        when(pullRequestService.getById(123, 123455L)).thenReturn(pullRequest);
        when(commentService.getComment(1L)).thenReturn(Optional.of(comment));
        when(commitService.getCommit(argThat(new CommitRequestMatcher(repository, "aabbcc123")))).thenReturn(commit);
        lenient().when(refService.resolveRef(argThat(new ResolveRefRequestMatcher(repository, "refs/heads/branch"))))
                .thenReturn(branch);
        when(refService.resolveRef(argThat(new OrMatcher<>(new ResolveRefRequestMatcher(repository, "tag"),
                new ResolveRefRequestMatcher(repository, "refs/tags/tag"))))).thenReturn(tag);
        when(refService.getDefaultBranch(repository)).thenReturn(branch);

        //ids for deduplication
        when(commit.getId()).thenReturn("cmt");
        when(pullRequest.getId()).thenReturn(12345L);
        when(pullRequest.getFromRef()).thenReturn(pullRequestRef);
        when(pullRequestRef.getLatestCommit()).thenReturn("aabbcc123");
        when(comment.getId()).thenReturn(1L);
        when(branch.getId()).thenReturn("branch");
        when(branch.getType()).thenReturn(StandardRefType.BRANCH);
        when(branch.getLatestCommit()).thenReturn("aabbcc123");
        when(tag.getId()).thenReturn("tag");
        when(tag.getType()).thenReturn(StandardRefType.TAG);
        when(tag.getLatestCommit()).thenReturn("aabbcc123");

        when(securityService.impersonating(same(applicationUser), anyString())).thenReturn(escalatedSecurityContext);
        when(escalatedSecurityContext.call(any())).thenAnswer(args -> ((Operation) args.getArgument(0)).perform());

        when(slackUnfurlRenderer.repository(repository)).thenReturn(repositoryAttachment);
        lenient().when(slackUnfurlRenderer.pullRequest(pullRequest, null, null)).thenReturn(prAttachment);
        lenient().when(slackUnfurlRenderer.pullRequest(pullRequest, comment, null)).thenReturn(prCommentAttachment);
        when(slackUnfurlRenderer.pullRequest(pullRequest, null, commit)).thenReturn(prCommitAttachment);
        lenient().when(slackUnfurlRenderer.commit(commit, null)).thenReturn(commitAttachment);
        when(slackUnfurlRenderer.commit(commit, comment)).thenReturn(commitCommentAttachment);
        lenient().when(slackUnfurlRenderer.branchOrTag(branch, repository)).thenReturn(branchAttachment);
        when(slackUnfurlRenderer.branchOrTag(tag, repository)).thenReturn(tagAttachment);
        when(slackUnfurlRenderer.file(fileDtoCaptor.capture(), anyString())).thenReturn(fileAttachment);

        final List<Unfurl> result = target.findLinksToUnfurl(links, applicationUser);

        assertThat(result, containsInAnyOrder(
                new Unfurl(URL + "/projects/PRJ/repos/repo-1", repositoryAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/pull-requests/123455/diff", prAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/pull-requests/123455/overview?commentId=1", prCommentAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/pull-requests/123455/commits/aabbcc123", prCommitAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/commits/aabbcc123", commitAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/commits/aabbcc123?commentId=1", commitCommentAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/browse?at=refs%2Fheads%2Fbranch", branchAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/browse?at=tag", tagAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/browse/some-directory/nested-file.txt", fileAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/browse/some-directory/nested-file.txt?at=refs%2Ftags%2Ftag", fileAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/browse/some-directory/nested-file.txt?at=refs%2Ftags%2Ftag#1-3,5", fileAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/commits/aabbcc123#some-directory/nested-file.txt", fileAttachment),
                new Unfurl(URL + "/projects/PRJ/repos/repo-1/pull-requests/123455/diff#some-directory/nested-file.txt", fileAttachment)
        ));

        List<FileDto> parsedFiles = fileDtoCaptor.getAllValues();
        assertThat(parsedFiles.get(0), is(new FileDto(repository, Optional.of(branch), Optional.empty(), commit,
                "some-directory/nested-file.txt", emptyList())));
        assertThat(parsedFiles.get(1), is(new FileDto(repository, Optional.of(tag), Optional.empty(), commit,
                "some-directory/nested-file.txt", emptyList())));
        assertThat(parsedFiles.get(2), is(new FileDto(repository, Optional.of(tag), Optional.empty(), commit,
                "some-directory/nested-file.txt", Arrays.asList(new LineRange(1, 3), new LineRange(5, 5)))));
        assertThat(parsedFiles.get(3), is(new FileDto(repository, Optional.empty(), Optional.empty(), commit,
                "some-directory/nested-file.txt", emptyList())));
        assertThat(parsedFiles.get(4), is(new FileDto(repository, Optional.empty(), Optional.of(pullRequest), commit,
                "some-directory/nested-file.txt", emptyList())));
    }

    @Test
    void findLinksToUnfurl_shouldNotFindRepositoryToUnfurl() throws Throwable {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(URL);
        when(repositoryService.getBySlug("PRJ", "repo-1")).thenReturn(null);

        when(securityService.impersonating(same(applicationUser), anyString())).thenReturn(escalatedSecurityContext);
        when(escalatedSecurityContext.call(any())).thenAnswer(args -> ((Operation) args.getArgument(0)).perform());

        final List<Unfurl> result = target.findLinksToUnfurl(links, applicationUser);

        assertThat(result, empty());
    }

    @Test
    void findLinksToUnfurl_shouldLackPermissionsToRepoUnfurl() throws Throwable {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(URL);
        when(repositoryService.getBySlug("PRJ", "repo-1")).thenThrow(exception);

        when(securityService.impersonating(same(applicationUser), anyString())).thenReturn(escalatedSecurityContext);
        when(escalatedSecurityContext.call(any())).thenAnswer(args -> ((Operation) args.getArgument(0)).perform());

        final List<Unfurl> result = target.findLinksToUnfurl(links, applicationUser);

        assertThat(result, empty());
    }

    @Test
    void findLinksToUnfurl_shouldFallbackToRepoUnfurlWhenUserCannotAccessOtherItems() throws Throwable {
        when(pullRequestService.getById(anyInt(), anyLong())).thenThrow(exception);
        when(commentService.getComment(anyLong())).thenThrow(exception);
        when(commitService.getCommit(any())).thenThrow(exception);
        when(refService.resolveRef(any())).thenThrow(exception);

        testInvalidUrlFallbackToRepoUnfurl(links, URL + "/projects/PRJ/repos/repo-1");
    }

    @Test
    void findLinksToUnfurl_shouldFallbackToRepoUnfurlWhenOtherItemsAreNotFound() throws Throwable {
        when(pullRequestService.getById(anyInt(), anyLong())).thenReturn(null);
        when(commentService.getComment(anyLong())).thenReturn(Optional.empty());
        when(commitService.getCommit(any())).thenReturn(null);
        when(refService.resolveRef(any())).thenReturn(null);

        testInvalidUrlFallbackToRepoUnfurl(links, URL + "/projects/PRJ/repos/repo-1");
    }

    @Test
    void findLinksToUnfurl_shouldFallbackToRepoUnfurlWithDifferentPath() throws Throwable {
        testInvalidUrlFallbackToRepoUnfurl(
                Collections.singletonList(URL + "/projects/PRJ/repos/repo-1/browse"),
                URL + "/projects/PRJ/repos/repo-1/browse");
    }

    @Test
    void findLinksToUnfurl_shouldFallbackToRepoUnfurlWithInvalidCommit() throws Throwable {
        when(commitService.getCommit(any())).thenReturn(null);

        testInvalidUrlFallbackToRepoUnfurl(
                Collections.singletonList(URL + "/projects/PRJ/repos/repo-1/commits/aabbcc123unknown"),
                URL + "/projects/PRJ/repos/repo-1/commits/aabbcc123unknown");
    }

    private void testInvalidUrlFallbackToRepoUnfurl(final List<String> invalidUrls, final String repoUrl) throws Throwable {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(URL);

        when(repositoryService.getBySlug("PRJ", "repo-1")).thenReturn(repository);
        when(repository.getId()).thenReturn(123);

        when(securityService.impersonating(same(applicationUser), anyString())).thenReturn(escalatedSecurityContext);
        when(escalatedSecurityContext.call(any())).thenAnswer(args -> ((Operation) args.getArgument(0)).perform());

        when(slackUnfurlRenderer.repository(repository)).thenReturn(repositoryAttachment);

        final List<Unfurl> result = target.findLinksToUnfurl(invalidUrls, applicationUser);

        assertThat(result.size(), is(1));
        assertThat(result, hasItems(new Unfurl(repoUrl, repositoryAttachment)));
    }

    static class CommitRequestMatcher implements ArgumentMatcher<CommitRequest> {
        private final Repository repository;
        private final String hash;

        CommitRequestMatcher(final Repository repository, final String hash) {
            this.repository = repository;
            this.hash = hash;
        }

        @Override
        public boolean matches(final CommitRequest argument) {
            return argument != null && repository.equals(argument.getRepository())
                    && hash.equals(argument.getCommitId());
        }
    }

    static class ResolveRefRequestMatcher implements ArgumentMatcher<ResolveRefRequest> {
        private final Repository repository;
        private final String refId;

        ResolveRefRequestMatcher(final Repository repository, final String refId) {
            this.repository = repository;
            this.refId = refId;
        }

        @Override
        public boolean matches(final ResolveRefRequest argument) {
            return argument != null && repository.equals(argument.getRepository())
                    && Optional.of(refId).equals(argument.getRefId());
        }
    }

    static class OrMatcher<T, M extends ArgumentMatcher<T>> implements ArgumentMatcher<T> {
        private final M[] matchers;

        public OrMatcher(final M... matchers) {
            this.matchers = matchers;
        }

        @Override
        public boolean matches(T argument) {
            return Arrays.stream(matchers)
                    .anyMatch(matcher -> matcher.matches(argument));
        }
    }

}
