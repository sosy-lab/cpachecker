// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessViolationV2Parser;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * Hands the wrapped CPA the original CFA edge instead of the per-thread clone POR explores.
 * Specification automata match an edge against the CFA nodes the witness parser resolved from the
 * unmodified CFA, which no cloned node is ever identical to.
 */
public final class OriginalEdgeCPA extends AbstractSingleWrapperCPA {

  private final TransferRelation transferRelation;

  OriginalEdgeCPA(ConfigurableProgramAnalysis pWrapped, ActiveWitnessThread pActiveWitnessThread) {
    super(pWrapped);
    transferRelation =
        new OriginalEdgeTransferRelation(pWrapped.getTransferRelation(), pActiveWitnessThread);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  private record OriginalEdgeTransferRelation(
      TransferRelation delegate, ActiveWitnessThread activeWitnessThread)
      implements TransferRelation {

    @Override
    public Collection<? extends AbstractState> getAbstractSuccessors(
        AbstractState pState, Precision pPrecision)
        throws CPATransferException, InterruptedException {
      return delegate.getAbstractSuccessors(pState, pPrecision);
    }

    @Override
    public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
        AbstractState pState, Precision pPrecision, CFAEdge pEdge)
        throws CPATransferException, InterruptedException {
      return delegate.getAbstractSuccessorsForEdge(
          pState, pPrecision, ConcurrentEdgeCloner.getOriginalEdge(pEdge));
    }

    @Override
    public Collection<? extends AbstractState> strengthen(
        AbstractState pState,
        Iterable<AbstractState> pOtherStates,
        @Nullable CFAEdge pEdge,
        Precision pPrecision)
        throws CPATransferException, InterruptedException {
      Iterable<AbstractState> others = pOtherStates;
      if (pEdge != null) {
        // which thread the witness means is not something any state of the composite knows
        others =
            ImmutableList.<AbstractState>builder()
                .addAll(pOtherStates)
                .add(new ActiveThreadState(activeWitnessThread.get()))
                .build();
      }
      return delegate.strengthen(
          pState,
          others,
          pEdge == null ? null : ConcurrentEdgeCloner.getOriginalEdge(pEdge),
          pPrecision);
    }
  }

  /** Answers the witness automaton's thread query, which only a sibling state can. */
  private record ActiveThreadState(int witnessThreadId) implements AbstractQueryableState {

    @Override
    public String getCPAName() {
      return "ConcurrentCPA";
    }

    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      if (!pProperty.startsWith(AutomatonWitnessViolationV2Parser.THREAD_ID_QUERY)) {
        throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
      }
      String expected =
          pProperty.substring(AutomatonWitnessViolationV2Parser.THREAD_ID_QUERY.length());
      try {
        // a thread the witness hands out no identifier for matches none of its waypoints
        return witnessThreadId >= 0 && witnessThreadId == Integer.parseInt(expected);
      } catch (NumberFormatException e) {
        throw new InvalidQueryException(
            "Query '" + pProperty + "' does not compare against an integer.", e);
      }
    }
  }
}
