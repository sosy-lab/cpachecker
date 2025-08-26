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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3ProcedureDeclaration extends AFunctionDeclaration implements K3Declaration {
  @Serial private static final long serialVersionUID = 5272479408537906183L;
  private List<K3ParameterDeclaration> parameters;
  private List<K3ParameterDeclaration> localVariables;
  private List<K3ParameterDeclaration> returnValues;

  public K3ProcedureDeclaration(
      FileLocation pFileLocation,
      String pName,
      List<K3ParameterDeclaration> pParameters,
      List<K3ParameterDeclaration> pLocalVariables,
      List<K3ParameterDeclaration> pReturnValues) {
    // The type of the procedure declaration can be inferred from the parameters, since there is no
    // anonymous parameters and no function declaration using only the types
    super(
        pFileLocation,
        new K3ProcedureType(
            pFileLocation,
            FluentIterable.from(pParameters).transform(K3ParameterDeclaration::getType).toList(),
            FluentIterable.from(pLocalVariables)
                .transform(K3ParameterDeclaration::getType)
                .toList(),
            FluentIterable.from(pReturnValues).transform(K3ParameterDeclaration::getType).toList()),
        pName,
        pName,
        pParameters);
    parameters = pParameters;
    localVariables = pLocalVariables;
    returnValues = pReturnValues;
  }

  public static K3ProcedureDeclaration mainFunctionDeclaration() {
    return new K3ProcedureDeclaration(
        FileLocation.DUMMY, "__VERIFIER_MAIN", List.of(), List.of(), List.of());
  }

  @Override
  public String getQualifiedName() {
    return getName();
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    return getType().toASTString(getName());
  }

  @Override
  public List<K3ParameterDeclaration> getParameters() {
    return parameters;
  }

  public List<K3ParameterDeclaration> getLocalVariables() {
    return localVariables;
  }

  public List<K3ParameterDeclaration> getReturnValues() {
    return returnValues;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3ProcedureDeclaration other
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
