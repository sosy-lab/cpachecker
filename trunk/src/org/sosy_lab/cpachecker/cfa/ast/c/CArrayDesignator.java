// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class CArrayDesignator extends CDesignator {

  private static final long serialVersionUID = 6803448218616765608L;
  private final AExpression subscriptExpression;

  public CArrayDesignator(
      final FileLocation pFileLocation, final CExpression pSubscriptExpression) {
    super(pFileLocation);
    subscriptExpression = pSubscriptExpression;
  }

  public CExpression getSubscriptExpression() {
    return (CExpression) subscriptExpression;
  }

  @Override
  public String toASTString(boolean pQualified) {
    return "[" + getSubscriptExpression().toASTString(pQualified) + "]";
  }

  @Override
  public String toParenthesizedASTString(boolean pQualified) {
    return toASTString(pQualified);
  }

  @Override
  public <R, X extends Exception> R accept(CDesignatorVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(subscriptExpression);
    result = prime * result * super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CArrayDesignator) || !super.equals(obj)) {
      return false;
    }

    CArrayDesignator other = (CArrayDesignator) obj;

    return Objects.equals(other.subscriptExpression, subscriptExpression);
  }
}
