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
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

/**
 * This class represents a reference to an enum constant.
 *
 *
 */
public class JEnumConstantExpression extends AExpression implements JExpression {

  // TODO Change the representation of the constantName from String to JIdExpression

  private final String constantName;

  public JEnumConstantExpression(FileLocation pFileLocation, JClassType pType, String pConstantName) {
    super(pFileLocation, pType);

    constantName = pConstantName;
  }

  @Override
  public JClassType getExpressionType() {
    return (JClassType) super.getExpressionType();
  }

  public String getConstantName() {
    return constantName;
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    return constantName;
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(constantName);
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

    if (!(obj instanceof JEnumConstantExpression)
        || !super.equals(obj)) {
      return false;
    }

    JEnumConstantExpression other = (JEnumConstantExpression) obj;

    return Objects.equals(other.constantName, constantName);
  }


}