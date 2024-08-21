// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/** Interface for the precision adjustment operator. */
public interface PrecisionAdjustment {

  /**
   * This method may adjust the current abstractState and precision using information from the
   * current set of reached states.
   *
   * <p>If this method doesn't change anything, it is strongly recommended to return the identical
   * objects for abstractState and precision. This makes it easier for wrapper CPAs.
   *
   * @param state The current abstract state for this CPA.
   * @param precision The current precision for this CPA.
   * @param states The current reached set with ALL abstract states.
   * @param stateProjection Projection function from any state within reached set to a state
   *     belonging to this CPA.
   * @param fullState The current abstract state, but for all CPAs (This can be used to access
   *     information stored in abstract states of other CPAs such as the current CFA location. Use
   *     methods from {@link AbstractStates} to access the individual states.).
   * @return The new abstract state, new precision and the action flag encapsulated in a {@link
   *     PrecisionAdjustmentResult} instance OR Optional.empty() if the newly produced abstract
   *     states corresponds to BOTTOM.
   */
  Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState)
      throws CPAException, InterruptedException;

  /**
   * Second strengthen phase which runs after precision adjustment was finished. This method is only
   * called by {@link org.sosy_lab.cpachecker.cpa.composite.CompositeCPA}.
   *
   * @param pState Input state for this CPA, after the initial run of precision adjustment.
   * @param pPrecision Associated precision.
   * @param otherStates Sibling states, as given by {@link
   *     org.sosy_lab.cpachecker.cpa.composite.CompositeCPA}.
   * @return Result of the strengthening operation.
   * @throws CPAException By the CPA code, when errors happen.
   * @throws InterruptedException On interrupt.
   */
  default Optional<? extends AbstractState> strengthen(
      AbstractState pState, Precision pPrecision, Iterable<AbstractState> otherStates)
      throws CPAException, InterruptedException {
    return Optional.of(pState);
  }
}
