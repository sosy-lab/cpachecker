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

import java.util.List;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;

/**
 *  Array creation expression AST node type.
 *
 * ArrayCreation:
 *   new PrimitiveType [ Expression ] { [ Expression ] } { [ ] }
 *   new TypeName [ < Type { , Type } > ]
 *       [ Expression ] { [ Expression ] } { [ ] }
 *   new PrimitiveType [ ] { [ ] } ArrayInitializer
 *   new TypeName [ < Type { , Type } > ]
 *       [ ] { [ ] } ArrayInitializer
 *
 *
 *   The mapping from Java language syntax to AST nodes is as follows:
 *
 *   the type node is the array type of the creation expression. It contains information
 *   like the dimension and the element type.
 *   The length contains the expression, which determines the length of the array.
 *   There is an expression in the list for each array dimension from left to right.
 *
 */
public class JArrayCreationExpression extends AExpression implements JExpression {

  private final List<JExpression> length;
  private final JArrayInitializer initializer;
  //TODO Type Variables < Type { , Type } >

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
    int result = 7;
    result = prime * result + Objects.hashCode(initializer);
    result = prime * result + Objects.hashCode(length);
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

    if (!(obj instanceof JArrayCreationExpression)
        || !super.equals(obj)) {
      return false;
    }

    JArrayCreationExpression other = (JArrayCreationExpression) obj;

    return Objects.equals(other.initializer, initializer)
            && Objects.equals(other.length, length);
  }

}
