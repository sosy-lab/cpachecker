/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.ast.java;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * public class JClassLiteral { ClassLiteral: TypeName {[ ]} . class NumericType {[ ]} . class
 * boolean {[ ]} . class void . class
 */
public class JClassLiteralExpression extends ALiteralExpression implements JLiteralExpression {

  private JClassLiteralExpression(FileLocation pFileLocation, Type pType) {
    super(pFileLocation, pType);
  }

  public JClassLiteralExpression(FileLocation pFileLocation, JType pJType) {
    this(pFileLocation, (Type) pJType);
    checkArgument(
        pJType instanceof JClassOrInterfaceType
            || pJType instanceof JArrayType
            || (pJType instanceof JSimpleType && !pJType.equals(JSimpleType.getNull())),
        "Type of class literals can only be class, interface, array, or primitive type, "
            + "or the pseudo-type void");
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public Type getValue() {
    return getExpressionType();
  }

  @Override
  public String toASTString() {
    return getExpressionType().toString();
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
