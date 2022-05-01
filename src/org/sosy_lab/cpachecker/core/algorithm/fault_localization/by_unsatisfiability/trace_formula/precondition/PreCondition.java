// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * PreConditions contain the initial variable assignment responsible for satisfying the
 * post-condition. Preconditions can consist of only a subset of the actual variable assignment. For
 * programs without nondeterministic variables `true` is also a valid precondition.
 */
public class PreCondition {

  private final ImmutableList<CFAEdge> edgesForPrecondition;
  private final ImmutableList<CFAEdge> remainingCounterexample;
  private final BooleanFormula precondition;

  /**
   * The Precondition wraps a {@code BooleanFormula}. Additionally, it stores on which edges it is
   * based on.
   *
   * @param pEdges declaration and statement edges responsible for the initial variable assignment
   * @param pRemainingCounterexample all edges of a complete counterexample that are not part of the
   *     precondition (see {@link CounterexampleInfo#getCFAPathWithAssignments()})
   * @param pPrecondition the actual precondition as {@link BooleanFormula}
   */
  PreCondition(
      List<CFAEdge> pEdges, List<CFAEdge> pRemainingCounterexample, BooleanFormula pPrecondition) {
    edgesForPrecondition = ImmutableList.copyOf(pEdges);
    remainingCounterexample = ImmutableList.copyOf(pRemainingCounterexample);
    precondition = pPrecondition;
  }

  /**
   * Precondition solely based on a condition.
   *
   * @param pPrecondition BooleanFormula of a pre-condition.
   * @return an object of {@link PreCondition} wrapping the provided {@link BooleanFormula}
   */
  public static PreCondition of(BooleanFormula pPrecondition) {
    return new PreCondition(ImmutableList.of(), ImmutableList.of(), pPrecondition);
  }

  public ImmutableList<CFAEdge> getRemainingCounterexample() {
    return remainingCounterexample;
  }

  public BooleanFormula getPrecondition() {
    return precondition;
  }

  public ImmutableList<CFAEdge> getEdgesForPrecondition() {
    return edgesForPrecondition;
  }

  public PreCondition instantiate(FormulaManagerView pFmgr, SSAMap pSSAMap) {
    return new PreCondition(
        edgesForPrecondition,
        remainingCounterexample,
        pFmgr.instantiate(pFmgr.uninstantiate(precondition), pSSAMap));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("precondition", precondition).toString();
  }
}
