/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.types.Type;


public abstract class AArraySubscriptExpression extends AExpression {


  private final IAExpression arrayExpression;
  private final IAExpression subscriptExpression;

  public AArraySubscriptExpression(FileLocation pFileLocation,
      Type pType,
      final IAExpression pArrayExpression,
      final IAExpression pSubscriptExpression) {
    super(pFileLocation, pType);
    arrayExpression = pArrayExpression;
    subscriptExpression = pSubscriptExpression;

  }

  @Override
  public Type getExpressionType() {
    return  super.getExpressionType();
  }

  public IAExpression getArrayExpression() {
    return arrayExpression;
  }

  public IAExpression getSubscriptExpression() {
    return subscriptExpression;
  }

  @Override
  public String toASTString() {
    String left = (arrayExpression instanceof AArraySubscriptExpression) ? arrayExpression.toASTString() : arrayExpression.toParenthesizedASTString();
    return left + "[" + subscriptExpression.toASTString() + "]";
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((arrayExpression == null) ? 0 : arrayExpression.hashCode());
    result = prime * result + ((subscriptExpression == null) ? 0 : subscriptExpression.hashCode());
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (!super.equals(obj)) { return false; }
    if (!(obj instanceof AArraySubscriptExpression)) { return false; }
    AArraySubscriptExpression other = (AArraySubscriptExpression) obj;
    if (arrayExpression == null) {
      if (other.arrayExpression != null) { return false; }
    } else if (!arrayExpression.equals(other.arrayExpression)) { return false; }
    if (subscriptExpression == null) {
      if (other.subscriptExpression != null) { return false; }
    } else if (!subscriptExpression.equals(other.subscriptExpression)) { return false; }

    return super.equals(other);
  }

}