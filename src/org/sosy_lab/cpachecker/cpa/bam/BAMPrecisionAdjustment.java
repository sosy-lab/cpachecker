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
import com.google.common.base.Optional;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.logging.Level;

public class BAMPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecisionAdjustment;
  private final BAMTransferRelation trans;
  private final BAMDataManager data;
  private final LogManager logger;

  public BAMPrecisionAdjustment(PrecisionAdjustment pWrappedPrecisionAdjustment,
      BAMDataManager pData, BAMTransferRelation pTransfer, LogManager pLogger) {
    this.wrappedPrecisionAdjustment = pWrappedPrecisionAdjustment;
    this.data = pData;
    this.trans = pTransfer;
    this.logger = pLogger;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements, Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {
    if (trans.breakAnalysis) {
      return Optional.of(
          PrecisionAdjustmentResult.create(pElement, pPrecision, Action.BREAK));
    }

    // precision might be outdated, if comes from a block-start and the inner part was refined.
    // so lets use the (expanded) inner precision.
    final Precision validPrecision;
    if (data.expandedStateToExpandedPrecision.containsKey(pElement)) {
      assert AbstractStates.isTargetState(pElement)
          || trans.getBlockPartitioning().isReturnNode(AbstractStates.extractLocation(pElement));
      validPrecision = data.expandedStateToExpandedPrecision.get(pElement);
    } else {
      validPrecision = pPrecision;
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

    PrecisionAdjustmentResult updatedResult = result.get().withAbstractState(
        trans.attachAdditionalInfoToCallNode(result.get().abstractState()));

    if (pElement != updatedResult.abstractState()) {
      logger.log(Level.ALL, "before PREC:", pElement);
      logger.log(Level.ALL, "after PREC:", updatedResult.abstractState());
      data.replaceStateInCaches(pElement, updatedResult.abstractState(), false);
    }

    return Optional.of(updatedResult);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> postAdjustmentStrengthen(
      AbstractState result,
      Precision precision,
      Iterable<AbstractState> otherStates,
      Iterable<Precision> otherPrecisions,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState resultFullState) throws CPAException, InterruptedException {
    return Optional.of(PrecisionAdjustmentResult.create(result, precision, Action.CONTINUE));
  }
}
