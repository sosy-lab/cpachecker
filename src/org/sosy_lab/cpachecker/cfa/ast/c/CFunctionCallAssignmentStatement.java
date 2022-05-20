// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class CFunctionCallAssignmentStatement extends AFunctionCallAssignmentStatement
    implements CStatement, CAssignment, CFunctionCall {

  private static final long serialVersionUID = 8744203402170708743L;

  public CFunctionCallAssignmentStatement(
      FileLocation pFileLocation,
      CLeftHandSide pLeftHandSide,
      CFunctionCallExpression pRightHandSide) {
    super(pFileLocation, pLeftHandSide, pRightHandSide);
  }

  @Override
  public CLeftHandSide getLeftHandSide() {
    return (CLeftHandSide) super.getLeftHandSide();
  }

  @Override
  public CFunctionCallExpression getRightHandSide() {
    return (CFunctionCallExpression) super.getRightHandSide();
  }

  @Override
  public CFunctionCallExpression getFunctionCallExpression() {
    return (CFunctionCallExpression) super.getFunctionCallExpression();
  }

  @Override
  public <R, X extends Exception> R accept(CStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public String toASTString(boolean pQualified) {
    return getLeftHandSide().toASTString(pQualified)
        + " = "
        + getRightHandSide().toASTString(pQualified)
        + ";";
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CFunctionCallAssignmentStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
