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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;

/** This is the declaration of a function parameter. It contains a type and a name. */
public final class JSParameterDeclaration extends AParameterDeclaration
    implements JSSimpleDeclaration {

  private static final long serialVersionUID = 4718050921269110062L;
  private String qualifiedName;

  public JSParameterDeclaration(FileLocation pFileLocation, String pName) {
    super(pFileLocation, JSAnyType.ANY, checkNotNull(pName));
  }

  /**
   * Is set in {@link
   * org.sosy_lab.cpachecker.cfa.parser.eclipse.js.FunctionScopeImpl#setQualifiedNameOfParameters()}.
   *
   * @param pQualifiedName The qualified name of the parameter, which depends on the qualified name
   *     of the function scope.
   */
  public void setQualifiedName(String pQualifiedName) {
    checkState(qualifiedName == null);
    qualifiedName = checkNotNull(pQualifiedName);
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public JSType getType() {
    return (JSType) super.getType();
  }

  @Override
  public <R, X extends Exception> R accept(JSSimpleDeclarationVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JSAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }
}
