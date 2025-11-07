// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibProcedureDeclaration extends AFunctionDeclaration
    implements SvLibDeclaration {
  @Serial private static final long serialVersionUID = 5272479408537906183L;
  private List<SvLibParameterDeclaration> parameters;
  private List<SvLibParameterDeclaration> localVariables;
  private List<SvLibParameterDeclaration> returnValues;

  public SvLibProcedureDeclaration(
      FileLocation pFileLocation,
      String pName,
      List<SvLibParameterDeclaration> pParameters,
      List<SvLibParameterDeclaration> pReturnValues,
      List<SvLibParameterDeclaration> pLocalVariables) {
    // The type of the procedure declaration can be inferred from the parameters, since there is no
    // anonymous parameters and no function declaration using only the types
    super(
        pFileLocation,
        new SvLibProcedureType(
            pFileLocation,
            transformedImmutableListCopy(pParameters, SvLibParameterDeclaration::getType),
            transformedImmutableListCopy(pLocalVariables, SvLibParameterDeclaration::getType),
            transformedImmutableListCopy(pReturnValues, SvLibParameterDeclaration::getType)),
        pName,
        pName,
        pParameters);
    parameters = pParameters;
    localVariables = pLocalVariables;
    returnValues = pReturnValues;
  }

  public static SvLibProcedureDeclaration mainFunctionDeclaration() {
    return new SvLibProcedureDeclaration(
        FileLocation.DUMMY,
        "__VERIFIER_MAIN",
        ImmutableList.of(),
        ImmutableList.of(),
        ImmutableList.of());
  }

  @Override
  public String getQualifiedName() {
    return getName();
  }

  @Override
  public SvLibProcedureType getType() {
    return (SvLibProcedureType) super.getType();
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    return getType().toASTString(getName());
  }

  @Override
  public List<SvLibParameterDeclaration> getParameters() {
    return parameters;
  }

  public List<SvLibParameterDeclaration> getLocalVariables() {
    return localVariables;
  }

  public @NonNull List<@NonNull SvLibParameterDeclaration> getReturnValues() {
    return returnValues;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibProcedureDeclaration other
        && super.equals(other)
        && parameters.equals(other.parameters)
        && localVariables.equals(other.localVariables)
        && returnValues.equals(other.returnValues);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + super.hashCode();
    result = prime * result + parameters.hashCode();
    result = prime * result + localVariables.hashCode();
    result = prime * result + returnValues.hashCode();
    return result;
  }
}
