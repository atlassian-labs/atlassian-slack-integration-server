package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import com.atlassian.bitbucket.avatar.AvatarRequest;
import com.atlassian.bitbucket.avatar.AvatarService;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.nav.NavBuilder;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.Ref;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.Person;
import com.atlassian.sal.api.message.I18nResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.atlassian.plugins.slack.util.LinkHelper.decorateWithOrigin;
import static com.atlassian.plugins.slack.util.SlackHelper.escapeSignsForSlackLink;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringBefore;

/**
 * Generates notifications in Slack format.
 */
@SuppressWarnings("WeakerAccess")
@Component
public class SlackLinkRenderer {
    public static final String SLACK_PATH = "slack";
    public static final String OAUTH_SESSIONS_PATH = "view-oauth-sessions";
    public static final String CONFIGURE_PATH = "configure";

    private final NavBuilder navBuilder;
    private final I18nResolver i18nResolver;
    private final AvatarService avatarService;

    @Autowired
    public SlackLinkRenderer(final NavBuilder navBuilder,
                             final I18nResolver i18nResolver,
                             final AvatarService avatarService) {
        this.navBuilder = navBuilder;
        this.i18nResolver = i18nResolver;
        this.avatarService = avatarService;
    }

    public String userLink(final ApplicationUser user) {
        if (user != null) {
            final String userLink = navBuilder.user(user).buildAbsolute();
            return slackLink(userLink, user.getDisplayName(), "user");
        } else {
            return i18nResolver.getText("plugins.slack.common.anonymous");
        }
    }

    public String userUrl(final Person user) {
        if (user instanceof ApplicationUser) {
            return decorateWithOrigin(navBuilder.user((ApplicationUser) user).buildAbsolute(), "user");
        } else {
            return "";
        }
    }

    public String userName(final Person user) {
        if (user instanceof ApplicationUser) {
            return ((ApplicationUser) user).getDisplayName();
        } else if (user != null) {
            return user.getName();
        } else {
            return "";
        }
    }

    public String userAvatarUrl(final Person user) {
        return avatarService.getUrlForPerson(user, new AvatarRequest(true, 24, true));
    }

    public String repoLink(final Repository repository) {
        String repoFullName = repository.getProject().getName() + "/" + repository.getName();
        return slackLink(repoUrl(repository), repoFullName, "repository");
    }

    public String repoUrl(final Repository repository) {
        return decorateWithOrigin(navBuilder
                .repo(repository)
                .buildAbsolute(), "repository");
    }

    public String projectAvatarUrl(final Project project) {
        return navBuilder.project(project).avatar(24).buildAbsolute();
    }

    public String commitLink(final Commit commit) {
        String commitShortId = commit.getDisplayId();
        return slackCode(slackLink(commitUrl(commit), commitShortId, "commit"));
    }

    public String commitUrl(final Commit commit) {
        return decorateWithOrigin(navBuilder
                .repo(commit.getRepository())
                .commit(commit.getId())
                .buildAbsolute(), "commit");
    }

    public String commitCommentUrl(final Commit commit, final long commentId) {
        return decorateWithOrigin(navBuilder.repo(commit.getRepository())
                .commit(commit.getId())
                .comment(commentId)
                .buildAbsolute(), "comment");
    }

    public String refLink(final Repository repository, final Ref ref) {
        String commitShortId = ref.getDisplayId();
        String type = ref.getType() instanceof Enum ? ((Enum) ref.getType()).name().toLowerCase() : "";
        return slackCode(slackLink(refUrl(repository, ref), commitShortId, type));
    }

    public String refUrl(final Repository repository, final Ref ref) {
        String type = ref.getType() instanceof Enum ? ((Enum) ref.getType()).name().toLowerCase() : "";
        return decorateWithOrigin(navBuilder
                .repo(repository)
                .browse()
                .atRevision(ref)
                .buildAbsolute(), type);
    }

    public String pullRequestLink(final PullRequest pullRequest) {
        String pullRequestUrl = navBuilder.repo(pullRequest.getToRef().getRepository())
                .pullRequest(pullRequest.getId())
                .buildAbsolute();
        return slackLink(pullRequestUrl, pullRequest.getTitle(), "pullRequest");
    }

    public String replyPullRequestCommentUrl(final PullRequest pullRequest, final long commentId) {
        return decorateWithOrigin(navBuilder.repo(pullRequest.getToRef().getRepository())
                .pullRequest(pullRequest.getId())
                .comment(commentId)
                .reply()
                .buildAbsolute(), "pullRequestComment");
    }


    public String pullRequestCommentUrl(final PullRequest pullRequest, final long commentId) {
        return decorateWithOrigin(navBuilder.repo(pullRequest.getToRef().getRepository())
                .pullRequest(pullRequest.getId())
                .comment(commentId)
                .buildAbsolute(), "pullRequestComment");
    }

    public String pullRequestCommitUrl(final PullRequest pullRequest, final Commit commit) {
        return decorateWithOrigin(navBuilder.repo(pullRequest.getToRef().getRepository())
                .pullRequest(pullRequest.getId())
                .commit(commit.getId())
                .buildAbsolute(), "pullRequestCommit");
    }

    public String fileLink(final String filePath, final String fileUrl) {
        return slackLink(fileUrl, filePath, "file");
    }

    public String oAuthSessionsPageUrl() {
        return navBuilder
                .pluginServlets()
                .path(SLACK_PATH, OAUTH_SESSIONS_PATH)
                .buildAbsolute();
    }

    public String adminConfigurationPage(final String teamId) {
        return navBuilder
                .pluginServlets()
                .path(SLACK_PATH, CONFIGURE_PATH)
                .withParam("teamId", teamId)
                .buildAbsolute();
    }

    public String homePage() {
        return decorateWithOrigin(navBuilder.dashboard().buildAbsolute(), "site");
    }

    public String formatCommitList(final List<Commit> commits) {
        return commits.stream()
                .map(commit -> String.format("%s %s", commitLink(commit), trimCommitMessage(commit.getMessage())))
                .collect(Collectors.joining("\n"));
    }

    private String trimCommitMessage(final String commitMessage) {
        final String commitMessageWithoutLineBreaks = substring(substringBefore(commitMessage, "\n"), 0, 200);
        boolean isAbbreviated = commitMessage.length() != commitMessageWithoutLineBreaks.length();
        return commitMessageWithoutLineBreaks + (isAbbreviated ? "..." : "");
    }

    private String slackCode(final String code) {
        return "`" + code + "`";
    }

    private String slackLink(final String link, final String text, final String type) {
        return "<" + decorateWithOrigin(link, type) + "|" + escapeSignsForSlackLink(text) + ">";
    }

    public String slackMultilineQuote(final String text) {
        return ">>>" + text;
    }

    public String slackMultilineCode(final String text) {
        return "```" + text + "```";
    }
}
