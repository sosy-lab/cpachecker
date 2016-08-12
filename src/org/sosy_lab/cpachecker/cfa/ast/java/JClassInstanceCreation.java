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

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;

import java.util.List;

import javax.annotation.Nullable;

/**
 *
 * This class represents the class instance creation expression AST node type.
 *
 * ClassInstanceCreation:
 *       [ Expression . ]
 *           new [ < Type { , Type } > ]
 *           Type ( [ Expression { , Expression } ] )
 *           [ AnonymousClassDeclaration ]
 *
 *  The functionname is in most cases a {@link JIdExpression}.
 *
 *  Not all node arragements will represent legal Java constructs.
 *  In particular, it is nonsense if the functionname does not contain a {@link JIdExpression}.
 *
 *
 *
 */
public class JClassInstanceCreation extends JMethodInvocationExpression implements JRightHandSide {

  // TODO refactor to be either abstract or final

  //TODO Type Variables , AnonymousClassDeclaration

  public JClassInstanceCreation(
      FileLocation pFileLocation,
      JClassOrInterfaceType pType,
      JExpression pFunctionName,
      List<? extends JExpression> pParameters,
      JConstructorDeclaration pDeclaration) {

    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
  }

  @Override
  @Nullable
  public JConstructorDeclaration getDeclaration() {
    return (JConstructorDeclaration) super.getDeclaration();
  }

  @Override
  public JClassOrInterfaceType getExpressionType() {
    return (JClassOrInterfaceType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {

    StringBuilder astString = new StringBuilder("new ");
    astString.append(getExpressionType().toASTString(getFunctionNameExpression().toASTString()));

    return astString.toString();
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

    if (!(obj instanceof JClassInstanceCreation)) {
      return false;
    }

    return super.equals(obj);
  }
}