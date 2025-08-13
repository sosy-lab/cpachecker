// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class PointerAnalysisTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;

  private final PointerTransferOptions options;

  private final AtomicInteger allocationCounter = new AtomicInteger(0);

  private final MachineModel machineModel;

  public PointerAnalysisTransferRelation(
      LogManager pLogger, PointerTransferOptions pOptions, CFA pCfa) {
    logger = pLogger;
    options = pOptions;
    machineModel = pCfa.getMachineModel();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    PointerAnalysisState pointerState = (PointerAnalysisState) pState;

    PointerAnalysisState resultState = handlerFor(pCfaEdge).handleEdge(pointerState, pCfaEdge);

    if (resultState == PointerAnalysisState.BOTTOM_STATE) {
      return ImmutableSet.of();
    }
    return Collections.<AbstractState>singleton(resultState);
  }

  /** Binds the handler type parameter to the concrete edge type E to avoid using a raw type. */
  private <E extends CFAEdge> TransferRelationEdgeHandler<E> handlerFor(E edge) {
    @SuppressWarnings("unchecked")
    TransferRelationEdgeHandler<E> handler =
        (TransferRelationEdgeHandler<E>)
            TransferRelationEdgeHandlerFactory.createEdgeHandler(
                edge.getEdgeType(), logger, options, allocationCounter, machineModel);
    return handler;
  }
}
