// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

public final class JVariableRunTimeType extends AbstractExpression
    implements JRunTimeTypeExpression {

  @Serial private static final long serialVersionUID = 1949325105551973491L;
  private final JIdExpression referencedVariable;

  public JVariableRunTimeType(FileLocation pFileLocation, JIdExpression pReferencedVariable) {
    super(pFileLocation, pReferencedVariable.getExpressionType());

    assert pReferencedVariable.getExpressionType() instanceof JClassOrInterfaceType
        || pReferencedVariable.getExpressionType() instanceof JArrayType;

    referencedVariable = pReferencedVariable;
    assert getReferencedVariable() != null;
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
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return switch (pAAstNodeRepresentation) {
      case QUALIFIED, ORIGINAL_NAMES ->
          getReferencedVariable().toASTString(pAAstNodeRepresentation) + "_getClass()";
      case DEFAULT -> getReferencedVariable().getName() + "_getClass()";
    };
  }

  public JIdExpression getReferencedVariable() {
    return referencedVariable;
  }

  @Override
  public boolean isThisReference() {
    return false;
  }

  @Override
  public boolean isVariableReference() {
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(referencedVariable);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof JVariableRunTimeType other
        && super.equals(obj)
        && Objects.equals(other.referencedVariable, referencedVariable);
  }
}
