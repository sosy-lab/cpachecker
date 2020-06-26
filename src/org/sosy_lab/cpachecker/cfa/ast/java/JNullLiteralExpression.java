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
import org.sosy_lab.cpachecker.cfa.types.java.JNullType;

/**
 * This class represents the null literal AST node type.
 */
public final class JNullLiteralExpression extends ALiteralExpression implements JLiteralExpression {

  private static final long serialVersionUID = 6233269754214609854L;

  public JNullLiteralExpression(FileLocation pFileLocation) {
    super(pFileLocation, new JNullType());
  }

  @Override
  public JNullType getExpressionType() {
    return (JNullType) super.getExpressionType();
  }

  @Override
  public String toASTString() {
    return "null";
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String getValue() {
    return "null";
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

    if (!(obj instanceof JNullLiteralExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}