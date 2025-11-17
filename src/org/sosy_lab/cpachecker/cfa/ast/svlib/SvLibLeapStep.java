// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.base.Joiner;
import java.io.Serial;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibLeapStep extends SvLibTraceStep {

  @Serial private static final long serialVersionUID = 8365995208626503450L;
  private final Map<SvLibIdTerm, SvLibConstantTerm> assignments;
  private final String leapTag;

  SvLibLeapStep(
      FileLocation pFileLocation,
      Map<SvLibIdTerm, SvLibConstantTerm> pAssignments,
      String pLeapTag) {
    super(pFileLocation);
    assignments = pAssignments;
    leapTag = pLeapTag;
  }

  @Override
  <R, X extends Exception> R accept(SvLibTraceComponentVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(leap "
        + leapTag
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

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + leapTag.hashCode();
    result = prime * result + assignments.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibLeapStep other
        && leapTag.equals(other.leapTag)
        && assignments.equals(other.assignments);
  }

  public Map<SvLibIdTerm, SvLibConstantTerm> getAssignments() {
    return assignments;
  }

  public String getLeapTag() {
    return leapTag;
  }
}
