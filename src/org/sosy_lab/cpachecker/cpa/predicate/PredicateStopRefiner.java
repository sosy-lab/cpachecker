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
 * A refiner that's only job is to stop the current refinement. Intended for use with the
 * DelegatingRefiner as the refiner when all other refiner-heuristic pairs failed.
 */
public class PredicateStopRefiner implements ARGBasedRefiner {

  public PredicateStopRefiner() {}

  @Override
  public CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {
    return CounterexampleInfo.spurious();
  }

  @Override
  public boolean continueRefinement() {
    return false;
  }
}
