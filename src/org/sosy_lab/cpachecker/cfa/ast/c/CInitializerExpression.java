// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class CInitializerExpression extends AInitializerExpression implements CInitializer {

  private static final long serialVersionUID = 2706992437396660354L;

  public CInitializerExpression(final FileLocation pFileLocation, final CExpression pExpression) {
    super(pFileLocation, pExpression);
  }

  @Override
  public CExpression getExpression() {
    return (CExpression) super.getExpression();
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CInitializerVisitor<R, X> pV) throws X {
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

    if (!(obj instanceof CInitializerExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
