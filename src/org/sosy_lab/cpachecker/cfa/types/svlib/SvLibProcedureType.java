// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.svlib;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;

public final class SvLibProcedureType implements SvLibType, AFunctionType {

  @Serial private static final long serialVersionUID = 5728816904538462642L;
  private final ImmutableList<SvLibType> inputTypes;
  private final ImmutableList<SvLibType> localVariableTypes;
  private final ImmutableList<SvLibType> outputTypes;

  public SvLibProcedureType(
      List<SvLibType> pInputTypes,
      List<SvLibType> pLocalVariableTypes,
      List<SvLibType> pOutputTypes) {
    inputTypes = ImmutableList.copyOf(pInputTypes);
    localVariableTypes = ImmutableList.copyOf(pLocalVariableTypes);
    outputTypes = ImmutableList.copyOf(pOutputTypes);
  }

  public ImmutableList<SvLibType> getInputTypes() {
    return inputTypes;
  }

  public ImmutableList<SvLibType> getLocalVariableTypes() {
    return localVariableTypes;
  }

  @Override
  public String toASTString(String declarator) {
    return declarator
        + " ("
        + String.join(
            "", FluentIterable.from(getInputTypes()).transform(x -> "(" + x.toASTString("") + ")"))
        + ") ("
        + String.join(
            ", ",
            FluentIterable.from(getLocalVariableTypes())
                .transform(x -> "(" + x.toASTString("") + ")"))
        + ") ("
        + String.join(
            ", ",
            FluentIterable.from(getReturnType().getElementTypes())
                .transform(x -> "(" + x.toASTString("") + ")"))
        + ")";
  }

  @Override
  public String toASTString() {
    return "("
        + Joiner.on(") (").join(inputTypes)
        + ") ("
        + Joiner.on(", ").join(localVariableTypes)
        + ") ("
        + Joiner.on(", ").join(outputTypes)
        + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibProcedureType other
        && inputTypes.equals(other.inputTypes)
        && localVariableTypes.equals(other.localVariableTypes)
        && outputTypes.equals(other.outputTypes);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + inputTypes.hashCode();
    result = prime * result + localVariableTypes.hashCode();
    result = prime * result + outputTypes.hashCode();
    return result;
  }

  @Override
  public SvLibProductType getReturnType() {
    return new SvLibProductType(outputTypes);
  }

  @Override
  public ImmutableList<SvLibType> getParameters() {
    return getInputTypes();
  }

  @Override
  public boolean takesVarArgs() {
    return false;
  }
}
