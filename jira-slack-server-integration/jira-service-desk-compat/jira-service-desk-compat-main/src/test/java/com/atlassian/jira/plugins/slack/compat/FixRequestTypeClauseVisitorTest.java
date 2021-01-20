package com.atlassian.jira.plugins.slack.compat;

import com.atlassian.query.clause.AndClause;
import com.atlassian.query.clause.Clause;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.clause.TerminalClauseImpl;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operand.SingleValueOperand;
import com.atlassian.query.operator.Operator;
import io.atlassian.fugue.Option;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer4;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.*")
@PrepareForTest({FixRequestTypeClauseVisitor.class, FixRequestTypeOperandVisitor.class, ServiceDeskCompatibilityDispatcher.class})
public class FixRequestTypeClauseVisitorTest {
    @Mock
    FixRequestTypeOperandVisitor operandVisitor;
    @Mock
    ServiceDeskCompatibilityDispatcher dispatcher;
    @Mock
    ServiceDeskCompatibilityHelper helper;

    @InjectMocks
    FixRequestTypeClauseVisitor target;

    @Before
    public void setUp() throws Exception {
        target = new FixRequestTypeClauseVisitor(null);
    }

    @Test
    public void acceptInvokesOperandVisitor() throws Exception {
        SingleValueOperand reqTypeOperand = new SingleValueOperand("Req type name");
        AndClause whereClause = new AndClause(
                new TerminalClauseImpl("Customer Request Type", Operator.EQUALS, reqTypeOperand),
                new TerminalClauseImpl("summary", Operator.NOT_EQUALS, new SingleValueOperand("ignore")));

        PowerMockito.mockStatic(FixRequestTypeOperandVisitor.class, ServiceDeskCompatibilityDispatcher.class);
        PowerMockito.whenNew(FixRequestTypeOperandVisitor.class).withAnyArguments().thenReturn(operandVisitor);
        String typeKey = "replacedTypeKey";
        when(operandVisitor.visit(any(SingleValueOperand.class))).thenReturn(new SingleValueOperand(typeKey));
        when(operandVisitor.isOperandChanged()).thenReturn(true);
        PowerMockito.when(ServiceDeskCompatibilityDispatcher.getInstance()).thenReturn(dispatcher);
        when(dispatcher.getHelper()).thenReturn(Optional.of(helper));
        when(helper.getTerminalClauseProperty(any())).thenReturn(Option.none());
        when(helper.createTerminalClause(anyString(), any(Operator.class), any(Operand.class), any(Option.class)))
                .thenAnswer(answer((Answer4<TerminalClauseImpl, String, Operator, Operand, Option>) TerminalClauseImpl::new));

        Clause fixedClause = whereClause.accept(target);

        assertThat(fixedClause, notNullValue());
        assertThat(target.isClauseChanged(), equalTo(true));
        TerminalClause fixedTermClause = (TerminalClause) fixedClause.getClauses().get(0);
        assertThat(fixedTermClause.getOperand().getDisplayString(), equalTo("\"" + typeKey + "\""));
    }
}
