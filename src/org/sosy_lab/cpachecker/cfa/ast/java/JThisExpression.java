// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;

/**
 * This Expression is used, if either the Run Time Type or Run Time Object
 * of the this (Keyword) Reference  is requested.
 * As part of a regular Expression, it denotes the Run Time Object. As Part of a
 * JRunTimeTypeEqualsType Expression, it denotes the Run Time Type.
 */
public final class JThisExpression extends AbstractExpression implements JRunTimeTypeExpression {

  private static final long serialVersionUID = -3327127448924110155L;

  public JThisExpression(FileLocation pFileLocation, JClassOrInterfaceType pType) {
    super(pFileLocation, pType);
  }

  @Override
  public JClassOrInterfaceType getExpressionType() {
    return (JClassOrInterfaceType) super.getExpressionType();
  }

  @Override
  public String toASTString(boolean pQualified) {
    return toASTString();
  }

  @Override
  public String toASTString() {
    return "this";
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public boolean isThisReference() {
    return true;
  }

  @Override
  public boolean isVariableReference() {
    return false;
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

    if (!(obj instanceof JThisExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}