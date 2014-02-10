/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents an expression unique to the java cfa.
 * It evauates to true, if the run time type of the expression is the same
 * as the type Definition. Otherwise, it evaluates to false.
 *
 */
public class JRunTimeTypeEqualsType extends AExpression implements JExpression {

  private final JRunTimeTypeExpression runTimeTypeExpression;
  private final JClassOrInterfaceType typeDef;

  public JRunTimeTypeEqualsType(FileLocation pFileLocation,
      JRunTimeTypeExpression pRunTimeTypeExpression, JClassOrInterfaceType pTypeDef) {
    super(pFileLocation, new JSimpleType(JBasicType.BOOLEAN));

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
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    StringBuilder astString = new StringBuilder("(");
    astString.append(getRunTimeTypeExpression().toASTString());
    astString.append("_equals(");
    astString.append(getTypeDef().getName());
    astString.append("))");
    return astString.toString();
  }

  public JClassOrInterfaceType getTypeDef() {
    return typeDef;
  }

  public JRunTimeTypeExpression getRunTimeTypeExpression() {
    return runTimeTypeExpression;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(runTimeTypeExpression);
    result = prime * result + Objects.hashCode(typeDef);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JRunTimeTypeEqualsType)
        || !super.equals(obj)) {
      return false;
    }

    JRunTimeTypeEqualsType other = (JRunTimeTypeEqualsType) obj;

    return Objects.equals(other.runTimeTypeExpression, runTimeTypeExpression)
            && Objects.equals(other.typeDef, typeDef);
  }

}
