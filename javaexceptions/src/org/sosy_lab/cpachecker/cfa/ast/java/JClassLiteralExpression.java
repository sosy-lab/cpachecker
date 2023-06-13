// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 *
 *
 * <pre>
 * ClassLiteral:
 *   TypeName {[ ]} . class
 *   NumericType {[ ]} . class
 *   boolean {[ ]} . class
 *   void . class
 * </pre>
 */
public final class JClassLiteralExpression extends ALiteralExpression
    implements JLiteralExpression {

  private static final long serialVersionUID = -5629884765912549873L;

  private final JType type;

  public JClassLiteralExpression(FileLocation pFileLocation, JType pJType) {
    // FIXME: Passing pJType as expression type is wrong, it needs to be Class<pJType>
    super(pFileLocation, pJType);
    checkArgument(
        pJType instanceof JClassOrInterfaceType
            || pJType instanceof JArrayType
            || pJType instanceof JSimpleType,
        "Type of class literals can only be class, interface, array, or primitive type, "
            + "or the pseudo-type void");
    type = pJType;
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public JType getValue() {
    return type;
  }

  @Override
  public String toASTString() {
    return getValue() + ".class";
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
