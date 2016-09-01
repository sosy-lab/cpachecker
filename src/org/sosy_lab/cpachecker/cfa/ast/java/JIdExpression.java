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

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * Class for expressions that represent names of declared constructs.
 * ( e.g. variables, names of methods in Invocation).
 *
 * If possible, it saves a reference to the declaration this name references.
 *
 */
public class JIdExpression extends AIdExpression implements JLeftHandSide {

  public JIdExpression(FileLocation pFileLocation, JType pType, String pName, JSimpleDeclaration pDeclaration) {
    super(pFileLocation, pType, pName, pDeclaration);
    // TODO Refactor, so we do not need null for declaration.
    // (Insert extra classes or objects for unresolvable declarations)
    //assert pDeclaration != null;
  }

  @Override
  @Nullable
  public JSimpleDeclaration getDeclaration() {
    return (JSimpleDeclaration) super.getDeclaration();
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

    if (!(obj instanceof JIdExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}