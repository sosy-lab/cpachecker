// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

/**
 * This class represents a reference to an enum constant.
 *
 *
 */
public final class JEnumConstantExpression extends AbstractExpression implements JExpression {

  // TODO Change the representation of the constantName from String to JIdExpression

  private static final long serialVersionUID = 253114542568695975L;
  private final String constantName;

  public JEnumConstantExpression(FileLocation pFileLocation, JClassType pType, String pConstantName) {
    super(pFileLocation, pType);

    constantName = pConstantName;
  }

  @Override
  public JClassType getExpressionType() {
    return (JClassType) super.getExpressionType();
  }

  public String getConstantName() {
    return constantName;
  }

  @Override
  public String toASTString(boolean pQualified) {
    return toASTString();
  }

  @Override
  public String toASTString() {
    return constantName;
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(constantName);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JEnumConstantExpression)
        || !super.equals(obj)) {
      return false;
    }

    JEnumConstantExpression other = (JEnumConstantExpression) obj;

    return Objects.equals(other.constantName, constantName);
  }


}