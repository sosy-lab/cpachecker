// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class AssumptionStoragePrecisionAdjustment implements PrecisionAdjustment {

  private final AssumptionStorageTransferRelation transferRelation;

  public AssumptionStoragePrecisionAdjustment(AssumptionStorageTransferRelation pTransferRelation) {
    transferRelation = pTransferRelation;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {
    return StaticPrecisionAdjustment.getInstance()
        .prec(pState, pPrecision, pStates, pStateProjection, pFullState);
  }

  @Override
  public Optional<? extends AbstractState> strengthen(
      AbstractState pState, Precision pPrecision, Iterable<AbstractState> pOtherStates)
      throws CPAException, InterruptedException {
    AssumptionStorageState state = (AssumptionStorageState) pState;
    CFAEdge edge = getEdge(pOtherStates);
    return Optional.of(transferRelation.strengthen(state.reset(), pOtherStates, edge));
  }

  private CFAEdge getEdge(Iterable<AbstractState> pStates) {
    Optional<LocationState> locationState =
        from(pStates).filter(LocationState.class).first().toJavaUtil();
    final CFANode successor;
    if (locationState.isPresent()) {
      LocationState ls = locationState.orElseThrow();
      successor = ls.getLocationNode();
      if (successor.getNumEnteringEdges() == 1) {
        return successor.getEnteringEdge(0);
      }
    } else {
      successor = CFANode.newDummyCFANode("__CPAchecker_dummy");
    }
    CFANode predecessor = successor; // TODO wtf?
    return new DummyCFAEdge(predecessor, successor);
  }
}
