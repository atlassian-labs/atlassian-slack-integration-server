package com.atlassian.jira.plugins.slack.servicedesk;

import com.atlassian.query.clause.AndClause;
import com.atlassian.query.clause.Clause;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.clause.TerminalClauseImpl;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operand.SingleValueOperand;
import com.atlassian.query.operator.Operator;
import io.atlassian.fugue.Option;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer4;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FixRequestTypeClauseVisitorTest {
    @Mock
    FixRequestTypeOperandVisitor operandVisitor;
    @Mock
    ServiceDeskCompatibilityDispatcher dispatcher;
    @Mock
    ServiceDeskCompatibilityHelper helper;

    private MockedStatic<ServiceDeskCompatibilityDispatcher> mockedDispatcher;
    private MockedConstruction<FixRequestTypeOperandVisitor> mockedConstructor;

    @InjectMocks
    FixRequestTypeClauseVisitor target;

    @Before
    public void setUp() {
        target = new FixRequestTypeClauseVisitor(null);
    }

    @After
    public void tearDown() {
        if (mockedDispatcher != null) {
            mockedDispatcher.close();
        }
        if (mockedConstructor != null) {
            mockedConstructor.close();
        }
    }

    @Test
    public void acceptInvokesOperandVisitor() throws Exception {
        SingleValueOperand reqTypeOperand = new SingleValueOperand("Req type name");
        AndClause whereClause = new AndClause(
                new TerminalClauseImpl("Customer Request Type", Operator.EQUALS, reqTypeOperand),
                new TerminalClauseImpl("summary", Operator.NOT_EQUALS, new SingleValueOperand("ignore")));

        String typeKey = "replacedTypeKey";
        mockedConstructor = mockConstruction(FixRequestTypeOperandVisitor.class, (mock, context) -> {
            when(mock.visit(any(SingleValueOperand.class))).thenReturn(new SingleValueOperand(typeKey));
            when(mock.isOperandChanged()).thenReturn(true);
        });
        mockedDispatcher = mockStatic(ServiceDeskCompatibilityDispatcher.class);
        mockedDispatcher.when(ServiceDeskCompatibilityDispatcher::getInstance).thenReturn(dispatcher);
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
