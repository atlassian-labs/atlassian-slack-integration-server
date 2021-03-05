package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.icon.IconType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.renderer.IssueRenderContext;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.status.SimpleStatus;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.plugins.slack.model.event.DefaultJiraPostFunctionEvent;
import com.atlassian.jira.plugins.slack.service.notification.impl.JiraPostFunctionEventRenderer;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectCategory;
import com.atlassian.jira.project.type.ProjectTypeKey;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.templaterenderer.RenderingException;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressWarnings("deprecation")
@Path("message")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class SlackMessageResource {
    private final JiraPostFunctionEventRenderer eventRenderer;
    private final JiraAuthenticationContext authenticationContext;

    @Autowired
    public SlackMessageResource(final JiraPostFunctionEventRenderer eventRenderer,
                                final JiraAuthenticationContext authenticationContext) {
        this.eventRenderer = eventRenderer;
        this.authenticationContext = authenticationContext;
    }

    @POST
    @Path("render")
    public Response renderMessage(final MessageBean messageBean) {
        try {
            final ApplicationUser user = authenticationContext.getLoggedInUser();
            final DefaultJiraPostFunctionEvent.Builder builder = new DefaultJiraPostFunctionEvent.Builder()
                    .setActor(authenticationContext.getLoggedInUser())
                    .setCustomMessageFormat(messageBean.message)
                    .setIssue(getDummyIssue(user))
                    .setFirstStepName("Status A")
                    .setEndStepName("Status B")
                    .setActionName("Transition");

            return eventRenderer.renderPreviewNotification(builder.build()).fold(
                    e -> Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build(),
                    messageBuilder -> {
                        ChatPostMessageRequest message = messageBuilder.build();
                        String text = (StringUtils.isBlank(message.getText()) ? "" : message.getText() + "\n") +
                                message.getAttachments().get(0).getTitle() + "\n" +
                                StringUtils.defaultIfBlank(
                                        message.getAttachments().get(0).getPretext(),
                                        message.getAttachments().get(0).getText()
                                );
                        return Response.ok(new MessageBean(text)).build();
                    }
            );
        } catch (RenderingException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MessageBean {
        @JsonProperty
        String message;

        @SuppressWarnings("unused")
        MessageBean() {
        }

        MessageBean(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private Issue getDummyIssue(final ApplicationUser user) {
        return new Issue() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public GenericValue getProject() {
                return null;
            }

            @Override
            public Project getProjectObject() {
                return new Project() {
                    @Override
                    public Long getId() {
                        return 10001L;
                    }

                    @Override
                    public String getName() {
                        return "My First Project";
                    }

                    @Override
                    public String getKey() {
                        return "MFP";
                    }

                    @Override
                    public String getUrl() {
                        return "";
                    }

                    @Override
                    public String getEmail() {
                        return "";
                    }

                    @Override
                    public ApplicationUser getLead() {
                        return null;
                    }

                    @Override
                    public String getLeadUserName() {
                        return "";
                    }

                    @Override
                    public String getDescription() {
                        return "Super special project description";
                    }

                    @Override
                    public Long getAssigneeType() {
                        return 1L;
                    }

                    @Override
                    public ProjectTypeKey getProjectTypeKey() {
                        return null;
                    }

                    @Override
                    public Collection<ProjectComponent> getComponents() {
                        return Collections.emptyList();
                    }

                    @Override
                    public Collection<ProjectComponent> getProjectComponents() {
                        return Collections.emptyList();
                    }

                    @Override
                    public Collection<Version> getVersions() {
                        return Collections.emptyList();
                    }

                    @Override
                    public Collection<IssueType> getIssueTypes() {
                        return Collections.emptyList();
                    }

                    @Override
                    public ProjectCategory getProjectCategory() {
                        return null;
                    }

                    @Override
                    public ProjectCategory getProjectCategoryObject() {
                        return null;
                    }

                    @Override
                    public GenericValue getGenericValue() {
                        return null;
                    }

                    @Nonnull
                    @Override
                    public Avatar getAvatar() {
                        return new Avatar() {
                            @Nonnull
                            @Override
                            public Type getAvatarType() {
                                return Type.PROJECT;
                            }

                            @Nonnull
                            @Override
                            public IconType getIconType() {
                                return IconType.PROJECT_ICON_TYPE;
                            }

                            @Nonnull
                            @Override
                            public String getFileName() {
                                return "";
                            }

                            @Nonnull
                            @Override
                            public String getContentType() {
                                return "";
                            }

                            @Override
                            public Long getId() {
                                return 0L;
                            }

                            @Override
                            public String getOwner() {
                                return "";
                            }

                            @Override
                            public boolean isSystemAvatar() {
                                return false;
                            }
                        };
                    }

                    @Override
                    public ApplicationUser getProjectLead() {
                        return null;
                    }

                    @Override
                    public String getLeadUserKey() {
                        return null;
                    }

                    @Override
                    public String getOriginalKey() {
                        return null;
                    }

                    @Override
                    public boolean isArchived() {
                        return false;
                    }
                };
            }

            @Override
            public Long getProjectId() {
                return 11L;
            }

            @Override
            public IssueType getIssueType() {
                return new IssueType() {
                    @Override
                    public boolean isSubTask() {
                        return false;
                    }

                    @Nullable
                    @Override
                    public Avatar getAvatar() {
                        return null;
                    }

                    @Override
                    public String getId() {
                        return "task";
                    }

                    @Override
                    public String getName() {
                        return "Task";
                    }

                    @Override
                    public String getDescription() {
                        return getName();
                    }

                    @Override
                    public Long getSequence() {
                        return 0L;
                    }

                    @Override
                    public String getCompleteIconUrl() {
                        return "";
                    }

                    @Override
                    public String getIconUrl() {
                        return "";
                    }

                    @Override
                    public String getIconUrlHtml() {
                        return "";
                    }

                    @Override
                    public String getNameTranslation() {
                        return getName();
                    }

                    @Override
                    public String getDescTranslation() {
                        return getName();
                    }

                    @Override
                    public String getNameTranslation(final String locale) {
                        return getName();
                    }

                    @Override
                    public String getDescTranslation(final String locale) {
                        return getName();
                    }

                    @Override
                    public String getNameTranslation(final I18nHelper i18n) {
                        return getName();
                    }

                    @Override
                    public String getDescTranslation(final I18nHelper i18n) {
                        return getName();
                    }

                    @SuppressWarnings("NullableProblems")
                    @Override
                    public int compareTo(final Object o) {
                        return 0;
                    }
                };
            }

            @Override
            public IssueType getIssueTypeObject() {
                return null;
            }

            @Override
            public String getIssueTypeId() {
                return "";
            }

            @Override
            public String getSummary() {
                return "An important issue";
            }

            @Override
            public ApplicationUser getAssigneeUser() {
                return user;
            }

            @Override
            public ApplicationUser getAssignee() {
                return user;
            }

            @Override
            public String getAssigneeId() {
                return "";
            }

            @Override
            public Collection<ProjectComponent> getComponentObjects() {
                return Collections.emptyList();
            }

            @Override
            public Collection<ProjectComponent> getComponents() {
                return Collections.emptyList();
            }

            @Override
            public ApplicationUser getReporterUser() {
                return user;
            }

            @Override
            public ApplicationUser getReporter() {
                return user;
            }

            @Override
            public String getReporterId() {
                return "";
            }

            @Override
            public ApplicationUser getCreator() {
                return user;
            }

            @Override
            public String getCreatorId() {
                return "";
            }

            @Override
            public String getDescription() {
                return "Issue description goes here";
            }

            @Override
            public String getEnvironment() {
                return "Production";
            }

            @Override
            public Collection<Version> getAffectedVersions() {
                return Collections.emptyList();
            }

            @Override
            public Collection<Version> getFixVersions() {
                return Collections.emptyList();
            }

            @Override
            public Timestamp getDueDate() {
                return new Timestamp(System.currentTimeMillis());
            }

            @Override
            public GenericValue getSecurityLevel() {
                return null;
            }

            @Override
            public Long getSecurityLevelId() {
                return 0L;
            }

            @Override
            public Priority getPriority() {
                return new Priority() {
                    @Override
                    public String getStatusColor() {
                        return "";
                    }

                    @Override
                    public String getSvgIconUrl() {
                        return "";
                    }

                    @Override
                    public String getRasterIconUrl() {
                        return "";
                    }

                    @Override
                    public String getId() {
                        return "low";
                    }

                    @Override
                    public String getName() {
                        return getId();
                    }

                    @Override
                    public String getDescription() {
                        return getId();
                    }

                    @Override
                    public Long getSequence() {
                        return 1L;
                    }

                    @Override
                    public String getCompleteIconUrl() {
                        return getId();
                    }

                    @Override
                    public String getIconUrl() {
                        return "";
                    }

                    @Override
                    public String getIconUrlHtml() {
                        return "";
                    }

                    @Override
                    public String getNameTranslation() {
                        return getId();
                    }

                    @Override
                    public String getDescTranslation() {
                        return getId();
                    }

                    @Override
                    public String getNameTranslation(final String locale) {
                        return getId();
                    }

                    @Override
                    public String getDescTranslation(final String locale) {
                        return getId();
                    }

                    @Override
                    public String getNameTranslation(final I18nHelper i18n) {
                        return getId();
                    }

                    @Override
                    public String getDescTranslation(final I18nHelper i18n) {
                        return getId();
                    }

                    @SuppressWarnings("NullableProblems")
                    @Override
                    public int compareTo(final Object o) {
                        return 0;
                    }
                };
            }

            @Nullable
            @Override
            public Priority getPriorityObject() {
                return null;
            }

            @Override
            public String getResolutionId() {
                return "";
            }

            @Override
            public Resolution getResolution() {
                return null;
            }

            @Override
            public Resolution getResolutionObject() {
                return null;
            }

            @Override
            public String getKey() {
                return "KEY-1";
            }

            @Override
            public Long getNumber() {
                return 0L;
            }

            @Override
            public Long getVotes() {
                return 2L;
            }

            @Override
            public Long getWatches() {
                return 2L;
            }

            @Override
            public Timestamp getCreated() {
                return new Timestamp(System.currentTimeMillis());
            }

            @Override
            public Timestamp getUpdated() {
                return new Timestamp(System.currentTimeMillis());
            }

            @Override
            public Timestamp getResolutionDate() {
                return new Timestamp(System.currentTimeMillis());
            }

            @Override
            public Long getWorkflowId() {
                return 0L;
            }

            @Override
            public Object getCustomFieldValue(final CustomField customField) {
                return null;
            }

            @Override
            public Status getStatus() {
                return new Status() {
                    @Override
                    public StatusCategory getStatusCategory() {
                        return new StatusCategory() {
                            @Override
                            public Long getId() {
                                return 1L;
                            }

                            @Override
                            public String getTranslatedName() {
                                return "Done";
                            }

                            @Override
                            public String getTranslatedName(final String locale) {
                                return "Done";
                            }

                            @Override
                            public String getTranslatedName(final I18nHelper i18n) {
                                return "Done";
                            }

                            @Override
                            public String getKey() {
                                return "Done";
                            }

                            @Override
                            public String getName() {
                                return "Done";
                            }

                            @Override
                            public List<String> getAliases() {
                                return Collections.emptyList();
                            }

                            @Override
                            public String getPrimaryAlias() {
                                return "Done";
                            }

                            @Override
                            public String getColorName() {
                                return "";
                            }

                            @Override
                            public Long getSequence() {
                                return 1L;
                            }

                            @SuppressWarnings("NullableProblems")
                            @Override
                            public int compareTo(final StatusCategory o) {
                                return 0;
                            }
                        };
                    }

                    @Override
                    public SimpleStatus getSimpleStatus() {
                        return null;
                    }

                    @Override
                    public SimpleStatus getSimpleStatus(final I18nHelper i18nHelper) {
                        return null;
                    }

                    @Override
                    public String getCompleteIconUrl() {
                        return "";
                    }

                    @Override
                    public String getIconUrl() {
                        return "";
                    }

                    @Override
                    public String getId() {
                        return "Done";
                    }

                    @Override
                    public String getName() {
                        return "Done";
                    }

                    @Override
                    public String getDescription() {
                        return "Done";
                    }

                    @Override
                    public Long getSequence() {
                        return 1L;
                    }

                    @Override
                    public String getIconUrlHtml() {
                        return "";
                    }

                    @Override
                    public String getNameTranslation() {
                        return "Done";
                    }

                    @Override
                    public String getDescTranslation() {
                        return "Done";
                    }

                    @Override
                    public String getNameTranslation(final String locale) {
                        return "Done";
                    }

                    @Override
                    public String getDescTranslation(final String locale) {
                        return "Done";
                    }

                    @Override
                    public String getNameTranslation(final I18nHelper i18n) {
                        return "Done";
                    }

                    @Override
                    public String getDescTranslation(final I18nHelper i18n) {
                        return "Done";
                    }

                    @SuppressWarnings("NullableProblems")
                    @Override
                    public int compareTo(final Object o) {
                        return 0;
                    }
                };
            }

            @Override
            public String getStatusId() {
                return "Done";
            }

            @Override
            public Status getStatusObject() {
                return null;
            }

            @Override
            public Long getOriginalEstimate() {
                return 10L;
            }

            @Override
            public Long getEstimate() {
                return 10L;
            }

            @Override
            public Long getTimeSpent() {
                return 0L;
            }

            @Override
            public Object getExternalFieldValue(final String fieldId) {
                return null;
            }

            @Override
            public boolean isSubTask() {
                return false;
            }

            @Override
            public Long getParentId() {
                return 0L;
            }

            @Override
            public boolean isCreated() {
                return true;
            }

            @Override
            public Issue getParentObject() {
                return null;
            }

            @Override
            public GenericValue getParent() {
                return null;
            }

            @Override
            public Collection<GenericValue> getSubTasks() {
                return Collections.emptyList();
            }

            @Override
            public Collection<Issue> getSubTaskObjects() {
                return Collections.emptyList();
            }

            @Override
            public boolean isEditable() {
                return false;
            }

            @Override
            public IssueRenderContext getIssueRenderContext() {
                return null;
            }

            @Override
            public Collection<Attachment> getAttachments() {
                return Collections.emptyList();
            }

            @Override
            public Set<Label> getLabels() {
                return Collections.emptySet();
            }

            @Override
            public boolean isArchived() {
                return false;
            }

            @Override
            public ApplicationUser getArchivedByUser() {
                return null;
            }

            @Override
            public String getArchivedById() {
                return null;
            }

            @Override
            public Timestamp getArchivedDate() {
                return null;
            }

            @Override
            public GenericValue getGenericValue() {
                return null;
            }

            @Override
            public String getString(final String name) {
                return null;
            }

            @Override
            public Timestamp getTimestamp(final String name) {
                return new Timestamp(System.currentTimeMillis());
            }

            @Override
            public Long getLong(final String name) {
                return 1L;
            }

            @Override
            public void store() {
            }
        };
    }
}
