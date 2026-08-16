// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.svlibsafetyspec;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class SvLibSafetySpecState
    implements AbstractStateWithAssumptions, Graphable, AbstractQueryableState {

  private final ImmutableSet<@NonNull SvLibRelationalTerm> assumptions;
  private final boolean hasPropertyViolation;

  public SvLibSafetySpecState(
      Set<SvLibRelationalTerm> pAssumptions, boolean pHasPropertyViolation) {
    assumptions = ImmutableSet.copyOf(pAssumptions);
    hasPropertyViolation = pHasPropertyViolation;
  }

  public boolean hasPropertyViolation() {
    return hasPropertyViolation;
  }

  @Override
  public List<? extends AExpression> getAssumptions() {
    return assumptions.asList();
  }

  @Override
  public int hashCode() {
    return Objects.hash(assumptions, hasPropertyViolation);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibSafetySpecState other
        && Objects.equals(assumptions, other.assumptions)
        && hasPropertyViolation == other.hasPropertyViolation;
  }

  @Override
  public String toString() {
    return "SvLibSafetySpecState{assumeEdges=["
        + getReadableAssumptions()
        + "], hasPropertyViolation="
        + hasPropertyViolation
        + '}';
  }

  @Override
  public String toDOTLabel() {
    if (hasPropertyViolation) {
      return "Assumptions:\n" + getReadableAssumptions().replace(", ", "\n");
    }
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  private String getReadableAssumptions() {
    return Joiner.on(", ").join(FluentIterable.from(assumptions).transform(AAstNode::toASTString));
  }

  @Override
  public String getCPAName() {
    return "SvLibSafetySpecCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // If we are verifying a SV-LIB safety specification, this is the CPA
    // responsible for reporting property violations.
    return hasPropertyViolation;
  }
}
