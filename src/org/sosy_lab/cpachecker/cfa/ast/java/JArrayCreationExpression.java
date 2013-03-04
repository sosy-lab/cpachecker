/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;


public class JArrayCreationExpression extends AExpression implements JExpression {

  private final List<JExpression> length;
  private final JArrayInitializer initializer;

  public JArrayCreationExpression(FileLocation pFileLocation, JArrayType pType, JArrayInitializer pInitializer, List<JExpression> pLength) {
    super(pFileLocation, pType);
    length = pLength;
    initializer = pInitializer;

  }

  @Override
  public JArrayType getExpressionType() {
    return (JArrayType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    if (initializer != null) {
      return initializer.toASTString();
    } else {

      StringBuilder astString = new StringBuilder("new "+ getExpressionType().getElementType().toASTString(""));

      for (JExpression exp : length) {
        astString.append("[");
        astString.append(exp.toASTString());
        astString.append("]");
      }



      return   astString.toString();
    }
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public List<JExpression> getLength() {
    return length;
  }

  public JArrayInitializer getInitializer() {
      return initializer;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((initializer == null) ? 0 : initializer.hashCode());
    result = prime * result + ((length == null) ? 0 : length.hashCode());
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
    if (!(obj instanceof JArrayCreationExpression)) { return false; }
    JArrayCreationExpression other = (JArrayCreationExpression) obj;
    if (initializer == null) {
      if (other.initializer != null) { return false; }
    } else if (!initializer.equals(other.initializer)) { return false; }
    if (length == null) {
      if (other.length != null) { return false; }
    } else if (!length.equals(other.length)) { return false; }

    return super.equals(other);
  }

}
