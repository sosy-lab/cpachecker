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
