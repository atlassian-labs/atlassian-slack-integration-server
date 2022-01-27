package com.atlassian.plugins.slack.rest;

import com.atlassian.confluence.compat.api.service.accessmode.ReadOnlyAccessAllowed;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Attachment;
import com.sun.jersey.spi.container.ResourceFilters;
import io.atlassian.fugue.Either;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@ReadOnlyAccessAllowed
@Path("/debug")
public class DebugResource {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SlackClientProvider slackClientProvider;

    @Autowired
    public DebugResource(final SlackClientProvider slackClientProvider) {
        this.slackClientProvider = slackClientProvider;
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    @ResourceFilters(SlackLinkAdministerPermissionResourceFilter.class)
    public Response getDebugPage() {
        final String debugPage = "<html><body><form target=_blank action='debug/submit' method=get>"
                + "Workspace ID: <input type=text name=teamId />&nbsp;&nbsp;&nbsp;"
                + "Channel ID: <input type=text name=channelId />&nbsp;&nbsp;&nbsp;"
                + "Endpoint: <input type=text name=endpoint value='chat.postMessage'/><br/><br/>"
                + "Code or form arguments: <textarea name=content style='width: 100%' rows=10>sample1</textarea>"
                + "<br> Example:<pre>name=valueNotEncoded\nname2=!!valueAlreadyEncoded</pre>"
                + "<br/><br/><input type=submit>"
                + "</form></body></html>";
        return Response.ok(debugPage).build();
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    @ResourceFilters(SlackLinkAdministerPermissionResourceFilter.class)
    @Path("/submit")
    public Response test(@QueryParam("teamId") final String teamId,
                         @QueryParam("channelId") final String channelId,
                         @QueryParam("endpoint") final String endpoint,
                         @QueryParam("content") final String content) {
        return slackClientProvider.withTeamId(teamId)
                .flatMap(slackClient -> {
                    if (content != null && content.startsWith("sample")) {
                        return slackClient.postMessage(this.buildSampleMessage(channelId, content))
                                .leftMap(ErrorResponse::getException)
                                .map(message -> message);
                    }
                    return this.postGenericApiCall(channelId, endpoint, content, slackClient);
                })
                .fold(e -> {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            pw.println(e.getMessage());
                            e.printStackTrace(pw);

                            if (e.getCause() != null) {
                                pw.println("\nCaused by " + e.getCause().getMessage());
                                e.getCause().printStackTrace(pw);
                            }

                            return Response
                                    .status(BAD_REQUEST)
                                    .entity(sw.toString())
                                    .build();
                        },
                        o -> {
                            try {
                                String jsonResult = OBJECT_MAPPER.writeValueAsString(o);
                                return Response.ok(jsonResult).build();
                            } catch (IOException e) {
                                return Response.ok("Could not convert object to json. toString() is:\n" + o.toString()).build();
                            }
                        }
                );
    }

    // Read variables from form
    private Either<Throwable, Object> postGenericApiCall(final String channelId,
                                                         final String endpoint,
                                                         final String content,
                                                         final SlackClient slackClient) {
        Map<String, String> encodedFormArgs = new HashMap<>();
        if (channelId != null && !channelId.isEmpty()) {
            encodedFormArgs.put("channel", channelId);
        }
        if (content != null) {
            final String[] lines = content.split("[\\n\\r]+");
            for (int i = 0; i < lines.length; i++) {
                final String[] nameAndValue = lines[i].split("\\s*=\\s*", 2);
                if (nameAndValue.length > 1) {
                    encodedFormArgs.put(nameAndValue[0], nameAndValue[1]);
                }
            }
        }
        return slackClient.debugApiCall(endpoint, encodedFormArgs)
                .map(response -> {
                    try {
                        return (Object) response.body().string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .leftMap(ErrorResponse::getException);
    }

    private ChatPostMessageRequest buildSampleMessage(final String channelId, final String sampleId) {
        switch (sampleId) {
            case "sample1":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 1: notification sample!!! <https://jira.perf.customer.com/secure/ViewProfile.jspa?name=Z123SFW&atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258dXNlcg%3D%3D|Some.User> has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .attachments(Collections.singletonList(Attachment.builder()
                                .fallback("PROJTEST-29 Slack notification test")
                                .color("#2684FF")
                                .title("PROJTEST-29: Slack notification test")
                                .titleLink("https://jira.perf.customer.com/browse/PROJTEST-29?atlLinkOrigin\u003dc2xhY2staW50ZWdyYXRpb258aXNzdWU%3D")
                                .text("Status: `Backlog`       Assignee: *Unassigned*       Priority: *3 - Medium*")
                                .footer("\u003chttps://jira.perf.customer.com/projects/PROJTEST?atlLinkOrigin\u003dc2xhY2staW50ZWdyYXRpb258cHJvamVjdA%3D%3D|Gandalf\u0027s Kanban Project\u003e | \u003chttps://jira.perf.customer.com?atlLinkOrigin\u003dc2xhY2staW50ZWdyYXRpb258c2l0ZQ%3D%3D|Jira\u003e")
                                .mrkdwnIn(Arrays.asList("text", "pretext"))
                                .build()))
                        .build();
            case "sample2":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 2: no back quotes!!! <https://jira.perf.customer.com/secure/ViewProfile.jspa?name=Z123SFW&atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258dXNlcg%3D%3D|Some.User> has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .attachments(Collections.singletonList(Attachment.builder()
                                .fallback("PROJTEST-29 Slack notification test")
                                .color("#2684FF")
                                .title("PROJTEST-29: Slack notification test")
                                .titleLink("https://jira.perf.customer.com/browse/PROJTEST-29?atlLinkOrigin\u003dc2xhY2staW50ZWdyYXRpb258aXNzdWU%3D")
                                .text("Status: Backlog       Assignee: *Unassigned*       Priority: *3 - Medium*")
                                .footer("\u003chttps://jira.perf.customer.com/projects/PROJTEST?atlLinkOrigin\u003dc2xhY2staW50ZWdyYXRpb258cHJvamVjdA%3D%3D|Gandalf\u0027s Kanban Project\u003e | \u003chttps://jira.perf.customer.com?atlLinkOrigin\u003dc2xhY2staW50ZWdyYXRpb258c2l0ZQ%3D%3D|Jira\u003e")
                                .mrkdwnIn(Arrays.asList("text", "pretext"))
                                .build()))
                        .build();
            case "sample3":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 3: no unicode!!! <https://jira.perf.customer.com/secure/ViewProfile.jspa?name=Z123SFW&atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258dXNlcg%3D%3D|Some.User> has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .attachments(Collections.singletonList(Attachment.builder()
                                .fallback("PROJTEST-29 Slack notification test")
                                .color("#2684FF")
                                .title("PROJTEST-29: Slack notification test")
                                .titleLink("https://jira.perf.customer.com/browse/PROJTEST-29?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258aXNzdWU%3D")
                                .text("Status: Backlog       Assignee: *Unassigned*       Priority: *3 - Medium*")
                                .footer("<https://jira.perf.customer.com/projects/PROJTEST?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258cHJvamVjdA%3D%3D|Kanban Project> | <https://jira.perf.customer.com?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258c2l0ZQ%3D%3D|Jira>")
                                .mrkdwnIn(Arrays.asList("text", "pretext"))
                                .build()))
                        .build();
            case "sample4":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 4: no query parameters in links!!! <https://jira.perf.customer.com/secure/ViewProfile.jspa|Some.User> has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .attachments(Collections.singletonList(Attachment.builder()
                                .fallback("PROJTEST-29 Slack notification test")
                                .color("#2684FF")
                                .title("PROJTEST-29: Slack notification test")
                                .titleLink("https://jira.perf.customer.com/browse/PROJTEST-29")
                                .text("Status: Backlog       Assignee: *Unassigned*       Priority: *3 - Medium*")
                                .footer("<https://jira.perf.customer.com/projects/PROJTEST|Kanban Project> | <https://jira.perf.customer.com|Jira>")
                                .mrkdwnIn(Arrays.asList("text", "pretext"))
                                .build()))
                        .build();
            case "sample5":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 5: no links!!! Some.User has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .attachments(Collections.singletonList(Attachment.builder()
                                .fallback("PROJTEST-29 Slack notification test")
                                .color("#2684FF")
                                .title("PROJTEST-29: Slack notification test")
                                .text("Status: Backlog       Assignee: *Unassigned*       Priority: *3 - Medium*")
                                .footer("Kanban Project | Jira")
                                .mrkdwnIn(Arrays.asList("text", "pretext"))
                                .build()))
                        .build();
            case "sample6":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 6: no pipe character!!! Some.User has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .attachments(Collections.singletonList(Attachment.builder()
                                .fallback("PROJTEST-29 Slack notification test")
                                .color("#2684FF")
                                .title("PROJTEST-29: Slack notification test")
                                .text("Status: Backlog       Assignee: *Unassigned*       Priority: *3 - Medium*")
                                .footer("Kanban Project Jira")
                                .mrkdwnIn(Arrays.asList("text", "pretext"))
                                .build()))
                        .build();
            case "sample7":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 7: no markdown in attachment!!! Some.User has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .attachments(Collections.singletonList(Attachment.builder()
                                .fallback("PROJTEST-29 Slack notification test")
                                .color("#2684FF")
                                .title("PROJTEST-29: Slack notification test")
                                .text("Status: Backlog       Assignee: Unassigned       Priority: 3 - Medium")
                                .footer("Kanban Project Jira")
                                .build()))
                        .build();
            case "sample8":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 8: no color in attachment!!! Some.User has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .attachments(Collections.singletonList(Attachment.builder()
                                .fallback("PROJTEST-29 Slack notification test")
                                .title("PROJTEST-29: Slack notification test")
                                .text("Status: Backlog       Assignee: Unassigned       Priority: 3 - Medium")
                                .footer("Kanban Project Jira")
                                .build()))
                        .build();
            case "sample9":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 9: simple text in attachment!!! Some.User has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .attachments(Collections.singletonList(Attachment.builder()
                                .fallback("PROJTEST-29 Slack notification test")
                                .title("PROJTEST-29: Slack notification test")
                                .text("Attachment")
                                .footer("Kanban Project Jira")
                                .build()))
                        .build();
            case "sample10":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 10: only text in attachment!!! Some.User has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .attachments(Collections.singletonList(Attachment.builder()
                                .text("Attachment")
                                .build()))
                        .build();
            case "sample11":
                return ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text("!!!Test message 12: no attachment!!! Some.User has *created* a Story")
                        .mrkdwn(true)
                        .linkNames(false)
                        .build();
        }
        return ChatPostMessageRequest.builder().text("No test message").build();
    }
}
