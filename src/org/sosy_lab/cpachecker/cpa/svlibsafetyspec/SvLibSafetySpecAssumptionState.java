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
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.ForgetfulAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class SvLibSafetySpecAssumptionState
    implements AbstractStateWithAssumptions,
        Graphable,
        AbstractQueryableState,
        ForgetfulAbstractState {

  private final ImmutableSet<@NonNull SvLibRelationalTerm> assumptions;
  private final Set<SvLibSimpleDeclaration> declarationsToHavoc;
  private final boolean hasPropertyViolation;

  public SvLibSafetySpecAssumptionState(
      Set<SvLibRelationalTerm> pAssumptions,
      Set<SvLibSimpleDeclaration> pDeclarationsToHavoc,
      boolean pHasPropertyViolation) {
    assumptions = ImmutableSet.copyOf(pAssumptions);
    declarationsToHavoc = pDeclarationsToHavoc;
    hasPropertyViolation = pHasPropertyViolation;
  }

  boolean hasPropertyViolation() {
    return hasPropertyViolation;
  }

  @Override
  public List<? extends AExpression> getAssumptions() {
    return assumptions.asList();
  }

  @Override
  public int hashCode() {
    return Objects.hash(assumptions, declarationsToHavoc, hasPropertyViolation);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibSafetySpecAssumptionState other
        && Objects.equals(assumptions, other.assumptions)
        && Objects.equals(declarationsToHavoc, other.declarationsToHavoc)
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

  String getReadableAssumptions() {
    return Joiner.on(", ").join(FluentIterable.from(assumptions).transform(AAstNode::toASTString));
  }

  String getRedableVariablesToHavoc() {
    return Joiner.on(", ")
        .join(
            FluentIterable.from(declarationsToHavoc)
                .transform(SvLibSimpleDeclaration::toASTString));
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

  @Override
  public Set<ASimpleDeclaration> getForgettableVariables() {
    return FluentIterable.from(declarationsToHavoc).filter(ASimpleDeclaration.class).toSet();
  }
}
