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

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.js.JSFunctionType;

/**
 * This class represents forward declarations of functions. Example code:
 *
 * <p>int foo(int x);
 */
public class JSFunctionDeclaration extends AFunctionDeclaration implements JSDeclaration {

  private static final long serialVersionUID = -6049361884111627710L;

  public JSFunctionDeclaration(
      FileLocation pFileLocation,
      JSFunctionType pType,
      String pName,
      List<JSParameterDeclaration> parameters) {
    super(pFileLocation, pType, checkNotNull(pName), parameters);
  }

  @Override
  public JSFunctionType getType() {
    return (JSFunctionType) super.getType();
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<JSParameterDeclaration> getParameters() {
    return (List<JSParameterDeclaration>) super.getParameters();
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

    if (!(obj instanceof JSFunctionDeclaration)) {
      return false;
    }

    return super.equals(obj);
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
