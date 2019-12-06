/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
    this.wrappedPrecisionAdjustment = pWrappedPrecisionAdjustment;
    this.data = pData;
    bamPccManager = pBamPccManager;
    this.logger = pLogger;
    this.blockPartitioning = pBlockPartitioning;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {

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

    Optional<PrecisionAdjustmentResult> result = wrappedPrecisionAdjustment.prec(
        pElement,
        validPrecision,
        pElements,
        projection,
        fullState);

    if (!result.isPresent()) {
      return result;
    }

    if (bamPccManager != null && bamPccManager.isPCCEnabled()) {
      result = result
          .map(
              t -> t.withAbstractState(
                  bamPccManager.attachAdditionalInfoToCallNode(t.abstractState())
              )
          );
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
