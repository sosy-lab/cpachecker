// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;

public class AFunctionType implements IAFunctionType {

  private static final long serialVersionUID = 5378375954515193938L;
  private final Type returnType;
  private final List<? extends Type> parameters;
  private final boolean takesVarArgs;

  public AFunctionType(Type pReturnType, List<? extends Type> pParameters, boolean pTakesVarArgs) {

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
    sb.append("], ");

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
