// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.Function;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BAMPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecisionAdjustment;
  @Nullable private final BAMPCCManager bamPccManager;
  private final BAMDataManager data;
  private final LogManager logger;
  private final BlockPartitioning blockPartitioning;

  public BAMPrecisionAdjustment(
      PrecisionAdjustment pWrappedPrecisionAdjustment,
      BAMDataManager pData,
      @Nullable BAMPCCManager pBamPccManager,
      LogManager pLogger,
      BlockPartitioning pBlockPartitioning) {
    wrappedPrecisionAdjustment = pWrappedPrecisionAdjustment;
    data = pData;
    bamPccManager = pBamPccManager;
    logger = pLogger;
    blockPartitioning = pBlockPartitioning;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    // precision might be outdated, if comes from a block-start and the inner part was refined.
    // so lets use the (expanded) inner precision.
    Precision validPrecision = pPrecision;
    if (AbstractStates.isTargetState(pElement)
        || blockPartitioning.isReturnNode(AbstractStates.extractLocation(pElement))) {
      Precision expandedPrecision = data.getExpandedPrecisionForState(pElement);
      if (expandedPrecision != null) {
        validPrecision = expandedPrecision;
      }
    }

    Optional<PrecisionAdjustmentResult> result =
        wrappedPrecisionAdjustment.prec(pElement, validPrecision, pElements, projection, fullState);

    if (!result.isPresent()) {
      return result;
    }

    if (bamPccManager != null && bamPccManager.isPCCEnabled()) {
      result =
          result.map(
              t ->
                  t.withAbstractState(
                      bamPccManager.attachAdditionalInfoToCallNode(t.abstractState())));
    }

    AbstractState newState = result.orElseThrow().abstractState();
    if (pElement != newState) {
      logger.log(Level.ALL, "before PREC:", pElement);
      logger.log(Level.ALL, "after PREC:", newState);
      data.replaceStateInCaches(pElement, newState, false);
    }

    return result;
  }
}
