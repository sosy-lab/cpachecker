// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibProcedureType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibProductType;

public final class SvLibProcedureDeclaration implements SvLibParsingDeclaration {
  @Serial private static final long serialVersionUID = 5272479408537906183L;
  private final FileLocation fileLocation;
  private final String name;
  private ImmutableList<SvLibParsingParameterDeclaration> parameters;
  private ImmutableList<SvLibParsingParameterDeclaration> localVariables;
  private ImmutableList<SvLibParsingParameterDeclaration> returnValues;

  public SvLibProcedureDeclaration(
      FileLocation pFileLocation,
      String pName,
      List<SvLibParsingParameterDeclaration> pParameters,
      List<SvLibParsingParameterDeclaration> pReturnValues,
      List<SvLibParsingParameterDeclaration> pLocalVariables) {
    fileLocation = pFileLocation;
    name = pName;
    parameters = ImmutableList.copyOf(pParameters);
    localVariables = ImmutableList.copyOf(pLocalVariables);
    returnValues = ImmutableList.copyOf(pReturnValues);
  }

  @Override
  public SvLibProcedureType getType() {
    return new SvLibProcedureType(
        transformedImmutableListCopy(parameters, SvLibParsingParameterDeclaration::getType),
        transformedImmutableListCopy(localVariables, SvLibParsingParameterDeclaration::getType),
        transformedImmutableListCopy(returnValues, SvLibParsingParameterDeclaration::getType));
  }

  @Override
  public @Nullable String getProcedureName() {
    return name;
  }

  @Override
  public SvLibFunctionDeclaration toSimpleDeclaration() {
    return new SvLibFunctionDeclaration(
        fileLocation,
        new SvLibFunctionType(
            transformedImmutableListCopy(parameters, SvLibParsingParameterDeclaration::getType),
            new SvLibProductType(
                transformedImmutableListCopy(
                    returnValues, SvLibParsingParameterDeclaration::getType))),
        name,
        name,
        transformedImmutableListCopy(
            parameters, SvLibParsingParameterDeclaration::toSimpleDeclaration));
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return name
        + variablesToString(parameters)
        + " "
        + variablesToString(returnValues)
        + " "
        + variablesToString(localVariables);
  }

  private String variablesToString(List<SvLibParsingParameterDeclaration> variableList) {
    if (variableList.isEmpty()) {
      return "()";
    } else {
      return "(("
          + Joiner.on(") (")
              .join(
                  variableList.stream()
                      .map(var -> var.getName() + " " + var.getType().toASTString())
                      .toList())
          + "))";
    }
  }

  public ImmutableList<SvLibParsingParameterDeclaration> getParameters() {
    return parameters;
  }

  public ImmutableList<SvLibParsingParameterDeclaration> getLocalVariables() {
    return localVariables;
  }

  public @NonNull ImmutableList<@NonNull SvLibParsingParameterDeclaration> getReturnValues() {
    return returnValues;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibProcedureDeclaration other
        && parameters.equals(other.parameters)
        && localVariables.equals(other.localVariables)
        && returnValues.equals(other.returnValues);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + parameters.hashCode();
    result = prime * result + localVariables.hashCode();
    result = prime * result + returnValues.hashCode();
    return result;
  }

  public String getName() {
    return name;
  }
}
