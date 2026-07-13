// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

/**
 * Corresponds to a mathematical function declaration i.e. as given in SMT-LIB. It has neither
 * parameters, nor a return variable, since this is handled by assumptions in SMT-LIB
 */
public final class SvLibSmtFunctionDefinition implements SvLibParsingDeclaration {

  @Serial private static final long serialVersionUID = 8878814146461644214L;
  private final FileLocation fileLocation;
  private final String name;
  private final ImmutableList<SvLibType> inputTypes;
  private final SvLibType returnType;
  private final List<SvLibParsingParameterDeclaration> parameters;

  public SvLibSmtFunctionDefinition(
      FileLocation pFileLocation,
      String pName,
      List<SvLibType> pInputTypes,
      SvLibType pReturnType,
      List<SvLibParsingParameterDeclaration> pParameters) {
    Preconditions.checkArgument(
        pInputTypes.size() == pParameters.size(),
        "Input types and parameters must have the same size");
    for (int i = 0; i < pInputTypes.size(); i++) {
      Preconditions.checkArgument(
          pInputTypes.get(i).equals(pParameters.get(i).getType()),
          "Input type and parameter type must match for parameter %s",
          pParameters.get(i).getName());
    }
    fileLocation = pFileLocation;
    name = pName;
    inputTypes = ImmutableList.copyOf(pInputTypes);
    returnType = pReturnType;
    parameters = pParameters;
  }

  public String getName() {
    return name;
  }

  @Override
  public @NonNull String getProcedureName() {
    return name;
  }

  @Override
  public SvLibType getType() {
    if (inputTypes.isEmpty()) {
      return returnType;
    }

    return new SvLibFunctionType(inputTypes, returnType);
  }

  public SvLibType getReturnType() {
    return returnType;
  }

  public List<SvLibParsingParameterDeclaration> getParameters() {
    return parameters;
  }

  @Override
  public SvLibFunctionDeclaration toSimpleDeclaration() {
    return new SvLibFunctionDeclaration(
        fileLocation,
        new SvLibFunctionType(inputTypes, returnType),
        name,
        name,
        ImmutableList.copyOf(
            parameters.stream()
                .map(SvLibParsingParameterDeclaration::toSimpleDeclaration)
                .toList()));
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
    return getType().toASTString(getName());
  }

  @Override
  public int hashCode() {
    return name.hashCode() * 31 + getType().hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibSmtFunctionDefinition other
        && name.equals(other.name)
        && getType().equals(other.getType());
  }
}
