// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.postcondition;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * PostConditions contain the violated assertion. The precondition and the trace have to satisfy the
 * post-condition, i.e., the post-condition is the condition that is but should not be violated by
 * the program.
 */
public class PostCondition {

  private final ImmutableList<CFAEdge> edgesForPostCondition;
  private final ImmutableList<CFAEdge> irrelevantEdges;
  private final ImmutableList<CFAEdge> remainingCounterexample;
  private final BooleanFormula postCondition;

  /**
   * PostCondition wraps a {@link BooleanFormula}. Additionally, it stores on which edges it is
   * based on.
   *
   * @param pEdges the complete list of edges of the counterexample (see {@link
   *     CounterexampleInfo#getCFAPathWithAssignments()})
   * @param pIrrelevantEdges all edges after the edges that define the post-condition
   * @param pRemainingCounterexample all edges before the first edge that is part of this
   *     post-condition
   * @param pPostCondition the actual post-condition
   */
  PostCondition(
      List<CFAEdge> pEdges,
      List<CFAEdge> pIrrelevantEdges,
      List<CFAEdge> pRemainingCounterexample,
      BooleanFormula pPostCondition) {
    edgesForPostCondition = ImmutableList.copyOf(pEdges);
    irrelevantEdges = ImmutableList.copyOf(pIrrelevantEdges);
    remainingCounterexample = ImmutableList.copyOf(pRemainingCounterexample);
    postCondition = pPostCondition;
  }

  /**
   * Instantiate the post-condition if the post-condition was calculated independently of the trace.
   *
   * @param pFmgr FormulaManagerView that was used to calculate the trace
   * @param pSSAMap the SSAMap with most recent SSA-indices. Usually, the SSA-Map of the last entry
   *     in trace should be used.
   * @return a new PostCondition object with an instantiated post-condition formula.
   */
  public PostCondition instantiate(FormulaManagerView pFmgr, SSAMap pSSAMap) {
    return new PostCondition(
        edgesForPostCondition,
        irrelevantEdges,
        remainingCounterexample,
        pFmgr.instantiate(pFmgr.uninstantiate(postCondition), pSSAMap));
  }

  public BooleanFormula getPostCondition() {
    return postCondition;
  }

  public ImmutableList<CFAEdge> getEdgesForPostCondition() {
    return edgesForPostCondition;
  }

  public ImmutableList<CFAEdge> getIrrelevantEdges() {
    return irrelevantEdges;
  }

  public ImmutableList<CFAEdge> getRemainingCounterexample() {
    return remainingCounterexample;
  }

  /**
   * Create a post-condition solely based on a boolean formula.
   *
   * @param pPostCondition a post-condition
   * @return a new post-condition wrapping {@code pPostCondition}
   */
  public static PostCondition of(BooleanFormula pPostCondition) {
    return new PostCondition(
        ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), pPostCondition);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("postCondition", postCondition).toString();
  }
}
