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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public class FunctionParameterAssignment {

  private final CFAEdgeForThread callContext;

  private final CLeftHandSide leftHandSide;

  private final CExpression rightHandSide;

  public FunctionParameterAssignment(
      CFAEdgeForThread pCallContext, CLeftHandSide pLeftHandSide, CExpression pRightHandSide) {

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

  public CParameterDeclaration getLeftHandSideParameterDeclaration() {
    assert leftHandSide instanceof CIdExpression : "leftHandSide must be CIdExpression";
    CSimpleDeclaration simpleDeclaration = ((CIdExpression) leftHandSide).getDeclaration();
    assert simpleDeclaration instanceof CParameterDeclaration
        : "leftHandSide declaration must be CParameterDeclaration";
    return (CParameterDeclaration) simpleDeclaration;
  }

  // getters

  public CLeftHandSide getLeftHandSide() {
    return leftHandSide;
  }

  public CExpression getRightHandSide() {
    return rightHandSide;
  }

  public CFAEdgeForThread getCallContext() {
    return callContext;
  }
}
