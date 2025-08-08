package com.atlassian.jira.plugins.slack.servicedesk;

import com.atlassian.jira.project.Project;
import com.atlassian.query.clause.AndClause;
import com.atlassian.query.clause.ChangedClause;
import com.atlassian.query.clause.Clause;
import com.atlassian.query.clause.ClauseVisitor;
import com.atlassian.query.clause.NotClause;
import com.atlassian.query.clause.OrClause;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.clause.WasClause;
import com.atlassian.query.operand.MultiValueOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operand.SingleValueOperand;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class FixRequestTypeClauseVisitor implements ClauseVisitor<Clause> {
    private final Project project;

    @Getter
    private boolean clauseChanged;

    public FixRequestTypeClauseVisitor(final Project project) {
        this.project = project;
    }

    @Override
    public Clause visit(final AndClause clause) {
        return fixNestedClauses(clause);
    }

    @Override
    public Clause visit(final NotClause clause) {
        return fixNestedClauses(clause);
    }

    @Override
    public Clause visit(final OrClause clause) {
        return fixNestedClauses(clause);
    }

    @Override
    public Clause visit(final TerminalClause clause) {
        String clauseName = clause.getName();
        Operand oldOperand = clause.getOperand();

        // not a customer request type clause we have to fix; return it unchanged
        if (!"Customer Request Type".equalsIgnoreCase(clauseName)
                || !(oldOperand instanceof SingleValueOperand || oldOperand instanceof MultiValueOperand)) {
            return clause;
        }

        FixRequestTypeOperandVisitor visitor = new FixRequestTypeOperandVisitor(project);
        Operand fixedOperand = oldOperand.accept(visitor);
        Clause fixedClause = clause;
        if (visitor.isOperandChanged()) {
            Optional<ServiceDeskCompatibilityHelper> helper = ServiceDeskCompatibilityDispatcher.getInstance().getHelper();
            Optional<Object> property = helper.map(h -> h.getTerminalClauseProperty(clause));
            fixedClause = helper.map(h -> h.createTerminalClause(clauseName, clause.getOperator(), fixedOperand, property.orElse(null)))
                    .orElse(clause);

            clauseChanged = true;
        }

        return fixedClause;
    }

    @Override
    public Clause visit(final WasClause clause) {
        return clause;
    }

    @Override
    public Clause visit(final ChangedClause clause) {
        return clause;
    }

    private Clause fixNestedClauses(Clause clause) {
        List<Clause> fixedNestedClauses = new ArrayList<>();
        boolean isNestedClauseChanged = false;

        for (Clause oldNestedClause : clause.getClauses()) {
            FixRequestTypeClauseVisitor visitor = new FixRequestTypeClauseVisitor(project);
            fixedNestedClauses.add(oldNestedClause.accept(visitor));

            isNestedClauseChanged |= visitor.isClauseChanged();
        }

        Clause fixedClause = clause;
        if (isNestedClauseChanged) {
            fixedClause = buildNewNonTerminalClause(clause, fixedNestedClauses);

            clauseChanged = true;
        }

        return fixedClause;
    }

    private Clause buildNewNonTerminalClause(Clause oldClause, List<Clause> nestedClauses) {
        Clause newClause = oldClause;
        if (oldClause instanceof AndClause) {
            newClause = new AndClause(nestedClauses);
        } else if (oldClause instanceof OrClause) {
            newClause = new OrClause(nestedClauses);
        } else if (oldClause instanceof NotClause) {
            newClause = new NotClause(nestedClauses.get(0));
        } else {
            log.error("Unexpected clause of type {}", oldClause.getClass());
        }

        return newClause;
    }
}
