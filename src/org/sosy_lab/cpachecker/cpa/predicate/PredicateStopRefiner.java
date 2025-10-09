// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * A refiner that's sole job is to stop the refinement and end the verification run. Intended for
 * use with the {@link PredicateDelegatingRefiner} to stop refinement when all its heuristics have
 * indicated likely divergence.
 */
public class PredicateStopRefiner implements ARGBasedRefiner {
  public PredicateStopRefiner() {}

  @Override
  public CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {
    return CounterexampleInfo.giveUp(pPath);
  }

  @Override
  public boolean shouldTerminateRefinement() {
    return true;
  }
}
