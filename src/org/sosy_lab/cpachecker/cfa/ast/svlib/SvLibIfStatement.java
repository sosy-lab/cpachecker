// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibIfStatement extends SvLibControlFlowStatement {

  @Serial private static final long serialVersionUID = 8786709909853416125L;
  private final SvLibTerm condition;
  private final SvLibStatement thenBranch;
  private final Optional<SvLibStatement> elseBranch;

  public SvLibIfStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences,
      SvLibTerm pCondition,
      SvLibStatement pThenBranch) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    condition = pCondition;
    thenBranch = pThenBranch;
    elseBranch = Optional.empty();
  }

  public SvLibIfStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences,
      SvLibTerm pCondition,
      SvLibStatement pThenBranch,
      SvLibStatement pElseBranch) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    condition = pCondition;
    thenBranch = pThenBranch;
    elseBranch = Optional.of(pElseBranch);
  }

  public SvLibStatement getThenBranch() {
    return thenBranch;
  }

  public Optional<SvLibStatement> getElseBranch() {
    return elseBranch;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(if "
        + condition.toASTString(pAAstNodeRepresentation)
        + " "
        + thenBranch.toASTString(pAAstNodeRepresentation)
        + " "
        + (elseBranch.isPresent()
            ? elseBranch.orElseThrow().toASTString(pAAstNodeRepresentation)
            : "")
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(while "
        + condition.toParenthesizedASTString(pAAstNodeRepresentation)
        + " "
        + thenBranch.toParenthesizedASTString(pAAstNodeRepresentation)
        + " "
        + (elseBranch.isPresent()
            ? elseBranch.orElseThrow().toParenthesizedASTString(pAAstNodeRepresentation)
            : "")
        + ")";
  }

  public SvLibTerm getCondition() {
    return condition;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + condition.hashCode();
    result = prime * result + thenBranch.hashCode();
    result = prime * result + (elseBranch.isPresent() ? elseBranch.orElseThrow().hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibIfStatement other
        && super.equals(other)
        && condition.equals(other.condition)
        && thenBranch.equals(other.thenBranch)
        && elseBranch.equals(other.elseBranch);
  }
}
