// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JReferenceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

public final class JInstanceOfType extends AbstractExpression implements JExpression {
  private static final long serialVersionUID = -1313620435920744071L;
  private final JRunTimeTypeExpression runTimeTypeExpression;
  private final JReferenceType typeDef;

  public JInstanceOfType(
      FileLocation pFileLocation,
      JRunTimeTypeExpression pRunTimeTypeExpression,
      JReferenceType pTypeDef) {
    super(pFileLocation, JSimpleType.getBoolean());

    runTimeTypeExpression = pRunTimeTypeExpression;
    typeDef = pTypeDef;

    assert getRunTimeTypeExpression() != null;
    assert getTypeDef() != null;
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
  public String toASTString(boolean pQualified) {
    return toASTString();
  }

  @Override
  public String toASTString() {
    String name;
    if (getTypeDef() instanceof JClassOrInterfaceType) {
      name = ((JClassOrInterfaceType) getTypeDef()).getName();
    } else {
      name = ((JArrayType) getTypeDef()).getElementType().toASTString("");
    }
    StringBuilder astString = new StringBuilder("(");
    astString.append(getRunTimeTypeExpression().toASTString());
    astString.append("_equals(");
    astString.append(name); // FIXME _class missing? I.e. var_getClass()_equals(typeDef_class)?
    astString.append("))");
    return astString.toString();
  }

  public JReferenceType getTypeDef() {
    return typeDef;
  }

  public JRunTimeTypeExpression getRunTimeTypeExpression() {
    return runTimeTypeExpression;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 5;
    result = prime * result + Objects.hashCode(runTimeTypeExpression);
    result = prime * result + Objects.hashCode(typeDef);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof JInstanceOfType other
        && super.equals(obj)
        && Objects.equals(other.runTimeTypeExpression, runTimeTypeExpression)
        && Objects.equals(other.typeDef, typeDef);
  }
}
