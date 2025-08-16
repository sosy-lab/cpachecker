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
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class K3ProcedureType implements K3Type, AFunctionType {

  @Serial private static final long serialVersionUID = 5728816904538462642L;
  private final FileLocation fileLocation;
  private final List<K3Type> inputType;
  private final List<K3Type> localVariableTypes;
  private final List<K3Type> outputType;

  public K3ProcedureType(
      FileLocation pFileLocation,
      List<K3Type> pInputType,
      List<K3Type> pLocalVariableTypes,
      List<K3Type> pOutputType) {
    fileLocation = pFileLocation;
    inputType = pInputType;
    localVariableTypes = pLocalVariableTypes;
    outputType = pOutputType;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public List<K3Type> getInputType() {
    return inputType;
  }

  public List<K3Type> getLocalVariableTypes() {
    return localVariableTypes;
  }

  public List<K3Type> getOutputType() {
    return outputType;
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
            FluentIterable.from(getOutputType()).transform(x -> "(" + x.toASTString("") + ")"))
        + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3ProcedureType other
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
  public Type getReturnType() {
    return new K3ProductType(outputType);
  }

  @Override
  public List<? extends Type> getParameters() {
    return getInputType();
  }

  @Override
  public boolean takesVarArgs() {
    return false;
  }
}
