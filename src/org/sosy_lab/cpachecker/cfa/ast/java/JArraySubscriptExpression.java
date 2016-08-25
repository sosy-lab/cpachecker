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

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 *  This class represents the array access expression AST node type.
 *
 * ArrayAccess:
 *   Expression [ Expression ]
 *
 *  The array expression gives the identifier of the array.
 *  The subscript Expression gives the index of the arraycell to be read.
 *
 */
public final class JArraySubscriptExpression extends AArraySubscriptExpression
    implements JLeftHandSide {

  public JArraySubscriptExpression(FileLocation pFileLocation, JType pType, JExpression pArrayExpression,
      JExpression pSubscriptExpression) {
    super(pFileLocation, pType, pArrayExpression, pSubscriptExpression);
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public JExpression getArrayExpression() {
    return (JExpression) super.getArrayExpression();
  }

  @Override
  public JExpression getSubscriptExpression() {
    return (JExpression) super.getSubscriptExpression();
  }

  @Override
  public <R, X extends Exception> R accept(JLeftHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
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

    if (!(obj instanceof JArraySubscriptExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
