/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Depending on the flag {@link #breakOnMissingBlock}, this class causes the {@link CPAAlgorithm} to
 * terminate when finding a missing block abstraction, or just compute more states.
 */
class BAMPrecisionAdjustmentWithBreakOnMissingBlock extends BAMPrecisionAdjustment {

  private final boolean breakOnMissingBlock;

  public BAMPrecisionAdjustmentWithBreakOnMissingBlock(
      PrecisionAdjustment pWrappedPrecisionAdjustment,
      BAMDataManager pData,
      LogManager pLogger,
      BlockPartitioning pBlockPartitioning,
      boolean pBreakForMissingBlock) {
    super(pWrappedPrecisionAdjustment, pData, null, pLogger, pBlockPartitioning);
    breakOnMissingBlock = pBreakForMissingBlock;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    if (pElement instanceof MissingBlockAbstractionState) {
      final Action action = breakOnMissingBlock ? Action.BREAK : Action.CONTINUE;
      return Optional.of(PrecisionAdjustmentResult.create(pElement, pPrecision, action));
    }

    return super.prec(pElement, pPrecision, pElements, projection, fullState);
  }
}
