// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Hands the wrapped CPA the original CFA edge instead of the per-thread clone POR explores.
 * Specification automata match an edge against the CFA nodes the witness parser resolved from the
 * unmodified CFA, which no cloned node is ever identical to.
 */
public final class OriginalEdgeCPA extends AbstractSingleWrapperCPA {

  private final TransferRelation transferRelation;

  OriginalEdgeCPA(ConfigurableProgramAnalysis pWrapped) {
    super(pWrapped);
    transferRelation = new OriginalEdgeTransferRelation(pWrapped.getTransferRelation());
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  private record OriginalEdgeTransferRelation(TransferRelation delegate)
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
      return delegate.strengthen(
          pState,
          pOtherStates,
          pEdge == null ? null : ConcurrentEdgeCloner.getOriginalEdge(pEdge),
          pPrecision);
    }
  }
}
