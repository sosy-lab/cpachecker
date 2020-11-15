// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 *  This class represents the array access expression AST node type.
 *
 * ArrayAccess:
 *   Expression [ Expression ]
 *
 *  The array expression gives the identifier of the array.
 *  The subscript Expression gives the index of the arraycell to be read.
 *
 */
public final class JArraySubscriptExpression extends AArraySubscriptExpression
    implements JLeftHandSide {

  private static final long serialVersionUID = 5326760755937022733L;

  public JArraySubscriptExpression(FileLocation pFileLocation, JType pType, JExpression pArrayExpression,
      JExpression pSubscriptExpression) {
    super(pFileLocation, pType, pArrayExpression, pSubscriptExpression);
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public JExpression getArrayExpression() {
    return (JExpression) super.getArrayExpression();
  }

  @Override
  public JExpression getSubscriptExpression() {
    return (JExpression) super.getSubscriptExpression();
  }

  @Override
  public <R, X extends Exception> R accept(JLeftHandSideVisitor<R, X> v) throws X {
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

    if (!(obj instanceof JArraySubscriptExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
