// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;

public abstract class AFunctionCallAssignmentStatement extends AbstractStatement
    implements AAssignment, AFunctionCall {

  private static final long serialVersionUID = 715848925972223832L;
  private final ALeftHandSide leftHandSide;
  private final AFunctionCallExpression rightHandSide;

  protected AFunctionCallAssignmentStatement(
      FileLocation pFileLocation,
      ALeftHandSide pLeftHandSide,
      AFunctionCallExpression pRightHandSide) {
    super(pFileLocation);

    leftHandSide = pLeftHandSide;
    rightHandSide = pRightHandSide;
  }

  @Override
  public ALeftHandSide getLeftHandSide() {
    return leftHandSide;
  }

  @Override
  public AFunctionCallExpression getRightHandSide() {
    return rightHandSide;
  }

  @Override
  public AFunctionCallExpression getFunctionCallExpression() {
    return rightHandSide;
  }

  @Override
  public String toASTString(boolean pQualified) {
    return leftHandSide.toASTString(pQualified)
        + " = "
        + rightHandSide.toASTString(pQualified)
        + ";";
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
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

    if (!(obj instanceof AFunctionCallAssignmentStatement) || !super.equals(obj)) {
      return false;
    }

    AFunctionCallAssignmentStatement other = (AFunctionCallAssignmentStatement) obj;

    return Objects.equals(other.leftHandSide, leftHandSide)
        && Objects.equals(other.rightHandSide, rightHandSide);
  }
}
