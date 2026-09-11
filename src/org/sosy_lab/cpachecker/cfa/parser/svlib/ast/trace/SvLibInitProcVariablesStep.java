// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.io.Serial;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;

public final class SvLibInitProcVariablesStep extends SvLibTraceStep {
  @Serial private static final long serialVersionUID = -1341873304472826329L;
  private final SvLibProcedureDeclaration procedureDeclaration;
  private final ImmutableMap<SvLibIdTerm, SvLibConstantTerm> assignments;

  public SvLibInitProcVariablesStep(
      SvLibProcedureDeclaration pProcedureDeclaration,
      Map<SvLibIdTerm, SvLibConstantTerm> pValues,
      FileLocation pFileLocation) {
    super(pFileLocation);
    procedureDeclaration = pProcedureDeclaration;
    assignments = ImmutableMap.copyOf(pValues);
  }

  @Override
  <R, X extends Exception> R accept(SvLibTraceComponentVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString() {
    return "(init-proc-vars "
        + procedureDeclaration.getName()
        + " "
        + Joiner.on(" ")
            .join(
                assignments.entrySet().stream()
                    .map(
                        entry ->
                            "("
                                + entry.getKey().toASTString()
                                + " "
                                + entry.getValue().toASTString()
                                + ")")
                    .toList())
        + ")";
  }

  public ImmutableMap<SvLibIdTerm, SvLibConstantTerm> getAssignments() {
    return assignments;
  }

  public SvLibProcedureDeclaration getProcedureDeclaration() {
    return procedureDeclaration;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + assignments.hashCode();
    result = prime * result + procedureDeclaration.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibInitProcVariablesStep other
        && procedureDeclaration.equals(other.procedureDeclaration)
        && assignments.equals(other.assignments);
  }
}
