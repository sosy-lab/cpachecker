// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pendingException;

import java.util.Deque;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpressionIsPendingExceptionThrown;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.PendingExceptionOfJIdExpression;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodCallEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JThrowStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class PendingExceptionTransferRelation
    extends ForwardingTransferRelation<PendingExceptionState, PendingExceptionState, Precision> {

  @Override
  protected PendingExceptionState handleDeclarationEdge(
      JDeclarationEdge cfaEdge, JDeclaration decl) {
    return state; // TODO
  }

  @Override
  protected @Nullable PendingExceptionState handleAssumption(
      JAssumeEdge cfaEdge, JExpression expression, boolean truthAssumption) {

    if (!(expression instanceof JIdExpressionIsPendingExceptionThrown)) {
      return state;
    }

    if (state == null) {
      return state;
    }

    Deque<String> pendingExceptionStack = state.getPendingExceptionStack();

    if (pendingExceptionStack.isEmpty()) {
      return state;
    }

    return state; // TODO
  }

  @Override
  protected PendingExceptionState handleStatementEdge(
      JStatementEdge cfaEdge, JStatement statement) {
    assert state != null;

    if (!(cfaEdge instanceof JThrowStatementEdge)
        || !(statement instanceof JExpressionAssignmentStatement)) {
      return state;
    }

    if ((((JExpressionAssignmentStatement) statement).getLeftHandSide()
        instanceof PendingExceptionOfJIdExpression)) {

      final JExpression rightHandSide =
          ((JExpressionAssignmentStatement) statement).getRightHandSide();

      assert isThrowable(rightHandSide.getExpressionType());
      state.getPendingExceptionStack().push(rightHandSide.toString());
    }

    return state; // TODO Create copy?
  }

  @Override
  protected PendingExceptionState handleFunctionCallEdge(
      JMethodCallEdge cfaEdge,
      List<JExpression> arguments,
      List<JParameterDeclaration> parameters,
      String calledFunctionName) {
    return state; // TODO
  }

  @Override
  protected PendingExceptionState handleReturnStatementEdge(JReturnStatementEdge cfaEdge) {
    return state; // TODO
  }

  @Override
  protected PendingExceptionState handleFunctionReturnEdge(
      JMethodReturnEdge cfaEdge,
      JMethodSummaryEdge fnkCall,
      JMethodOrConstructorInvocation summaryExpr,
      String callerFunctionName) {
    return state; // TODO
  }

  private boolean isThrowable(JType pJType) {

    if (!(pJType instanceof JClassType)) {
      return false;
    }

    JClassType parentClass = ((JClassType) pJType).getParentClass();

    while (parentClass != null && !parentClass.toString().equals("java.lang.Throwable")) {
      parentClass = parentClass.getParentClass();
    }

    if (parentClass != null && parentClass.toString().equals("java.lang.Throwable")) {
      return true;
    }

    return false;
  }
}
