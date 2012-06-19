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
package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CAstStringIterable;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class CFunctionType extends CType {

  private final CType returnType;
  private String name = null;
  private final List<CParameterDeclaration> parameters;
  private final boolean takesVarArgs;

  public CFunctionType(
      boolean pConst,
      boolean pVolatile,
      CType pReturnType,
      List<CParameterDeclaration> pParameters,
      boolean pTakesVarArgs) {
    super(pConst, pVolatile);
    returnType = pReturnType;
    parameters = ImmutableList.copyOf(pParameters);
    takesVarArgs = pTakesVarArgs;
  }

  public CType getReturnType() {
    return returnType;
  }

  public String getName() {
    return name;
  }

  public void setName(String pName) {
    checkState(name == null);
    name = pName;
  }

  public List<CParameterDeclaration> getParameters() {
    return parameters;
  }

  public boolean takesVarArgs() {
    return takesVarArgs;
  }

  @Override
  public String toASTString(String pDeclarator) {
    StringBuilder lASTString = new StringBuilder();

    if (isConst()) {
      lASTString.append("const ");
    }
    if (isVolatile()) {
      lASTString.append("volatile ");
    }

    lASTString.append(returnType.toASTString(""));
    lASTString.append(" ");

    if (pDeclarator.startsWith("*")) {
      // this is a function pointer, insert parentheses
      lASTString.append("(");
      lASTString.append(pDeclarator);
      lASTString.append(")");
    } else {
      lASTString.append(pDeclarator);
    }

    lASTString.append("(");
    Joiner.on(", ").appendTo(lASTString, new CAstStringIterable(parameters));
    if (takesVarArgs) {
      if (!parameters.isEmpty()) {
        lASTString.append(", ");
      }
      lASTString.append("...");
    }
    lASTString.append(")");

    return lASTString.toString();
  }
}
