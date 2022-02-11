// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;

/**
 *  This class represents a Array initializer AST node type.
 *
 * ArrayInitializer:
 *   { [ Expression { , Expression} [ , ]] }
 *
 * The List of initializerExpressions gives the expression
 * the array cell is initialized with from left to right.
 *
 */
public final class JArrayInitializer extends AbstractExpression
    implements JAstNode, JInitializer, JExpression {

  private static final long serialVersionUID = -9034136529891743726L;
  private final List<JExpression> initializerExpressions;

  public JArrayInitializer(FileLocation pFileLocation, List<JExpression> pInitializerExpression, JArrayType pType) {
    super(pFileLocation, pType);

    initializerExpressions = pInitializerExpression;
  }

  @Override
  public JArrayType getExpressionType() {
    return (JArrayType) super.getExpressionType();
  }

  public List<JExpression> getInitializerExpressions() {
    return initializerExpressions;
  }

  @Override
  public String toASTString(boolean pQualified) {

    StringBuilder astString = new StringBuilder("{");

    for (JExpression exp : initializerExpressions) {
      astString.append(exp.toASTString(pQualified) + ", ");
    }

    if (!initializerExpressions.isEmpty()) {
      // delete ', ' at the end of the current string
      int stringLength = astString.length();

      astString.delete(stringLength - 2, stringLength);
    }

    astString.append("}");

    return astString.toString();
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(initializerExpressions);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JArrayInitializer)
        || super.equals(obj)) {
      return false;
    }

    JArrayInitializer other = (JArrayInitializer) obj;

    return Objects.equals(other.initializerExpressions, initializerExpressions);
  }

}
