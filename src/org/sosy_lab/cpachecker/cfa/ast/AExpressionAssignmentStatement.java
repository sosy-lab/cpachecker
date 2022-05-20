// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

public abstract class AExpressionAssignmentStatement extends AbstractStatement
    implements AAssignment {

  private static final long serialVersionUID = -6099960243945488221L;
  private final ALeftHandSide leftHandSide;
  private final AExpression rightHandSide;

  protected AExpressionAssignmentStatement(
      FileLocation pFileLocation, ALeftHandSide pLeftHandSide, AExpression pRightHandSide) {
    super(pFileLocation);
    leftHandSide = checkNotNull(pLeftHandSide);
    rightHandSide = checkNotNull(pRightHandSide);
  }

  @Override
  public String toASTString(boolean pQualified) {
    return leftHandSide.toASTString(pQualified)
        + " = "
        + rightHandSide.toASTString(pQualified)
        + ";";
  }

  @Override
  public ALeftHandSide getLeftHandSide() {
    return leftHandSide;
  }

  @Override
  public AExpression getRightHandSide() {
    return rightHandSide;
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(leftHandSide);
    result = prime * result + Objects.hashCode(rightHandSide);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AExpressionAssignmentStatement) || !super.equals(obj)) {
      return false;
    }

    AExpressionAssignmentStatement other = (AExpressionAssignmentStatement) obj;

    return Objects.equals(other.leftHandSide, leftHandSide)
        && Objects.equals(other.rightHandSide, rightHandSide);
  }
}
