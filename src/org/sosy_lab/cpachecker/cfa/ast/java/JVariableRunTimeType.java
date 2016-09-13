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

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import java.util.Objects;


public final class JVariableRunTimeType extends AbstractExpression
    implements JRunTimeTypeExpression {

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
    return (JType)super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    return getReferencedVariable().getName() + "_getClass()";
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

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(referencedVariable);
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

    if (!(obj instanceof JVariableRunTimeType)
        || !super.equals(obj)) {
      return false;
    }

    JVariableRunTimeType other = (JVariableRunTimeType) obj;

    return Objects.equals(other.referencedVariable, referencedVariable);
  }

}
