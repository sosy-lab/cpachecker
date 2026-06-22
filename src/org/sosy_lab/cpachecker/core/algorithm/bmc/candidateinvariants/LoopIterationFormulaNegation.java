// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 CPAchecker contributors
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * A frontier-only termination candidate for one additional deterministic loop iteration.
 *
 * <p>The candidate is evaluated at a loop head. It says that from the current loop-head frontier,
 * the deterministic prefix through the loop body followed by the selected internal continuation
 * edge is infeasible.
 */
public final class LoopIterationFormulaNegation extends SingleLocationFormulaInvariant {

  private final ImmutableList<CFAEdge> prefixEdges;
  private final AssumeEdge continuationEdge;

  public LoopIterationFormulaNegation(
      CFANode pLoopHead, Iterable<CFAEdge> pPrefixEdges, AssumeEdge pContinuationEdge) {
    super(pLoopHead);
    prefixEdges = ImmutableList.copyOf(pPrefixEdges);
    continuationEdge = Objects.requireNonNull(pContinuationEdge);
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
      throws CPATransferException, InterruptedException {
    PathFormula pathFormula = pPFMGR.makeEmptyPathFormulaWithContextFrom(pContext);
    for (CFAEdge edge : prefixEdges) {
      pathFormula = pPFMGR.makeAnd(pathFormula, edge);
    }
    pathFormula = pPFMGR.makeAnd(pathFormula, continuationEdge);
    return pFMGR
        .getBooleanFormulaManager()
        .not(pFMGR.uninstantiate(pathFormula.getFormula()));
  }

  @Override
  public Iterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    return FluentIterable.from(super.filterApplicable(pStates))
        .filter(
            state -> {
              LoopBoundState loopBoundState =
                  AbstractStates.extractStateByType(state, LoopBoundState.class);
              return loopBoundState != null && loopBoundState.mustDumpAssumptionForAvoidance();
            });
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    // Keep frontier-only termination candidates observational only.
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof LoopIterationFormulaNegation other
        && getLocation().equals(other.getLocation())
        && prefixEdges.equals(other.prefixEdges)
        && continuationEdge.getTruthAssumption() == other.continuationEdge.getTruthAssumption()
        && continuationEdge.getExpression().equals(other.continuationEdge.getExpression());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getLocation(),
        prefixEdges,
        continuationEdge.getTruthAssumption(),
        continuationEdge.getExpression());
  }

  @Override
  public String toString() {
    return "loop-iteration frontier " + continuationEdge.getRawStatement();
  }
}
