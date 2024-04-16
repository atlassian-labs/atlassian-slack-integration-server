package com.atlassian.bitbucket.plugins.slack.listener;

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
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Ref;
import com.atlassian.bitbucket.repository.RefService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.repository.ResolveRefRequest;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.google.common.primitives.Ints;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.uri.UriComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MultivaluedMap;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UnfurlLinkExtractor {
    private final static Logger logger = LoggerFactory.getLogger(UnfurlLinkExtractor.class);

    private final SlackUnfurlRenderer slackUnfurlRenderer;
    private final ApplicationProperties applicationProperties;
    private final CommentService commentService;
    private final CommitService commitService;
    private final PullRequestService pullRequestService;
    private final RepositoryService repositoryService;
    private final SecurityService securityService;
    private final RefService refService;

    @Autowired
    public UnfurlLinkExtractor(
            final SlackUnfurlRenderer slackUnfurlRenderer,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
            final CommentService commentService,
            final CommitService commitService,
            final PullRequestService pullRequestService,
            final RepositoryService repositoryService,
            final SecurityService securityService,
            final RefService refService) {
        this.slackUnfurlRenderer = slackUnfurlRenderer;
        this.applicationProperties = applicationProperties;
        this.commentService = commentService;
        this.commitService = commitService;
        this.pullRequestService = pullRequestService;
        this.repositoryService = repositoryService;
        this.securityService = securityService;
        this.refService = refService;
    }

    /**
     * This method parses Bitbucket Server links and find their respective content. It impersonates the given user
     * for permission verification purposes.
     *
     * @param links Links to be parsed
     * @param user  User to be impersonated and who has pasted the link in Slack.
     */
    public List<Unfurl> findLinksToUnfurl(final List<String> links, final ApplicationUser user) {
        final Set<String> dedup = new HashSet<>();
        return links.stream()
                .distinct()
                .map(url -> securityService
                        .impersonating(user, "Unfurl user objects in Slack")
                        .call(() -> this.findContentFrom(url, dedup)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<Unfurl> findContentFrom(final String url, final Set<String> dedup) {
        final URI link = URI.create(url);
        // it should contain only the path after the context path
        final URI relativeLink = getContentPath(link);
        final MultivaluedMap<String, String> queryParams = UriComponent.decodeQuery(link, true);
        try {
            final Comment comment = tryComment(queryParams).orElse(null);
            final String commentKey = comment != null ? String.valueOf(comment.getId()) : "";

            final List<String> segments = UriComponent
                    .decodePath(relativeLink, false)
                    .stream()
                    .map(path -> {
                        try {
                            return URLDecoder.decode(path.getPath(), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            return path.getPath();
                        }
                    })
                    .collect(Collectors.toList());
            final String fragment = StringUtils.defaultString(relativeLink.getFragment());

            final Optional<Repository> repositoryOptional = tryRepository(segments);
            if (!repositoryOptional.isPresent()) {
                return Optional.empty();
            }

            final Repository repository = repositoryOptional.get();
            final Optional<Commit> commit = tryCommit(repository, segments);
            final String commitKey = commit.map(Commit::getId).orElse("");
            final Optional<PullRequest> pullRequest = tryPullRequest(repository, segments);

            // file unfurl
            final Optional<FileDto> fileOptional = tryFile(repository, commit, pullRequest, segments, queryParams, fragment);
            if (fileOptional.isPresent()) {
                FileDto file = fileOptional.get();
                String dedupUniqueKey = file.getPath()
                        + file.getRef().map(Ref::getId).orElse(null)
                        + file.getPullRequest().map(PullRequest::getId).orElse(null)
                        + file.getLineRanges().size();
                if (!dedup.add(dedupUniqueKey)) {
                    return Optional.empty();
                }
                return Optional.of(new Unfurl(url, slackUnfurlRenderer.file(file, url)));
            }

            // PR unfurl, with optional comment or commit
            if (pullRequest.isPresent()) {
                final String dedupUniqueKey = pullRequest.get().getId() + commitKey + commentKey;
                if (!dedup.add(dedupUniqueKey)) {
                    return Optional.empty();
                }
                return Optional.of(new Unfurl(url, slackUnfurlRenderer.pullRequest(
                        pullRequest.get(),
                        comment,
                        commit.orElse(null))));
            }

            // commit unfurl, with optional comment
            if (commit.isPresent()) {
                final String dedupUniqueKey = commitKey + commentKey;
                if (!dedup.add(dedupUniqueKey)) {
                    return Optional.empty();
                }
                return Optional.of(new Unfurl(url, slackUnfurlRenderer.commit(commit.get(), comment)));
            }

            // tag or branch unfurl
            final Optional<Ref> ref = tryBranchOrTag(repository, queryParams, segments);
            if (ref.isPresent()) {
                final String dedupUniqueKey = repository.getId() + ref.get().getId();
                if (!dedup.add(dedupUniqueKey)) {
                    return Optional.empty();
                }
                return Optional.of(new Unfurl(url, slackUnfurlRenderer.branchOrTag(ref.get(), repository)));
            }

            // repository unfurl
            final String dedupUniqueKey = String.valueOf(repository.getId());
            if (!dedup.add(dedupUniqueKey)) {
                return Optional.empty();
            }
            return Optional.of(new Unfurl(url, slackUnfurlRenderer.repository(repository)));
        } catch (Exception e) {
            logger.debug("Could not parse URI {}", relativeLink, e);
        }
        return Optional.empty();
    }

    /**
     * /projects/HC/repos/webcore-next/pull-requests/3586/overview?commentId=1223781
     * /projects/HC/repos/webcore-next/commits/428cf995f290c9601df043dbb6748bb641bb6b10?commentId=1582891
     */
    private Optional<Comment> tryComment(final MultivaluedMap<String, String> queryParams) {
        return queryParams.entrySet().stream()
                .filter(param -> "commentId".equals(param.getKey()))
                .map(param -> getCommentById(param.getValue().get(0)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny();
    }

    private Optional<Comment> getCommentById(final String idStr) {
        try {
            return commentService.getComment(Long.parseLong(idStr));
        } catch (Exception e) {
            logger.debug("Could not find comment for {}", idStr, e);
            return Optional.empty();
        }
    }

    /**
     * /projects/PROJECT_1/repos/rep_1/browse/basic_branching/file.txt?at=refs%2Ftags%2Ftag1.0#1
     * /projects/PROJECT_1/repos/rep_1/browse?at=refs%2Fheads%2Fbasic_branching
     * /projects/PROJECT_1/repos/rep_1/browse/add_file/add_file.txt?at=refs%2Fheads%2Fbasic_branching
     * /projects/PROJECT_1/repos/rep_1/browse?at=tag1.0
     * /projects/PROJECT_1/repos/rep_1/browse/add_file/add_file.txt?at=basic_branching
     */
    private Optional<Ref> tryBranchOrTag(final Repository repository,
                                         final MultivaluedMap<String, String> queryParams,
                                         final List<String> segments) {
        if (segments.size() == 5 && "browse".equals(segments.get(4))) {
            return getBranchOrTag(repository, queryParams);
        }
        return Optional.empty();
    }

    private Optional<Ref> getBranchOrTag(final Repository repository, final MultivaluedMap<String, String> queryParams) {
        return queryParams.entrySet().stream()
                .filter(param -> "at".equals(param.getKey()))
                .map(param -> getBranchOrTag(repository, param.getValue().get(0)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny();
    }

    private Optional<Ref> getBranchOrTag(final Repository repository, final String path) {
        try {
            final Ref ref = refService.resolveRef(new ResolveRefRequest.Builder(repository)
                    .refId(path)
                    .build());
            if (ref != null && (ref.getType() == StandardRefType.BRANCH || ref.getType() == StandardRefType.TAG)) {
                return Optional.of(ref);
            }
        } catch (Exception e) {
            logger.debug("Could not find branch or tag with ref id={}", path, e);
        }
        return Optional.empty();
    }

    /**
     * /projects/HC/repos/webcore-next/commits/23fe834d7110020f39de45f1ff5ee67f38af4ac1
     * /projects/HC/repos/webcore-next/commits/0a943a29376f2336b78312d99e65da17048951db?commentId=22
     * /projects/HC/repos/webcore-next/pull-requests/3586/commits/23fe834d7110020f39de45f1ff5ee67f38af4ac1
     */
    private Optional<Commit> tryCommit(final Repository repository, final List<String> segments) {
        try {
            if (segments.size() >= 6 && "commits".equals(segments.get(4))) {
                final String hash = segments.get(5);
                return findCommit(repository, hash);
            }
            if (segments.size() >= 8 && "commits".equals(segments.get(6))) {
                final String hash = segments.get(7);
                return findCommit(repository, hash);
            }
        } catch (Exception e) {
            logger.debug("Could not parse commit info from segments {}", segments, e);
        }
        return Optional.empty();
    }

    private Optional<Commit> findCommit(final Repository repository, final MultivaluedMap<String, String> queryParams) {
        return queryParams.entrySet().stream()
                .filter(param -> "at".equals(param.getKey()))
                .map(param -> findCommit(repository, param.getValue().get(0)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny();
    }

    private Optional<Commit> findCommit(final Repository repository, final String hash) {
        try {
            Commit commit = commitService.getCommit(new CommitRequest.Builder(repository, hash).build());
            return Optional.ofNullable(commit);
        } catch (Exception e) {
            logger.debug("Could not find commit for {}", hash, e);
            return Optional.empty();
        }
    }

    /**
     * /projects/HC/repos/webcore-next/pull-requests/3586/diff
     * /projects/HC/repos/webcore-next/pull-requests/3586/overview
     * <p>
     * /projects/HC/repos/webcore-next/pull-requests/3586/diff#basic_branching/file.txt?F1
     * /projects/HC/repos/webcore-next/pull-requests/3586/diff#webcore/src/js/app/components/navigation/settings/ConversationNotificationSettingsTable.js
     */
    private Optional<PullRequest> tryPullRequest(final Repository repository, final List<String> segments) {
        try {
            if (segments.size() >= 6 && "pull-requests".equals(segments.get(4))) {
                final long pullRequestId = Long.parseLong(segments.get(5));
                return Optional.ofNullable(pullRequestService.getById(repository.getId(), pullRequestId));
            }
        } catch (Exception e) {
            logger.debug("Could not parse pull request info from segments {}", segments, e);
        }
        return Optional.empty();
    }

    /**
     * file default:        /projects/PROJECT_1/repos/rep_2/browse/some-directory/nested-file.txt
     * file branch:         /projects/PROJECT_1/repos/rep_2/browse/some-directory/nested-file.txt?at=refs%2Fheads%2Fbr1
     * file tag:            /projects/PROJECT_1/repos/rep_2/browse/some-directory/nested-file.txt?at=refs%2Ftags%2Ftag-1.0
     * file tag&line:       /projects/PROJECT_1/repos/rep_2/browse/some-directory/nested-file.txt?at=refs%2Ftags%2Ftag-1.0#1-3,5
     * file commit:         /projects/PROJECT_1/repos/rep_2/commits/970a4096948fe617c5f9b1bcd9729b30f033b5fb#some-directory/nested-file.txt
     * file commit from pr: /projects/PROJECT_1/repos/rep_2/browse/some-directory/nested-file.txt?at=b5c9c88718acfa1f828db05c3462fa06e96c83c1
     * file pr:             /projects/PROJECT_1/repos/rep_2/pull-requests/15/diff#some-directory/nested-file.txt
     * file pr&commit:      /projects/PROJECT_1/repos/rep_2/pull-requests/19/commits/b5c9c88718acfa1f828db05c3462fa06e96c83c1#some-directory/nested-file.txt
     */
    private Optional<FileDto> tryFile(final Repository repository,
                                      final Optional<Commit> commitFromPath,
                                      final Optional<PullRequest> pullRequest,
                                      final List<String> segments,
                                      final MultivaluedMap<String, String> queryParams,
                                      final String fragment) {
        // file in default or custom branch/tag, or in "commit from pr" view
        if (segments.size() >= 6 && "browse".equals(segments.get(4))) {
            String filePath = segments.subList(5, segments.size()).stream()
                    .collect(Collectors.joining("/"));

            return parseFileForKnownRef(repository, queryParams, fragment, filePath);
        }

        // file in regular commit view
        if (segments.size() == 6 && "commits".equals(segments.get(4)) && commitFromPath.isPresent()
                && StringUtils.isNoneBlank(fragment)) {
            return parseFileForRegularCommitView(repository, commitFromPath, fragment);
        }

        // file in pull-request (optionally with commit)
        if (segments.size() >= 7 && "pull-requests".equals(segments.get(4)) && pullRequest.isPresent()
                && StringUtils.isNoneBlank(fragment)) {
            return parseFileInPullRequest(repository, pullRequest, segments, fragment);
        }

        return Optional.empty();
    }

    private Optional<FileDto> parseFileForKnownRef(final Repository repository,
                                                   final MultivaluedMap<String, String> queryParams,
                                                   final String fragment,
                                                   final String filePath) {
        Commit commit;
        Optional<Ref> refOptional;
        Optional<Ref> branchOrTagFromQueryParam = getBranchOrTag(repository, queryParams);
        List<LineRange> lineRanges = fragmentToLineRanges(fragment);
        // file in custom branch/tag and "commit from pr"
        if (branchOrTagFromQueryParam.isPresent()) {
            refOptional = branchOrTagFromQueryParam;
            String latestCommitId = refOptional.get().getLatestCommit();
            commit = commitService.getCommit(new CommitRequest.Builder(repository, latestCommitId).build());
        } else {
            Optional<Commit> commitFromQueryParam = findCommit(repository, queryParams);
            // file in "commit from PR" view
            if (commitFromQueryParam.isPresent()) {
                commit = commitFromQueryParam.get();
                refOptional = Optional.empty();
            } else {
                // file in default branch
                Ref ref = branchOrTagFromQueryParam.orElseGet(() -> refService.getDefaultBranch(repository));
                refOptional = Optional.of(ref);
                String latestCommitId = ref.getLatestCommit();
                commit = commitService.getCommit(new CommitRequest.Builder(repository, latestCommitId).build());
            }
        }

        return Optional.of(new FileDto(repository, refOptional, Optional.empty(), commit, filePath, lineRanges));
    }

    private Optional<FileDto> parseFileForRegularCommitView(final Repository repository,
                                                            final Optional<Commit> commitFromPath,
                                                            final String fragment) {
        String filePath = fragment;
        // current UI does not allow to select line ranges in commit view
        List<LineRange> lineRanges = Collections.emptyList();

        return Optional.of(new FileDto(repository, Optional.empty(), Optional.empty(), commitFromPath.get(),
                filePath, lineRanges));
    }

    private Optional<FileDto> parseFileInPullRequest(final Repository repository,
                                                     final Optional<PullRequest> pullRequest,
                                                     final List<String> segments,
                                                     final String fragment) {
        String filePath = fragment;
        // current UI does not allow to select line ranges in PR view
        List<LineRange> lineRanges = Collections.emptyList();
        String commitId;
        if (segments.size() >= 8 && "commits".equals(segments.get(6))) {
            commitId = segments.get(7);
        } else {
            commitId = pullRequest.get().getFromRef().getLatestCommit();
        }
        Optional<Commit> commit = findCommit(repository, commitId);

        return Optional.of(new FileDto(repository, Optional.empty(), pullRequest, commit.get(), filePath, lineRanges));
    }

    private List<LineRange> fragmentToLineRanges(final String fragment) {
        List<LineRange> ranges = new ArrayList<>();
        String[] rawRanges = fragment.split(",");
        for (String range : rawRanges) {
            String[] borders = range.split("-");
            if (borders.length == 2) {
                Integer startBorder = Ints.tryParse(borders[0]);
                Integer endBorder = Ints.tryParse(borders[1]);
                if (startBorder != null && endBorder != null) {
                    ranges.add(new LineRange(startBorder, endBorder));
                }
            } else if (borders.length == 1) {
                Integer lineNumber = Ints.tryParse(borders[0]);
                if (lineNumber != null) {
                    ranges.add(new LineRange(lineNumber, lineNumber));
                }
            }
        }
        return ranges;
    }

    /**
     * /projects/HC/repos/webcore-next/
     */
    private Optional<Repository> tryRepository(final List<String> segments) {
        try {
            if (segments.size() >= 4 && "projects".equals(segments.get(0)) && "repos".equals(segments.get(2))) {
                final String projectKey = segments.get(1);
                final String repositorySlug = segments.get(3);
                return Optional.ofNullable(repositoryService.getBySlug(projectKey, repositorySlug));
            }
        } catch (Exception e) {
            logger.debug("Could not parse blog info from segments {}", segments, e);
        }
        return Optional.empty();
    }

    /**
     * e.g.: http://localhost:7990/bitbucket/projects/HC/repos/webcore -> /projects/HC/repos/webcore
     */
    private URI getContentPath(final URI link) {
        return URI.create(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).relativize(link);
    }
}
