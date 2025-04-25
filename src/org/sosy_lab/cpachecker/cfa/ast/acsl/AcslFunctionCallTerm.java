// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslFunctionCallTerm extends AcslTerm {

  @Serial private static final long serialVersionUID = -612040123327639887L;
  private final AcslTerm functionName;
  private final List<AcslTerm> parameters;
  private final AcslFunctionDeclaration declaration;

  public AcslFunctionCallTerm(
      FileLocation pLocation,
      AcslType pType,
      AcslTerm pFunctionName,
      List<AcslTerm> pParameters,
      AcslFunctionDeclaration pDeclaration) {
    super(pLocation, pType);
    functionName = pFunctionName;
    parameters = pParameters;
    declaration = pDeclaration;
  }

  public AcslTerm getFunctionName() {
    return functionName;
  }

  public List<AcslTerm> getParameters() {
    return parameters;
  }

  public AcslFunctionDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return functionName.toASTString(pAAstNodeRepresentation)
        + "("
        + parameters.stream()
            .map(p -> p.toASTString(pAAstNodeRepresentation))
            .reduce((a, b) -> a + ", " + b)
            .orElse("")
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return functionName.toParenthesizedASTString(pAAstNodeRepresentation)
        + "("
        + parameters.stream()
            .map(p -> p.toParenthesizedASTString(pAAstNodeRepresentation))
            .reduce((a, b) -> a + ", " + b)
            .orElse("")
        + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 5;
    int result = 7;
    result = prime * result + functionName.hashCode();
    result = prime * result + parameters.hashCode();
    result = prime * result + declaration.hashCode();
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslFunctionCallTerm other
        && super.equals(other)
        && Objects.equals(other.functionName, functionName)
        && Objects.equals(other.parameters, parameters)
        && Objects.equals(other.declaration, declaration);
  }
}
