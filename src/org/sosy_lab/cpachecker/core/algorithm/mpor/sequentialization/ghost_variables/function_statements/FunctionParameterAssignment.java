// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;

public class FunctionParameterAssignment {

  public final CLeftHandSide leftHandSide;

  public final CExpression rightHandSide;

  public FunctionParameterAssignment(CLeftHandSide pLeftHandSide, CExpression pRightHandSide) {
    leftHandSide = pLeftHandSide;
    rightHandSide = pRightHandSide;
  }

  public boolean isPointer() {
    return leftHandSide.getExpressionType() instanceof CPointerType;
  }

  public CExpressionAssignmentStatement toExpressionAssignmentStatement() {
    return SeqStatementBuilder.buildExpressionAssignmentStatement(leftHandSide, rightHandSide);
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
    if (rightHandSide instanceof CIdExpression rhsId) {
      return rhsId.getDeclaration();
    }
    throw new IllegalArgumentException("could not extract CSimpleDeclaration from rightHandSide");
  }
}
