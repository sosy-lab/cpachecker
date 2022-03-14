// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;

/**
 * Array creation expression AST node type.
 *
 * <pre>{@code
 * ArrayCreation:
 *   new PrimitiveType [ Expression ] { [ Expression ] } { [ ] }
 *   new TypeName [ < Type { , Type } > ] [ Expression ] { [ Expression ] } { [ ] }
 *   new PrimitiveType [ ] { [ ] } ArrayInitializer
 *   new TypeName [ < Type { , Type } > ] [ ] { [ ] } ArrayInitializer
 * }</pre>
 *
 * The mapping from Java language syntax to AST nodes is as follows:
 *
 * <p>the type node is the array type of the creation expression. It contains information like the
 * dimension and the element type. The length contains the expression, which determines the length
 * of the array. There is an expression in the list for each array dimension from left to right.
 */
public final class JArrayCreationExpression extends AbstractExpression implements JExpression {

  private static final long serialVersionUID = 8794036217601570272L;
  private final ImmutableList<JExpression> length;
  private final @Nullable JArrayInitializer initializer;
  // TODO Type Variables < Type { , Type } >

  public JArrayCreationExpression(
      FileLocation pFileLocation,
      JArrayType pType,
      @Nullable JArrayInitializer pInitializer,
      List<JExpression> pLength) {
    super(pFileLocation, pType);
    length = ImmutableList.copyOf(pLength);
    initializer = pInitializer;
  }

  @Override
  public JArrayType getExpressionType() {
    return (JArrayType) super.getExpressionType();
  }

  @Override
  public String toASTString(boolean pQualified) {
    if (initializer != null) {
      return initializer.toASTString();
    } else {

      StringBuilder astString =
          new StringBuilder("new " + getExpressionType().getElementType().toASTString(""));

      for (JExpression exp : length) {
        astString.append("[");
        astString.append(exp.toASTString(pQualified));
        astString.append("]");
      }

      return astString.toString();
    }
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public ImmutableList<JExpression> getLength() {
    return length;
  }

  public @Nullable JArrayInitializer getInitializer() {
    return initializer;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(initializer);
    result = prime * result + Objects.hashCode(length);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JArrayCreationExpression) || !super.equals(obj)) {
      return false;
    }

    JArrayCreationExpression other = (JArrayCreationExpression) obj;

    return Objects.equals(other.initializer, initializer) && Objects.equals(other.length, length);
  }
}
