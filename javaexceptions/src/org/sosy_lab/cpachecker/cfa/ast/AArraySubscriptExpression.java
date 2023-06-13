// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class AArraySubscriptExpression extends AbstractLeftHandSide {

  private static final long serialVersionUID = 8359800949073538182L;
  private final AExpression arrayExpression;
  private final AExpression subscriptExpression;

  protected AArraySubscriptExpression(
      FileLocation pFileLocation,
      Type pType,
      final AExpression pArrayExpression,
      final AExpression pSubscriptExpression) {
    super(pFileLocation, pType);
    arrayExpression = pArrayExpression;
    subscriptExpression = pSubscriptExpression;
  }

  public AExpression getArrayExpression() {
    return arrayExpression;
  }

  public AExpression getSubscriptExpression() {
    return subscriptExpression;
  }

  @Override
  public String toASTString(boolean pQualified) {
    String left =
        (arrayExpression instanceof AArraySubscriptExpression)
            ? arrayExpression.toASTString(pQualified)
            : arrayExpression.toParenthesizedASTString(pQualified);
    return left + "[" + subscriptExpression.toASTString(pQualified) + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(arrayExpression);
    result = prime * result + Objects.hashCode(subscriptExpression);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AArraySubscriptExpression) || !super.equals(obj)) {
      return false;
    }

    AArraySubscriptExpression other = (AArraySubscriptExpression) obj;

    return Objects.equals(other.arrayExpression, arrayExpression)
        && Objects.equals(other.subscriptExpression, subscriptExpression);
  }
}
