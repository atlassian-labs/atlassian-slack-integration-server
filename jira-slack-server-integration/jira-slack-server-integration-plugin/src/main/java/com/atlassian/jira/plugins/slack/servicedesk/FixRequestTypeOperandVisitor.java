package com.atlassian.jira.plugins.slack.servicedesk;

import com.atlassian.jira.project.Project;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.FunctionOperand;
import com.atlassian.query.operand.MultiValueOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operand.OperandVisitor;
import com.atlassian.query.operand.SingleValueOperand;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class FixRequestTypeOperandVisitor implements OperandVisitor<Operand> {
    private final Project project;

    @Getter
    private boolean operandChanged;
    private Map<String, String> requestTypeNameToKey;

    public FixRequestTypeOperandVisitor(final Project project) {
        this.project = project;
    }

    @Override
    public Operand visit(final EmptyOperand operand) {
        return operand;
    }

    @Override
    public Operand visit(final FunctionOperand operand) {
        return operand;
    }

    @Override
    public Operand visit(final MultiValueOperand operand) {
        List<Operand> fixedNestedOperands = new ArrayList<>();
        boolean isNestedOperandChanged = false;

        for (Operand oldNestedOperand : operand.getValues()) {
            FixRequestTypeOperandVisitor visitor = new FixRequestTypeOperandVisitor(project);
            Operand fixedNestedOperand = oldNestedOperand.accept(visitor);
            fixedNestedOperands.add(fixedNestedOperand);

            isNestedOperandChanged |= visitor.isOperandChanged();
        }

        Operand fixedOperand = operand;
        if (isNestedOperandChanged) {
            fixedOperand = new MultiValueOperand(fixedNestedOperands);

            operandChanged = true;
        }

        return fixedOperand;
    }

    @Override
    public Operand visit(final SingleValueOperand operand) {
        buildTypeNameToKeyMapping();

        String requestTypeName = operand.getStringValue();
        String requestTypeKey = requestTypeNameToKey.get(requestTypeName);

        Operand fixedOperand = operand;
        if (requestTypeKey != null) {
            fixedOperand = new SingleValueOperand(requestTypeKey);

            operandChanged = true;
        }

        return fixedOperand;
    }

    private void buildTypeNameToKeyMapping() {
        if (requestTypeNameToKey != null) {
            return;
        }

        ServiceDeskCompatibilityDispatcher dispatcher = ServiceDeskCompatibilityDispatcher.getInstance();
        requestTypeNameToKey = dispatcher.isServiceDeskInstalled()
                ? dispatcher.getHelper().map(h -> h.buildRequestTypeNameToKeyMap(project))
                    .orElse(Collections.emptyMap())
                : Collections.emptyMap();
    }
}
