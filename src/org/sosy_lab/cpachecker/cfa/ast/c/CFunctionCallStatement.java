// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class CFunctionCallStatement extends AFunctionCallStatement
    implements CStatement, CFunctionCall {

  private static final long serialVersionUID = 1103049666572120249L;

  public CFunctionCallStatement(FileLocation pFileLocation, CFunctionCallExpression pFunctionCall) {
    super(pFileLocation, pFunctionCall);
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

    if (!(obj instanceof CFunctionCallStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
