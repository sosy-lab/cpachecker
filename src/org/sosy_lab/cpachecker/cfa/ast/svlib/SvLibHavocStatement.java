// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibHavocStatement extends SvLibCfaEdgeStatement {
  @Serial private static final long serialVersionUID = 3102375106958425786L;
  private final ImmutableList<SvLibSimpleDeclaration> variables;

  public SvLibHavocStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences,
      List<SvLibSimpleDeclaration> pVariables) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    variables = ImmutableList.copyOf(pVariables);
  }

  public ImmutableList<SvLibSimpleDeclaration> getVariables() {
    return variables;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(havoc (" + Joiner.on(" ").join(variables) + "))";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(havoc (" + Joiner.on(" ").join(variables) + "))";
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + variables.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof SvLibHavocStatement other
        && super.equals(other)
        && variables.equals(other.variables);
  }
}
