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
import org.sosy_lab.java_smt.api.FormulaType;

public final class SvLibProcedureType implements SvLibType, AFunctionType {

  @Serial private static final long serialVersionUID = 5728816904538462642L;
  private final ImmutableList<SvLibType> inputType;
  private final ImmutableList<SvLibType> localVariableTypes;
  private final ImmutableList<SvLibType> outputType;

  public SvLibProcedureType(
      List<SvLibType> pInputType,
      List<SvLibType> pLocalVariableTypes,
      List<SvLibType> pOutputType) {
    inputType = ImmutableList.copyOf(pInputType);
    localVariableTypes = ImmutableList.copyOf(pLocalVariableTypes);
    outputType = ImmutableList.copyOf(pOutputType);
  }

  public ImmutableList<SvLibType> getInputType() {
    return inputType;
  }

  public ImmutableList<SvLibType> getLocalVariableTypes() {
    return localVariableTypes;
  }

  @Override
  public FormulaType<?> toFormulaType() {
    throw new UnsupportedOperationException("JavaSMT does not support custom types");
  }

  @Override
  public String toASTString(String declarator) {
    return declarator
        + " ("
        + String.join(
            "", FluentIterable.from(getInputType()).transform(x -> "(" + x.toASTString("") + ")"))
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
  public String toPlainString() {
    return "("
        + Joiner.on(") (").join(inputType)
        + ") ("
        + Joiner.on(", ").join(localVariableTypes)
        + ") ("
        + Joiner.on(", ").join(outputType)
        + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibProcedureType other
        && inputType.equals(other.inputType)
        && localVariableTypes.equals(other.localVariableTypes)
        && outputType.equals(other.outputType);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + inputType.hashCode();
    result = prime * result + localVariableTypes.hashCode();
    result = prime * result + outputType.hashCode();
    return result;
  }

  @Override
  public SvLibProductType getReturnType() {
    return new SvLibProductType(outputType);
  }

  @Override
  public ImmutableList<SvLibType> getParameters() {
    return getInputType();
  }

  @Override
  public boolean takesVarArgs() {
    return false;
  }
}
