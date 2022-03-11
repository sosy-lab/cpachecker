// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for implementations of forced coverings (strengthening a new abstract state such that
 * it is covered by another state from the reached set).
 *
 * <p>Implementations need to have exactly one public constructor or a static method named "create"
 * which may take a {@link Configuration}, a {@link LogManager}, and a {@link
 * ConfigurableProgramAnalysis}, and throw at most an {@link InvalidConfigurationException}.
 */
public interface ForcedCovering {

  /**
   * Try to cover the abstract state by strengthening it (and possibly its parents).
   *
   * <p>This method should not change the reached set, except by re-adding some state to the
   * waitlist. It is necessary to re-add states to the waitlist, which are covered by strengthened
   * state, and this method is responsible for this!
   *
   * <p>The methods returns a boolean indicating success in covering the state or not. This means,
   * if this method returns true, the stop operator called with the same arguments after this method
   * returned also needs to return true.
   *
   * @param state The state which hopefully is covered afterwards.
   * @param precision The precision for the state.
   * @param reached The current reached set.
   * @return Whether forced covering was successful.
   */
  boolean tryForcedCovering(AbstractState state, Precision precision, ReachedSet reached)
      throws CPAException, InterruptedException;

  interface Factory {
    ForcedCovering create(Configuration config, LogManager logger, ConfigurableProgramAnalysis cpa)
        throws InvalidConfigurationException;
  }
}
