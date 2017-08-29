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
package org.sosy_lab.cpachecker.cfa.types;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;


public class AFunctionType implements IAFunctionType {

  private static final long serialVersionUID = 5378375954515193938L;
  private final Type returnType;
  private final List<? extends Type> parameters;
  private final boolean takesVarArgs;

  public AFunctionType(
      Type pReturnType,
      List<? extends Type> pParameters,
      boolean pTakesVarArgs) {

    returnType = checkNotNull(pReturnType);
    parameters = ImmutableList.copyOf(pParameters);
    takesVarArgs = pTakesVarArgs;
  }

  @Override
  public Type getReturnType() {
    return returnType;
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
      final int prime = 31;
      int result = 7;
      result = prime * result + Objects.hashCode(parameters);
      result = prime * result + Objects.hashCode(returnType);
      return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AFunctionType)) {
      return false;
    }

    AFunctionType other = (AFunctionType) obj;

    // We don't compare takesVarArgs here,
    // because it's not really relevant for type equality.
    return Objects.equals(parameters, other.parameters)
           && Objects.equals(returnType, other.returnType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Return type: [");
    int parameterCounter = 0;

    sb.append(returnType.toString());
    sb.append ("], ");

    sb.append("Parameters: " + parameters.size() + ", ");

    if (!parameters.isEmpty()) {
      for (Type currType : parameters) {
        parameterCounter++;
        sb.append("Parameter " + parameterCounter + " type: [" + currType + "], ");
      }
    }

    sb.append("VarArgs: " + takesVarArgs);

    return sb.toString();
  }
}