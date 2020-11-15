// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;


import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents the boolean literal AST node type.
 *
 * BooleanLiteral:
 *   true
 *   false
 *
 */
public final class JBooleanLiteralExpression extends ALiteralExpression
    implements JLiteralExpression {

  private static final long serialVersionUID = 1623276041882984116L;
  private final boolean value;

  public JBooleanLiteralExpression(FileLocation pFileLocation,  boolean pValue) {
    super(pFileLocation, JSimpleType.getBoolean());
    value = pValue;
  }

  public boolean getBoolean() {
    return value;
  }

  @Override
  @Deprecated // call getBoolean()
  public Boolean getValue() {
    return value;
  }

  @Override
  public String toASTString() {
    if (value) {
      return "true";
    } else {
      return"false";
    }
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Boolean.hashCode(value);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JBooleanLiteralExpression)
        || !super.equals(obj)) {
      return false;
    }

    JBooleanLiteralExpression other = (JBooleanLiteralExpression) obj;

    return other.value == value;
  }

}
