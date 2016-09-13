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
package org.sosy_lab.cpachecker.core.interfaces;


import com.google.common.base.Function;

import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.List;
import java.util.Optional;

/**
 * Interface for the precision adjustment operator.
 */
public interface PrecisionAdjustment {

  /**
   * This method may adjust the current abstractState and precision using information
   * from the current set of reached states.
   *
   * If this method doesn't change anything, it is strongly recommended to return
   * the identical objects for abstractState and precision. This makes it easier for
   * wrapper CPAs.
   * @param state The current abstract state for this CPA.
   * @param precision The current precision for this CPA.
   * @param states The current reached set with ALL abstract states.
   * @param stateProjection Projection function from any state within reached
   * set to a state belonging to this CPA.
   * @param fullState The current abstract state, but for all CPAs
   * (This can be used to access information stored in abstract states of other CPAs
   * such as the current CFA location. Use methods from {@link AbstractStates}
   * to access the individual states.).
   *
   * @return The new abstract state, new precision and the action flag
   * encapsulated in a {@link PrecisionAdjustmentResult} instance OR
   * Optional.empty() if the newly produced abstract states corresponds
   * to BOTTOM.
   */
  Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState
  ) throws CPAException, InterruptedException;


  /**
   * Second strengthen phase which runs after precision adjustment was finished.
   * This method is only called by {@link org.sosy_lab.cpachecker.cpa.composite.CompositeCPA}.
   *
   * @param pState Input state for this CPA, after the initial run of precision adjustment.
   * @param pPrecision Associated precision.
   * @param otherStates Sibling states, as given by
   *  {@link org.sosy_lab.cpachecker.cpa.composite.CompositeCPA}.
   *
   * @return Result of the strengthening operation.
   * @throws CPAException By the CPA code, when errors happen.
   * @throws InterruptedException On interrupt.
   */
  default Optional<? extends AbstractState> strengthen(
      AbstractState pState, Precision pPrecision, List<AbstractState> otherStates)
      throws CPAException, InterruptedException {
    return Optional.of(pState);
  }
}
