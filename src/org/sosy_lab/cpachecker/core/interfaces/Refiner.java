// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Strategy for refinement of ARG used by {@link CEGARAlgorithm}.
 *
 * <p>Implementations need to have exactly one public constructor or a static method named "create"
 * which may take a {@link ConfigurableProgramAnalysis}, and throw at most a {@link
 * InvalidConfigurationException} and a {@link CPAException}.
 */
public interface Refiner {

  /**
   * Perform refinement, if possible.
   *
   * @param pReached The reached set.
   * @return Whether the refinement was successful.
   * @throws CPAException If an error occurred during refinement.
   */
  boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException;

  interface Factory {
    Refiner create(
        ConfigurableProgramAnalysis cpa, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException;
  }
}
