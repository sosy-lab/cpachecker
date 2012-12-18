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
import static com.google.common.collect.Iterables.transform;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class CFunctionPointerType implements CType {


  private boolean   isConst;
  private boolean   isVolatile;
  private final CType returnType;
  private String name = null;
  private final List<CType> parameters;
  private final boolean takesVarArgs;

  public CFunctionPointerType(
      boolean pConst,
      boolean pVolatile,
      CType pReturnType,
      List<CType> pParameters,
      boolean pTakesVarArgs) {


    isConst = pConst;
    isVolatile = pVolatile;

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

  public List<CType> getParameters() {
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
    Joiner.on(", ").appendTo(lASTString, transform(parameters, new Function<CType, String>() {
                                                      @Override
                                                      public String apply(CType pInput) {
                                                        return pInput.toASTString("");
                                                      }
                                                    }));
    if (takesVarArgs) {
      if (!parameters.isEmpty()) {
        lASTString.append(", ");
      }
      lASTString.append("...");
    }
    lASTString.append(")");

    return lASTString.toString();
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }
}
