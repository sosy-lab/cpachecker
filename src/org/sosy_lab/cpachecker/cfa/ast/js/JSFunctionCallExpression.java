/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.js;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;

public class JSFunctionCallExpression extends AFunctionCallExpression implements JSRightHandSide {

  private static final long serialVersionUID = 9202497344519251662L;

  public JSFunctionCallExpression(
      final FileLocation pFileLocation,
      final JSExpression pFunctionName,
      final List<JSExpression> pParameters,
      final JSFunctionDeclaration pDeclaration) {

    super(pFileLocation, JSAnyType.ANY, pFunctionName, pParameters, pDeclaration);
  }

  @Override
  public JSType getExpressionType() {
    return (JSType) super.getExpressionType();
  }

  @Override
  public JSExpression getFunctionNameExpression() {
    return (JSExpression) super.getFunctionNameExpression();
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<JSExpression> getParameterExpressions() {
    return (List<JSExpression>) super.getParameterExpressions();
  }

  /**
   * Get the declaration of the function. A function may have several declarations in a C file
   * (several forward declarations without a body, and one with it). In this case, it is not defined
   * which declaration is returned.
   *
   * <p>The result may be null if the function was not declared, or if a complex function name
   * expression is used (i.e., a function pointer).
   */
  @Override
  public JSFunctionDeclaration getDeclaration() {

    return (JSFunctionDeclaration) super.getDeclaration();
  }

  @Override
  public <R, X extends Exception> R accept(JSRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JSAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
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

    if (!(obj instanceof JSFunctionCallExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
