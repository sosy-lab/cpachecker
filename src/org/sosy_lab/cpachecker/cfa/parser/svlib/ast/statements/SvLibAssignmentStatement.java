// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;

public final class SvLibAssignmentStatement extends SvLibStatement {
  @Serial private static final long serialVersionUID = 5878865332404007544L;
  private final ImmutableMap<SvLibSimpleParsingDeclaration, SvLibTerm> assignments;

  public SvLibAssignmentStatement(
      Map<SvLibSimpleParsingDeclaration, SvLibTerm> pAssignments,
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
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTStringWithoutTags() {
    return "(assign "
        + ("("
            + Joiner.on(") (")
                .join(
                    FluentIterable.from(assignments.entrySet())
                        .transform(
                            entry ->
                                entry.getKey().toASTString()
                                    + " "
                                    + entry.getValue().toASTString()))
            + ")")
        + ")";
  }

  public ImmutableMap<SvLibSimpleParsingDeclaration, SvLibTerm> getAssignments() {
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
}
