// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

public class ThreadFunctionReturnValue {

  private final CFAEdge threadFunction;
  private final boolean success;

  ThreadFunctionReturnValue(CFAEdge pThreadFunction, boolean pSuccess) {
    threadFunction = pThreadFunction;
    success = pSuccess;
  }

  public CFAEdge getThreadFunction() {
    return threadFunction;
  }

  public boolean isSuccess() {
    return success;
  }

  public List<? extends AExpression> asAssumptions() {
    List<AExpression> expressions = new ArrayList<>();
    switch (threadFunction.getEdgeType()) {
      case FunctionCallEdge:
        AFunctionCall functionCall = ((FunctionCallEdge) threadFunction).getFunctionCall();
        if (functionCall instanceof AFunctionCallAssignmentStatement) {
          expressions.add(((AFunctionCallAssignmentStatement) functionCall).getLeftHandSide());
        }
        break;
      case StatementEdge:
        AStatement statement = ((AStatementEdge) threadFunction).getStatement();
        if (statement instanceof AFunctionCallAssignmentStatement) {
          expressions.add(((AFunctionCallAssignmentStatement) statement).getLeftHandSide());
        }
        break;
      default:
        throw new AssertionError("Unhandled edge type");
    }
    assert !expressions.isEmpty();

    ImmutableList.Builder<AExpression> builder = ImmutableList.builder();
    for (AExpression expression : expressions) {
      if (expression instanceof CExpression) {
        CExpression cExpression = (CExpression) expression;
        CBinaryExpression assumption =
            new CBinaryExpression(
                cExpression.getFileLocation(),
                CNumericTypes.BOOL,
                cExpression.getExpressionType(),
                cExpression,
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
                success ? BinaryOperator.EQUALS : BinaryOperator.NOT_EQUALS);
        builder.add(assumption);
      } else {
        throw new AssertionError("Unhandled expression type");
      }
    }
    return builder.build();
  }
}
