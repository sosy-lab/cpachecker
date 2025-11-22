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

public final class SvLibFunctionType implements SvLibType, AFunctionType {
  @Serial private static final long serialVersionUID = -6676402211597555266L;

  private final ImmutableList<SvLibType> inputTypes;
  private final SvLibType outputType;

  public SvLibFunctionType(List<SvLibType> pInputTypes, SvLibType pOutputType) {
    inputTypes = ImmutableList.copyOf(pInputTypes);
    outputType = pOutputType;
  }

  public ImmutableList<SvLibType> getInputTypes() {
    return inputTypes;
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
            "", FluentIterable.from(getInputTypes()).transform(x -> "(" + x.toASTString("") + ")"))
        + ") ("
        + getReturnType()
        + ")";
  }

  @Override
  public String toPlainString() {
    return "(" + Joiner.on(") (").join(inputTypes) + ") " + outputType;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibFunctionType other
        && inputTypes.equals(other.inputTypes)
        && outputType.equals(other.outputType);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + inputTypes.hashCode();
    result = prime * result + outputType.hashCode();
    return result;
  }

  @Override
  public SvLibType getReturnType() {
    return outputType;
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
