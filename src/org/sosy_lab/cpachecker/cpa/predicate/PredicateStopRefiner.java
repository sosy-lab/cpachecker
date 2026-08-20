// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;

/**
 * A refiner that's sole job is to stop the refinement and end the verification run. It is not
 * intended to perform any refinement execution. Intended for use with the {@link
 * PredicateDelegatingRefiner} to stop refinement when all its heuristics have indicated likely
 * divergence.
 */
public class PredicateStopRefiner implements Refiner {
  public PredicateStopRefiner() {}

  /**
   * Performs no refinement and returns false. The method is a placeholder to satisfy requirements
   * of Refiner interface.
   *
   * @param pReached the current reached set (unused)
   * @return {@code true} as no refinement was performed, but also no error was found
   */
  @Override
  public boolean performRefinement(ReachedSet pReached) throws RefinementFailedException {
    throw new RefinementFailedException(Reason.RepeatedCounterexample, null);
  }
}
