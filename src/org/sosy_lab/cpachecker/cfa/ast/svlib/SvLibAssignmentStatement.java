// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.AStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibCfaEdgeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatementVisitor;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagProperty;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagReference;

public final class SvLibAssignmentStatement extends SvLibCfaEdgeStatement {
  @Serial private static final long serialVersionUID = 5878865332404007544L;
  private final ImmutableMap<SvLibSimpleDeclaration, SvLibTerm> assignments;

  public SvLibAssignmentStatement(
      Map<SvLibSimpleDeclaration, SvLibTerm> pAssignments,
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    assignments = ImmutableMap.copyOf(pAssignments);
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
    return "(assign "
        + ("("
            + Joiner.on(") (")
                .join(
                    FluentIterable.from(assignments.entrySet())
                        .transform(
                            entry ->
                                entry.getKey().getOrigName()
                                    + " "
                                    + entry.getValue().toASTString()))
            + ")")
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  public ImmutableMap<SvLibSimpleDeclaration, SvLibTerm> getAssignments() {
    return assignments;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibAssignmentStatement other
        && assignments.equals(other.assignments)
        && super.equals(pO);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = result * prime + assignments.hashCode();
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
