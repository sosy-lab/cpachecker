// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.svlibsafetyspec;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class SvLibSafetySpecFutureAssertionState extends SvLibSafetySpecAssumptionState
    implements AbstractStateWithAssumptions, Graphable, AbstractQueryableState {

  private final CFANode nodeForAssertion;
  private final SvLibRelationalTerm assertion;

  public SvLibSafetySpecFutureAssertionState(
      Set<SvLibRelationalTerm> pAssumptions,
      Set<SvLibSimpleDeclaration> pDeclarationsToHavoc,
      CFANode pNodeForAssertion,
      SvLibRelationalTerm pAssertion) {
    // We can never have a property violation in a future assertion state,
    // since we want to check the assertions in the future, where it is transformed
    // into a normal assumption state with a property violation.
    super(pAssumptions, pDeclarationsToHavoc, false);
    nodeForAssertion = pNodeForAssertion;
    assertion = pAssertion;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibSafetySpecFutureAssertionState other && super.equals(other);
  }

  @Override
  public String toString() {
    return "SvLibSafetySpecFutureAssertionState{assumeEdges=["
        + getReadableAssumptions()
        + ", declarationsToHavoc=["
        + getRedableVariablesToHavoc()
        + "], assertionsEdges="
        + assertion.toASTString()
        + ", hasPropertyViolation="
        + hasPropertyViolation()
        + ", nodeForAssertion="
        + nodeForAssertion.getNodeNumber()
        + '}';
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String getCPAName() {
    return "SvLibSafetySpecCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    return false;
  }
}
