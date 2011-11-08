/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class IASTFunctionTypeSpecifier extends IType {

  private final IType returnType;
  private String name = null;
  private final List<IASTParameterDeclaration> parameters;
  private final boolean takesVarArgs;

  public IASTFunctionTypeSpecifier(
      boolean pConst,
      boolean pVolatile,
      IType pReturnType,
      List<IASTParameterDeclaration> pParameters,
      boolean pTakesVarArgs) {
    super(pConst, pVolatile);
    returnType = pReturnType;
    parameters = ImmutableList.copyOf(pParameters);
    takesVarArgs = pTakesVarArgs;
  }

  public IType getReturnType() {
    return returnType;
  }

  public String getName() {
    return name;
  }

  public void setName(String pName) {
    checkState(name == null);
    name = pName;
  }

  public List<IASTParameterDeclaration> getParameters() {
    return parameters;
  }

  public boolean takesVarArgs() {
    return takesVarArgs;
  }

  @Override
  public String toASTString() {
    return toASTStringHelper(false);
  }

  String toASTStringFunctionPointer() {
    return toASTStringHelper(true);
  }

  private String toASTStringHelper(boolean pPointer) {
    StringBuilder lASTString = new StringBuilder();

    if (isConst()) {
      lASTString.append("const ");
    }
    if (isVolatile()) {
      lASTString.append("volatile ");
    }

    lASTString.append(returnType.toASTString());

    if (name != null) {
      if (pPointer) {
        lASTString.append("(*");
        lASTString.append(name);
        lASTString.append(")");
      } else {
        lASTString.append(name);
      }
    }

    lASTString.append("(");
    if (parameters.isEmpty()) {
      if (!pPointer) {
        lASTString.append("void");
      }
    } else {
      lASTString.append(Joiner.on(", ").join(new ASTStringIterable(parameters)));
    }
    lASTString.append(") ");

    return lASTString.toString();
  }
}
