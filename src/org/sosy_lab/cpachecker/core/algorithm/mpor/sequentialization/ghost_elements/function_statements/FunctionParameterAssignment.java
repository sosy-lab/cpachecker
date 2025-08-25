// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class FunctionParameterAssignment {

  private final ThreadEdge callContext;

  private final CLeftHandSide leftHandSide;

  private final CExpression rightHandSide;

  public FunctionParameterAssignment(
      ThreadEdge pCallContext, CLeftHandSide pLeftHandSide, CExpression pRightHandSide) {

    callContext = pCallContext;
    leftHandSide = pLeftHandSide;
    rightHandSide = pRightHandSide;
  }

  public boolean isPointer() {
    return leftHandSide.getExpressionType() instanceof CPointerType;
  }

  public CExpressionAssignmentStatement toExpressionAssignmentStatement() {
    return SeqStatementBuilder.buildExpressionAssignmentStatement(leftHandSide, rightHandSide);
  }

  public ThreadEdge getCallContext() {
    return callContext;
  }
}
