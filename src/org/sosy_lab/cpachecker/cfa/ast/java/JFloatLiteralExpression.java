// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

/** This class represents the float number literal AST node type. */
public final class JFloatLiteralExpression extends AFloatLiteralExpression
    implements JLiteralExpression {

  @Serial private static final long serialVersionUID = -8344145326316408368L;

  public JFloatLiteralExpression(FileLocation pFileLocation, FloatValue pValue) {
    super(pFileLocation, getJType(pValue), pValue);
  }

  /** Returns the equivalent Java type for a {@link FloatValue} */
  private static JSimpleType getJType(FloatValue pValue) {
    if (pValue.getFormat().equals(FloatValue.Format.Float32)) {
      return JSimpleType.FLOAT;
    } else if (pValue.getFormat().equals(FloatValue.Format.Float64)) {
      return JSimpleType.DOUBLE;
    } else {
      throw new IllegalArgumentException();
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
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof JFloatLiteralExpression && super.equals(obj);
  }
}
