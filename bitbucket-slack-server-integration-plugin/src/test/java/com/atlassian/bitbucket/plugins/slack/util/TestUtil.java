package com.atlassian.bitbucket.plugins.slack.util;

import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.EscalatedSecurityContext;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostEphemeralRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.block.SectionBlock;
import com.github.seratch.jslack.api.model.block.composition.MarkdownTextObject;
import lombok.experimental.UtilityClass;

import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UtilityClass
public class TestUtil {
    public static String getFirstSectionText(final ChatPostMessageRequest message) {
        return ((MarkdownTextObject) ((SectionBlock) message.getBlocks().get(0)).getText()).getText();
    }

    public static String getFirstSectionText(final ChatPostEphemeralRequest message) {
        return ((MarkdownTextObject) ((SectionBlock) message.getBlocks().get(0)).getText()).getText();
    }

    public static void bypass(final SecurityService securityService, final ApplicationUser user) {
        EscalatedSecurityContext context = mock(EscalatedSecurityContext.class);
        if (user != null) {
            when(securityService.impersonating(same(user), anyString())).thenReturn(context);
        } else{
            when(securityService.impersonating(any(), anyString())).thenReturn(context);
        }
        when(context.<Boolean, RuntimeException>call(any()))
                .thenAnswer(answer((Operation<?, RuntimeException> operation) -> operation.perform()));
    }

    public static void bypass(final TransactionTemplate transactionTemplate) {
        when(transactionTemplate.execute(any())).thenAnswer(answer((TransactionCallback op) -> op.doInTransaction()));
    }
}
