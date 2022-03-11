// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/** AST node for the expression "a = b". */
public class CExpressionAssignmentStatement extends AExpressionAssignmentStatement
    implements CAssignment, CStatement {

  private static final long serialVersionUID = -5024636179305930137L;

  public CExpressionAssignmentStatement(
      FileLocation pFileLocation, CLeftHandSide pLeftHandSide, CExpression pRightHandSide) {
    super(pFileLocation, pLeftHandSide, pRightHandSide);
  }

  @Override
  public CLeftHandSide getLeftHandSide() {
    return (CLeftHandSide) super.getLeftHandSide();
  }

  @Override
  public CExpression getRightHandSide() {
    return (CExpression) super.getRightHandSide();
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

    if (!(obj instanceof CExpressionAssignmentStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
