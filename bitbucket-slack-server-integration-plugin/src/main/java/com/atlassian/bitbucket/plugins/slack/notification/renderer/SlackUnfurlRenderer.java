package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import com.atlassian.bitbucket.comment.Comment;
import com.atlassian.bitbucket.comment.CommentSearchRequest;
import com.atlassian.bitbucket.comment.CommentService;
import com.atlassian.bitbucket.comment.CommentSeverity;
import com.atlassian.bitbucket.comment.CommentState;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitListMergeFilter;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.CommitsRequest;
import com.atlassian.bitbucket.content.ContentTreeNode;
import com.atlassian.bitbucket.plugins.slack.model.FileDto;
import com.atlassian.bitbucket.plugins.slack.model.LineRange;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipantStatus;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.pull.PullRequestSearchRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.pull.PullRequestState;
import com.atlassian.bitbucket.repository.Branch;
import com.atlassian.bitbucket.repository.Ref;
import com.atlassian.bitbucket.repository.RefService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryBranchesRequest;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.scm.ScmService;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.Person;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageUtils;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Attachment.AttachmentBuilder;
import com.github.seratch.jslack.api.model.Field;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer.MAX_COMMITS_TO_SHOW;
import static com.atlassian.plugins.slack.util.SlackHelper.escapeSignsForSlackLink;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.leftPad;

/**
 * Generates notifications in Slack format.
 * <p>
 * Methods from this class will call services that may throw {@link @com.atlassian.bitbucket.AuthorisationException}
 */
@Component
public class SlackUnfurlRenderer {
    private final I18nResolver i18nResolver;
    private final CommentService commentService;
    private final PullRequestService pullRequestService;
    private final SlackLinkRenderer slackLinkRenderer;
    private final ScmService scmService;
    private final RepositoryService repositoryService;
    private final RefService refService;
    private final CommitService commitService;
    private final FileService fileService;

    @Autowired
    public SlackUnfurlRenderer(final I18nResolver i18nResolver,
                               final CommentService commentService,
                               final PullRequestService pullRequestService,
                               final SlackLinkRenderer slackLinkRenderer,
                               final ScmService scmService,
                               final RepositoryService repositoryService,
                               final RefService refService,
                               final CommitService commitService,
                               final FileService fileService) {
        this.i18nResolver = i18nResolver;
        this.commentService = commentService;
        this.pullRequestService = pullRequestService;
        this.slackLinkRenderer = slackLinkRenderer;
        this.scmService = scmService;
        this.repositoryService = repositoryService;
        this.refService = refService;
        this.commitService = commitService;
        this.fileService = fileService;
    }

