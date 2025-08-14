// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class FunctionParameterAssignment {

  private final ThreadEdge callContext;

  private final CParameterDeclaration originalParameterDeclaration;

  private final Optional<CSimpleDeclaration> originalRightHandSideDeclaration;

  private final CLeftHandSide leftHandSide;

  private final CExpression rightHandSide;

  public FunctionParameterAssignment(
      ThreadEdge pCallContext,
      CParameterDeclaration pOriginalParameterDeclaration,
      CExpression pOriginalRightHandSide,
      CLeftHandSide pLeftHandSide,
      CExpression pRightHandSide) {

    callContext = pCallContext;
    originalParameterDeclaration = pOriginalParameterDeclaration;
    originalRightHandSideDeclaration = extractSimpleDeclaration(pOriginalRightHandSide);
    leftHandSide = pLeftHandSide;
    rightHandSide = pRightHandSide;
  }

  private Optional<CSimpleDeclaration> extractSimpleDeclaration(CExpression pRightHandSide) {
    if (pRightHandSide instanceof CIdExpression idExpression) {
      return Optional.of(idExpression.getDeclaration());

    } else if (pRightHandSide instanceof CUnaryExpression unaryExpression) {
      if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
        return Optional.of(idExpression.getDeclaration());
      }
    }
    // can e.g. occur with 'param = 4' i.e. literal integer expressions
    return Optional.empty();
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

  public CParameterDeclaration getOriginalParameterDeclaration() {
    return originalParameterDeclaration;
  }

  public Optional<CSimpleDeclaration> getOriginalRightHandSideDeclaration() {
    return originalRightHandSideDeclaration;
  }

  public CParameterDeclaration getLeftHandSideDeclaration() {
    if (leftHandSide instanceof CIdExpression lhsId) {
      if (lhsId.getDeclaration() instanceof CParameterDeclaration parameterDeclaration) {
        return parameterDeclaration;
      }
    }
    throw new IllegalArgumentException("could not extract CParameterDeclaration from leftHandSide");
  }

  /**
   * Returns {@link CSimpleDeclaration} because the RHS can be either a {@link CVariableDeclaration}
   * or a {@link CParameterDeclaration}.
   */
  public CSimpleDeclaration getRightHandSideDeclaration() {
    // case 'ptr = &var;' where var is non-pointer variable
    if (rightHandSide instanceof CUnaryExpression rhsUnary) {
      if (rhsUnary.getOperand() instanceof CIdExpression rhsId) {
        return rhsId.getDeclaration();
      }
    }
    // case 'ptr = var;' where var is a pointer variable
    if (rightHandSide instanceof CIdExpression rhsId) {
      return rhsId.getDeclaration();
    }
    throw new IllegalArgumentException("could not extract CSimpleDeclaration from rightHandSide");
  }
}
