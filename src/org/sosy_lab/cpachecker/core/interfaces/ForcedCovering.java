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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for implementations of forced coverings
 * (strengthening a new abstract state such that it is covered by another state
 * from the reached set).
 *
 * Implementations need to have exactly one public constructor or a static method named "create"
 * which may take a {@link Configuration}, a {@link LogManager}, and a
 * {@link ConfigurableProgramAnalysis}, and throw at most an
 * {@link InvalidConfigurationException}.
 */
public interface ForcedCovering {

  /**
   * Try to cover the abstract state by strengthening it (and possibly its parents).
   *
   * This method should not change the reached set, except by re-adding some
   * state to the waitlist. It is necessary to re-add states to the waitlist,
   * which are covered by strengthened state, and this method is responsible
   * for this!
   *
   * The methods returns a boolean indicating success in covering the state
   * or not. This means, if this method returns true, the stop operator called
   * with the same arguments after this method returned also needs to return true.
   *
   * @param state The state which hopefully is covered afterwards.
   * @param precision The precision for the state.
   * @param reached The current reached set.
   * @return Whether forced covering was successful.
   */
  boolean tryForcedCovering(AbstractState state, Precision precision, ReachedSet reached) throws CPAException, InterruptedException;

  interface Factory {
    ForcedCovering create(Configuration config, LogManager logger, ConfigurableProgramAnalysis cpa)
        throws InvalidConfigurationException;
  }
}