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
package org.sosy_lab.cpachecker.cpa.arg;

import java.util.List;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Alternative to {@link Refiner} for refiners that are based on using an ARG.
 * The refiner is supplied with the error path through the ARG on refinement.
 *
 * Use {@link AbstractARGBasedRefiner#forARGBasedRefiner(ARGBasedRefiner, org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis, org.sosy_lab.common.configuration.Configuration)}
 * to create a {@link Refiner} instance from an instance of this interface.
 */
public interface ARGBasedRefiner {

  /**
   * Perform refinement.
   * @param pReached the reached set
   * @param pPath the potential error path
   * @return Information about the counterexample.
   */
  CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException;

  /**
   * Perform refinement for the given target paths.
   * The default implementation iterates
   * over the target paths and considers them individually,
   * but global refinement procedures exist that may differ from this behavior.
   *
   * @param pReached the reached set
   * @param pPaths the list of potential target paths
   * @return a found feasible counterexample that corresponds to one of the given
   *    target paths, or a spurious counterexample if all target paths were deemed infeasible
   *
   * @see org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisGlobalRefiner
   * @see org.sosy_lab.cpachecker.cpa.predicate.PredicateGlobalRefiner
   */
  default CounterexampleInfo performRefinementForPaths(ARGReachedSet pReached, List<ARGPath> pPaths)
      throws CPAException, InterruptedException {
    CounterexampleInfo cex;
    for (ARGPath path : pPaths) {
      assert path != null : "Counterexample should come from a correct path.";
      // through the use of &&, refinement is only performed if all previous error paths
      // were infeasible
      cex = performRefinementForPath(pReached, path);
      if (!cex.isSpurious()) {
        return cex;
      }
    }

    // If no cex was deemed feasible in loop above, return spurious cex.
    return CounterexampleInfo.spurious();
  }
}