    /**
     * Example from Cloud:
     * <pre>
     * {@code
     *  {
     *     "attachments": [
     *         {
     *             "actions": [
     *                 {
     *                     "type": "button",
     *                     "url": "https://bitbucket.org/atluiz/stride-api-java/pull-requests/4?utm_term=review-pull-request&utm_source=bb-slack&utm_medium=referral-external",
     *                     "text": "Review",
     *                     "style": ""
     *                 },
     *                 {
     *                     "name": "nudge",
     *                     "type": "button",
     *                     "text": "Nudge reviewers",
     *                     "style": "",
     *                     "value": "{\"bitbucketTeamId\":\"{39a95a9b-aa2f-4ff4-971f-f24ba0ce8516}\",\"repoId\":\"{20ee4d9e-34f0-4789-b949-053f632f8db3}\",\"pullRequestId\":\"4\"}"
     *                 },
     *                 {
     *                     "name": "createComment",
     *                     "type": "button",
     *                     "text": "Add comment",
     *                     "style": "",
     *                     "value": "{\"objectType\":\"pullRequest\",\"objectId\":\"4\",\"bitbucketTeamId\":\"{39a95a9b-aa2f-4ff4-971f-f24ba0ce8516}\",\"repoId\":\"{20ee4d9e-34f0-4789-b949-053f632f8db3}\"}"
     *                 }
     *             ],
     *             "fallback": "@Luiz Silva updated pull request #4",
     *             "pretext": "<https://pi-devs.slack.com/messages/@UC7LU9ZGS|@Luiz Silva> updated pull request <https://bitbucket.org/atluiz/stride-api-java/pull-requests/4?utm_term=pull-request&amp;utm_source=bb-slack&amp;utm_medium=referral-external|#4>",
     *             "author_name": "Luiz Ricardo",
     *             "author_link": "https://bitbucket.org/utluiz/?utm_term=account&utm_source=bb-slack&utm_medium=referral-external",
     *             "author_icon": "https://bitbucket.org/account/utluiz/avatar/",
     *             "text": "<https://bitbucket.org/atluiz/stride-api-java/pull-requests/4?utm_term=pull-request&amp;utm_source=bb-slack&amp;utm_medium=referral-external|*some-user-pr created online with Bitbucket*>\n`utluiz/someuserpr-created-online-with-bitbucket-1536766719877` → `master`",
     *             "footer": "<https://bitbucket.org/atluiz/stride-api-java?utm_term=repo&amp;utm_source=bb-slack&amp;utm_medium=referral-external|stride-api-java>",
     *             "footer_icon": "https://bytebucket.org/ravatar/%7B20ee4d9e-34f0-4789-b949-053f632f8db3%7D?ts=java",
     *             "fields": [
     *                 {
     *                     "title": "Description",
     *                     "value": "some-user-pr created online with Bitbucket",
     *                     "short": false
     *                 },
     *                 {
     *                     "title": "Reviewers",
     *                     "value": ":white_check_mark: <https://pi-devs.slack.com/messages/@UC7LU9ZGS|@Luiz Silva>, <https://bitbucket.org/trevor_thompson/?utm_term=account&amp;utm_source=bb-slack&amp;utm_medium=referral-external|Trevor Thompson>",
     *                     "short": false
     *                 }
     *             ]
     *         }
     *     ]
     *  }
     * }
     * </pre>
     *
     * @throws com.atlassian.bitbucket.AuthorisationException from underlying calls to Bitbucket API.
     */
    public Attachment pullRequest(final PullRequest pullRequest,
                                  @Nullable final Comment comment,
                                  @Nullable final Commit commit) {
        final PullRequestRef from = pullRequest.getFromRef();
        final PullRequestRef to = pullRequest.getToRef();
        final Repository repository = to.getRepository();

        if (comment != null) {
            final String title = i18nResolver.getText(
                    "bitbucket.plugins.slack.messages.unfurl.pr.title.comment.on",
                    slackLinkRenderer.replyPullRequestCommentUrl(pullRequest, comment.getId()),
                    escapeSignsForSlackLink(pullRequest.getTitle()),
                    pullRequest.getState().name(),
                    slackLinkRenderer.refLink(repository, from),
                    slackLinkRenderer.refLink(repository, to));
            final ApplicationUser author = comment.getAuthor();
            final String text = abbreviateText(comment.getText());

            return baseMessage(repository, author)
                    .text(title + text)
                    .fallback(text)
                    .ts(dateToSlackTimestamp(comment.getUpdatedDate()))
                    .build();
        }

        if (commit != null) {
            final String title = i18nResolver.getText(
                    "bitbucket.plugins.slack.messages.unfurl.pr.title.commit.on",
                    slackLinkRenderer.pullRequestCommitUrl(pullRequest, commit),
                    commit.getDisplayId(),
                    escapeSignsForSlackLink(pullRequest.getTitle()),
                    pullRequest.getState().name(),
                    slackLinkRenderer.refLink(repository, from),
                    slackLinkRenderer.refLink(repository, to));
            final String text = abbreviateText(commit.getMessage());

            return baseMessage(repository, commit.getAuthor())
                    .text(title + text)
                    .fallback(text)
                    .ts(dateToSlackTimestamp(commit.getAuthorTimestamp()))
                    .build();
        }

        final String title = i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl.pr.title",
                slackLinkRenderer.pullRequestLink(pullRequest),
                pullRequest.getState().name(),
                slackLinkRenderer.refLink(repository, from),
                slackLinkRenderer.refLink(repository, to));
        final String fallback = i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl.pr.fallback",
                pullRequest.getTitle(),
                pullRequest.getState().name(),
                from.getDisplayId(),
                to.getDisplayId());

        final List<Field> fields = new ArrayList<>();
        final String reviewers = pullRequest.getReviewers().stream()
                .sorted(Comparator.comparing(u -> u.getUser().getDisplayName()))
                .map(u -> getIconStatus(u.getStatus()) + slackLinkRenderer.userLink(u.getUser()))
                .collect(Collectors.joining(", "));

        final long taskCount = commentService.countComments(new CommentSearchRequest.Builder(pullRequest)
                .severity(CommentSeverity.BLOCKER)
                .state(CommentState.OPEN)
                .build());

        if (!reviewers.isEmpty()) {
            fields.add(Field.builder()
                    .title(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.pr.field.reviewers.title"))
                    .value(reviewers)
                    .valueShortEnough(taskCount > 0)
                    .build());
        }
        if (taskCount > 0) {
            fields.add(Field.builder()
                    .title(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.pr.field.tasks.title"))
                    .value(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.pr.field.tasks.value", taskCount))
                    .valueShortEnough(true)
                    .build());
        }

        final ApplicationUser author = pullRequest.getAuthor().getUser();
        return baseMessage(repository, author)
                .text(title)
                .fallback(fallback)
                .fields(fields)
                .ts(dateToSlackTimestamp(pullRequest.getUpdatedDate()))
                .build();
    }

    private String getIconStatus(final PullRequestParticipantStatus status) {
        if (status == PullRequestParticipantStatus.APPROVED) {
            return ":white_check_mark: ";
        }
        if (status == PullRequestParticipantStatus.NEEDS_WORK) {
            return ":warning: ";
        }
        return "";
    }

    /**
     * @throws com.atlassian.bitbucket.AuthorisationException from underlying calls to Bitbucket API
     */
    public Attachment repository(final Repository repository) {
        final long pullRequestCount = pullRequestService.count(new PullRequestSearchRequest.Builder()
                .fromRepositoryId(repository.getId())
                .state(PullRequestState.OPEN)
                .build());
        final String scm = scmService.getScmName(repository);
        final Page<Branch> branches = refService.getBranches(
                new RepositoryBranchesRequest.Builder(repository).build(),
                PageUtils.newRequest(0, 100));
        final String branchCountStr = branches.getSize() + (branches.getIsLastPage() ? "" : "+");
        final Page<Repository> forks = repositoryService.findByOrigin(
                repository,
                PageUtils.newRequest(0, 100));
        final String forkCountStr = forks.getSize() + (forks.getIsLastPage() ? "" : "+");

        final List<Field> fields = new ArrayList<>();
        if (pullRequestCount > 0) {
            fields.add(Field.builder()
                    .title(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.repo.field.open.prs.title"))
                    .value(String.valueOf(pullRequestCount))
                    .valueShortEnough(true)
                    .build());
        }
        if (branches.getSize() > 0) {
            fields.add(Field.builder()
                    .title(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.repo.field.branches.title"))
                    .value(branchCountStr)
                    .valueShortEnough(true)
                    .build());
        }
        if (forks.getSize() > 0) {
            fields.add(Field.builder()
                    .title(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.repo.field.forks.title"))
                    .value(forkCountStr)
                    .valueShortEnough(true)
                    .build());
        }
        fields.add(Field.builder()
                .title(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.repo.field.scm.title"))
                .value(scm)
                .valueShortEnough(true)
                .build());

        return baseMessage(repository, null)
                .title(repository.getName())
                .titleLink(slackLinkRenderer.repoUrl(repository))
                .fields(fields)
                .build();
    }

    /**
     * @throws com.atlassian.bitbucket.AuthorisationException from underlying calls to Bitbucket API
     */
    public Attachment commit(final Commit commit, @Nullable final Comment comment) {
        final Repository repository = commit.getRepository();

        if (comment != null) {
            final String title = i18nResolver.getText(
                    "bitbucket.plugins.slack.messages.unfurl.commit.title.with.comment",
                    slackLinkRenderer.commitCommentUrl(commit, comment.getId()),
                    slackLinkRenderer.commitLink(commit),
                    slackLinkRenderer.repoLink(repository));
            final String text = abbreviateText(comment.getText());

            return baseMessage(repository, comment.getAuthor())
                    .text(title + text)
                    .fallback(text)
                    .ts(dateToSlackTimestamp(comment.getUpdatedDate()))
                    .build();
        }

        final String title = i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl.commit.title",
                slackLinkRenderer.commitLink(commit),
                slackLinkRenderer.repoLink(repository));
        final String text = abbreviateText(commit.getMessage());

        return baseMessage(repository, commit.getAuthor())
                .text(title + text)
                .fallback(text)
                .ts(dateToSlackTimestamp(commit.getAuthorTimestamp()))
                .build();
    }

    /**
     * @throws com.atlassian.bitbucket.AuthorisationException from underlying calls to Bitbucket API
     */
    public Attachment branchOrTag(final Ref ref, final Repository repository) {
        final List<Commit> commits = commitService.getCommits(
                new CommitsRequest.Builder(repository, ref.getId())
                        .maxMessageLength(1000)
                        .merges(CommitListMergeFilter.EXCLUDE)
                        .build(),
                PageUtils.newRequest(0, MAX_COMMITS_TO_SHOW))
                .stream()
                .collect(Collectors.toList());
        final Person lastCommitAuthor = commits.size() > 0 ? commits.get(commits.size() - 1).getAuthor() : null;
        final String lastCommitTimestamp = commits.size() > 0 ? dateToSlackTimestamp(commits.get(commits.size() - 1).getAuthorTimestamp()) : null;
        final String refTypeStr = ref.getType().toString().toLowerCase();

        final String title = i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl." + refTypeStr + ".title",
                slackLinkRenderer.refLink(repository, ref),
                slackLinkRenderer.repoLink(repository));
        final String fallback = i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl." + refTypeStr + ".title.fallback",
                ref.getDisplayId(),
                repository.getName());

        final String formattedCommits = slackLinkRenderer.formatCommitList(commits);
        final String text = formattedCommits.isEmpty()
                ? ""
                : "\n" + i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.ref.commits") + "\n" + formattedCommits;

        return baseMessage(repository, lastCommitAuthor)
                .text(title + text)
                .fallback(fallback)
                .ts(lastCommitTimestamp)
                .build();
    }

    public Attachment file(final FileDto file, final String fileUrl) {
        Repository repository = file.getRepository();
        Commit commit = file.getCommit();
        String commitId = commit.getId();
        String filePath = file.getPath();
        ContentTreeNode.Type fileType = fileService.getType(repository, commitId, filePath);
        String fileTypeLabel = StringUtils.capitalize(fileType.name().toLowerCase());

        String text;
        String fileLink = slackLinkRenderer.fileLink(file.getName(), fileUrl);
        if (file.getRef().isPresent()) {
            Ref ref = file.getRef().get();
            String textResource = ref.getType() == StandardRefType.BRANCH
                    ? "bitbucket.plugins.slack.messages.unfurl.path.in.branch.title"
                    : "bitbucket.plugins.slack.messages.unfurl.path.at.tag.title";
            text = i18nResolver.getText(textResource,
                    fileTypeLabel, fileLink, slackLinkRenderer.refLink(repository, ref));
        } else if (file.getPullRequest().isPresent()) {
            PullRequest pullRequest = file.getPullRequest().get();
            text = i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.path.in.pullRequest.title",
                    fileTypeLabel, fileLink, slackLinkRenderer.pullRequestLink(pullRequest));
        } else {
            text = i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.path.in.commit.title",
                    fileTypeLabel, fileLink, slackLinkRenderer.commitLink(commit));
        }

        // add file content if any line range is selected
        List<LineRange> lineRanges = file.getLineRanges();
        String fileContentPreview = "";
        if (!lineRanges.isEmpty()) {
            String fileContent;
            FileContentHolder contentHolder = fileService.loadFileContent(repository, commitId, filePath, lineRanges);
            if (contentHolder.isBinary()) {
                fileContent = "<binary>";
            } else {
                List<SimpleLine> lines = contentHolder.getLines();
                if (lines.isEmpty()) {
                    fileContent = "<empty>";
                } else {
                    SimpleLine lastLine = lines.get(lines.size() - 1);
                    int maxLineNumberLength = String.valueOf(lastLine.getNumber()).length();
                    fileContent = lines.stream()
                            .map(line -> leftPad(String.valueOf(line.getNumber()), maxLineNumberLength)
                                    + " "
                                    + line.getText())
                            .collect(Collectors.joining("\n"));
                }
            }
            fileContentPreview = "\n" + slackLinkRenderer.slackMultilineCode(fileContent);
        }

        return baseMessage(repository, null)
                .text(text + fileContentPreview)
                .build();
    }

    private AttachmentBuilder baseMessage(final Repository repository, @Nullable final Person author) {
        AttachmentBuilder attachmentBuilder = Attachment.builder()
                .footer(slackLinkRenderer.repoLink(repository))
                .footerIcon(slackLinkRenderer.projectAvatarUrl(repository.getProject()))
                .mrkdwnIn(asList("text", "fields"));
        if (author != null) {
            attachmentBuilder = attachmentBuilder.authorName(slackLinkRenderer.userName(author))
                    .authorLink(slackLinkRenderer.userUrl(author))
                    .authorIcon(slackLinkRenderer.userAvatarUrl(author));
        }
        return attachmentBuilder;
    }

    private String dateToSlackTimestamp(final Date date) {
        return String.valueOf(date.getTime() / 1000);
    }

    private String abbreviateText(final String text) {
        return text != null
                ? "\n" + StringUtils.abbreviate(text, 1000).split("[\\n\\r]", 1)[0]
                : "";
    }
}

