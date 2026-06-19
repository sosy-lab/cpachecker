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
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Termination candidate for a loop-head frontier that blocks a concrete path from the loop head to
 * an internal branch that stays in the loop.
 *
 * <p>The candidate is attached to the loop head, not to the internal branch location, because
 * LoopBoundCPA usually creates frontier states when a path reaches a loop head beyond the current
 * bound.
 */
public final class LoopHeadPathFormulaNegation extends SingleLocationFormulaInvariant {

  private final ImmutableList<CFAEdge> prefixEdges;
  private final AssumeEdge continuationEdge;

  public LoopHeadPathFormulaNegation(
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
    return pFMGR.getBooleanFormulaManager().not(pFMGR.uninstantiate(pathFormula.getFormula()));
  }

  @Override
  public Iterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    return FluentIterable.from(AbstractStates.filterLocation(pStates, getLocation()))
        .filter(
            state -> {
              LoopBoundState loopBoundState =
                  AbstractStates.extractStateByType(state, LoopBoundState.class);
              return loopBoundState != null && loopBoundState.mustDumpAssumptionForAvoidance();
            });
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof LoopHeadPathFormulaNegation other
        && getLocation().equals(other.getLocation())
        && prefixEdges.equals(other.prefixEdges)
        && continuationEdge.equals(other.continuationEdge);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLocation(), prefixEdges, continuationEdge);
  }

  @Override
  public String toString() {
    return "frontier path-negation from "
        + getLocation()
        + " via "
        + continuationEdge.getRawStatement();
  }
}
