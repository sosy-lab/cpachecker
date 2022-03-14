// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Function;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions.SMGExportLevel;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public class SMGPrecisionAdjustment implements PrecisionAdjustment {

  private final SMGStatistics statistics;
  private final LogManager logger;
  private final SMGExportDotOption exportOptions;
  private final BlockOperator blockOperator;

  public SMGPrecisionAdjustment(
      LogManager pLogger,
      SMGExportDotOption pExportOptions,
      BlockOperator pBlockOperator,
      SMGStatistics pStats) {
    logger = pLogger;
    exportOptions = pExportOptions;
    blockOperator = pBlockOperator;
    statistics = pStats;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException {

    CFANode node = AbstractStates.extractLocation(pFullState);
    UnmodifiableSMGState state = (UnmodifiableSMGState) pState;
    state = state.copyWithBlockEnd(blockOperator.isBlockEnd(node, 0));

    return prec(state, (SMGPrecision) pPrecision, node);
  }

  private Optional<PrecisionAdjustmentResult> prec(
      UnmodifiableSMGState pState, SMGPrecision pPrecision, CFANode node) throws CPAException {

    boolean allowsFieldAbstraction = pPrecision.getAbstractionOptions().allowsFieldAbstraction();
    boolean allowsHeapAbstraction = pPrecision.allowsHeapAbstractionOnNode(node, blockOperator);
    boolean allowsStackAbstraction = pPrecision.getAbstractionOptions().allowsStackAbstraction();

    if (!allowsFieldAbstraction && !allowsHeapAbstraction && !allowsStackAbstraction) {
      return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.CONTINUE));
    }

    statistics.totalAbstraction.start();

    UnmodifiableSMGState result = pState;
    SMGState newState = pState.copyOf();

    if (allowsStackAbstraction) {
      boolean stackAbstractionChange =
          newState.forgetNonTrackedStackVariables(pPrecision.getTrackedStackVariablesOnNode(node));

      if (stackAbstractionChange) {
        String name =
            String.format("%03d-%03d-after-stack-abstraction", result.getId(), newState.getId());
        String description = "after-stack-abstraction-of-smg-" + result.getId();
        SMGUtils.plotWhenConfigured(
            name, newState, description, logger, SMGExportLevel.EVERY, exportOptions);

        result = newState;
        logger.log(
            Level.ALL,
            "Precision adjustment on node ",
            node.getNodeNumber(),
            " with result state id: ",
            result.getId());
      }
    }

    if (allowsFieldAbstraction) {

      boolean fieldAbstractionChange =
          newState.forgetNonTrackedHve(pPrecision.getTrackedMemoryPathsOnNode(node));

      if (fieldAbstractionChange) {
        String name =
            String.format("%03d-%03d-after-field-abstraction", result.getId(), newState.getId());
        String description = "after-field-abstraction-of-smg-" + result.getId();
        SMGUtils.plotWhenConfigured(
            name, newState, description, logger, SMGExportLevel.EVERY, exportOptions);

        result = newState;
        logger.log(
            Level.ALL,
            "Precision adjustment on node ",
            node.getNodeNumber(),
            " with result state id: ",
            result.getId());
      }
    }

    if (allowsHeapAbstraction) {

      boolean heapAbstractionChange =
          newState.executeHeapAbstraction(pPrecision.getAbstractionBlocks(node));

      if (heapAbstractionChange) {
        String name = String.format("%03d-before-heap-abstraction", result.getId());
        String name2 = String.format("%03d-after-heap-abstraction", result.getId());
        String description = "before-heap-abstraction-of-smg-" + result.getId();
        String description2 = "after-heap-abstraction-of-smg-" + result.getId();
        SMGUtils.plotWhenConfigured(
            name, result, description, logger, SMGExportLevel.EVERY, exportOptions);
        SMGUtils.plotWhenConfigured(
            name2, newState, description2, logger, SMGExportLevel.EVERY, exportOptions);
        logger.log(
            Level.ALL,
            "Heap abstraction on node ",
            node.getNodeNumber(),
            " with state id: ",
            pState.getId());
        result = newState;
      }
    }

    statistics.totalAbstraction.stop();
    statistics.abstractions.inc();
    return Optional.of(PrecisionAdjustmentResult.create(result, pPrecision, Action.CONTINUE));
  }
}
