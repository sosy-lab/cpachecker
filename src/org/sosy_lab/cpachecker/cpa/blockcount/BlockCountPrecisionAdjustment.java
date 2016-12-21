/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.blockcount;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.List;
import java.util.Optional;

public class BlockCountPrecisionAdjustment implements PrecisionAdjustment {

  private final int blockCountThreshold;

  private BlockCountPrecisionAdjustment(int pBlockCountThreshold) {
    Preconditions.checkArgument(
        pBlockCountThreshold >= 0,
        "Negative values are not allowed as a threshold for unrolling blocks. Use 0 to disable the threshold.");
    this.blockCountThreshold = pBlockCountThreshold;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {
    return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.CONTINUE));
  }

  @Override
  public Optional<? extends AbstractState> strengthen(
      AbstractState pState, Precision pPrecision, List<AbstractState> pOtherStates)
      throws CPAException, InterruptedException {
    AbstractState result = pState;
    if (FluentIterable.from(pOtherStates)
        .filter(PredicateAbstractState.class)
        .anyMatch(PredicateAbstractState.CONTAINS_ABSTRACTION_STATE)) {
      BlockCountState blockCountState = (BlockCountState) pState;
      BlockCountState withIncrement = blockCountState.incrementCount();
      if (blockCountThreshold <= 0 || withIncrement.getCount() < blockCountThreshold) {
        return Optional.of(withIncrement);
      }
      return Optional.of(withIncrement.stop());
    }
    return Optional.of(result);
  }

  public static PrecisionAdjustment forThreshold(int pBlockCountThreshold) {
    return new BlockCountPrecisionAdjustment(pBlockCountThreshold);
  }
}
