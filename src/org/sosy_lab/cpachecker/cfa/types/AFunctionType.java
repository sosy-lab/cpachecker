/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.types;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;


public class AFunctionType implements IAFunctionType {

  private final Type returnType;
  private String name = null;
  private final List<? extends Type> parameters;
  private final boolean takesVarArgs;

  public AFunctionType(
      Type pReturnType,
      List<? extends Type> pParameters,
      boolean pTakesVarArgs) {

    returnType = pReturnType;
    parameters = ImmutableList.copyOf(pParameters);
    takesVarArgs = pTakesVarArgs;
  }

  @Override
  public Type getReturnType() {
    return returnType;
  }

  @Override
  public String getName() {
    return name;
  }

  protected void setName(String pName) {
    checkState(getName() == null);
    name = pName;
  }

  @Override
  public List<? extends Type> getParameters() {
    return parameters;
  }

  @Override
  public boolean takesVarArgs() {
    return takesVarArgs;
  }

  @Override
  public String toASTString(String pDeclarator) {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(returnType.toASTString(""));
    lASTString.append(" ");

    lASTString.append(pDeclarator);


    lASTString.append("(");
    Joiner.on(", ").appendTo(lASTString, parameters);
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
  public int hashCode() {
    throw new UnsupportedOperationException("Do not use hashCode of CTypes");
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if(!(obj instanceof AFunctionType)) {
      return false;
    }

    AFunctionType other = (AFunctionType) obj;

    return Objects.equals(name, other.name) && Objects.equals(parameters, other.parameters)
           && Objects.equals(returnType, other.returnType) && Objects.equals(takesVarArgs, other.takesVarArgs);
  }

}