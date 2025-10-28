// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.collect.FluentIterable;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.java_smt.api.FormulaType;

public final class K3FunctionType implements K3Type, AFunctionType {
  @Serial private static final long serialVersionUID = -6676402211597555266L;

  private final FileLocation fileLocation;
  private final List<K3Type> inputType;
  private final K3Type outputType;

  public K3FunctionType(FileLocation pFileLocation, List<K3Type> pInputType, K3Type pOutputType) {
    fileLocation = pFileLocation;
    inputType = pInputType;
    outputType = pOutputType;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public List<K3Type> getInputType() {
    return inputType;
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
        + getReturnType()
        + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3FunctionType other
        && inputType.equals(other.inputType)
        && outputType.equals(other.outputType);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + inputType.hashCode();
    result = prime * result + outputType.hashCode();
    return result;
  }

  @Override
  public K3Type getReturnType() {
    return outputType;
  }

  @Override
  public List<K3Type> getParameters() {
    return getInputType();
  }

  @Override
  public boolean takesVarArgs() {
    return false;
  }
}
