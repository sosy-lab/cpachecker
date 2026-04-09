// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Termination candidate for loop-internal exit guards such as {@code do-while} conditions or {@code
 * break}-style exits.
 *
 * <p>Unlike {@link FrontierEdgeFormulaNegation}, this candidate is not tied to one exact CFA
 * location. Instead, it is checked on all frontier states that are still inside the loop.
 */
public final class LoopScopedFrontierEdgeFormulaNegation extends EdgeFormulaNegation {

  private final ImmutableSet<CFANode> applicableLocations;

  public LoopScopedFrontierEdgeFormulaNegation(
      CFANode pRepresentativeLocation,
      ImmutableSet<CFANode> pApplicableLocations,
      AssumeEdge pEdge) {
    super(pRepresentativeLocation, pEdge);
    applicableLocations = Objects.requireNonNull(pApplicableLocations);
  }

  @Override
  public boolean appliesTo(CFANode pLocation) {
    return applicableLocations.contains(pLocation);
  }

  @Override
  public Iterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    return AbstractStates.filterLocations(pStates, applicableLocations)
        .filter(
            state -> {
              LoopBoundState loopBoundState =
                  AbstractStates.extractStateByType(state, LoopBoundState.class);
              return loopBoundState != null && loopBoundState.mustDumpAssumptionForAvoidance();
            });
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    // Keep loop-scoped candidates observational only.
    // The single-location pruning logic from EdgeFormulaNegation does not generalize to
    // loop-scoped applicability.
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof LoopScopedFrontierEdgeFormulaNegation other
        && super.equals(other)
        && applicableLocations.equals(other.applicableLocations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), applicableLocations);
  }

  @Override
  public String toString() {
    return "loop-scoped frontier " + super.toString();
  }
}
