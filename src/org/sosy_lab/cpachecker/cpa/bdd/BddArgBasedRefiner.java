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
package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.collect.Multimap;
import java.io.PrintStream;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisPathInterpolator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisPathInterpolator}.
 */
class BddArgBasedRefiner implements ARGBasedRefiner, Statistics, StatisticsProvider {

  /**
   * refiner used for value-analysis interpolation refinement
   */
  private final ValueAnalysisPathInterpolator interpolatingRefiner;

  private final FeasibilityChecker<ValueAnalysisState> checker;

  private int previousErrorPathId = -1;

  // statistics
  private int numberOfValueAnalysisRefinements           = 0;
  private int numberOfSuccessfulValueAnalysisRefinements = 0;

  BddArgBasedRefiner(
      final FeasibilityChecker<ValueAnalysisState> pFeasibilityChecker,
      final ValueAnalysisPathInterpolator pPathInterpolator) {
    checker = pFeasibilityChecker;
    interpolatingRefiner = pPathInterpolator;
  }

  @Override
  public CounterexampleInfo performRefinementForPath(
      final ARGReachedSet reached, final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    // if path is infeasible, try to refine the precision
    if (!isPathFeasable(pErrorPath)) {
      if (performValueAnalysisRefinement(reached, pErrorPath)) {
        return CounterexampleInfo.spurious();
      }
    }

    // we use the imprecise version of the CounterexampleInfo, due to the possible
    // merges which are done in the BDD Analysis
    return CounterexampleInfo.feasibleImprecise(pErrorPath);
  }

  /**
   * This method performs an value-analysis refinement.
   *
   * @param reached the current reached set
   * @param errorPath the current error path
   * @returns true, if the value-analysis refinement was successful, else false
   * @throws CPAException when value-analysis interpolation fails
   */
  private boolean performValueAnalysisRefinement(final ARGReachedSet reached, final ARGPath errorPath) throws CPAException, InterruptedException {
    numberOfValueAnalysisRefinements++;

    int currentErrorPathId = errorPath.toString().hashCode();

    // same error path as in last iteration -> no progress
    if (currentErrorPathId == previousErrorPathId) {
      throw new RefinementFailedException(Reason.RepeatedCounterexample, errorPath);
    }

    previousErrorPathId = currentErrorPathId;

    UnmodifiableReachedSet reachedSet = reached.asReachedSet();
    Precision precision               = reachedSet.getPrecision(reachedSet.getLastState());
    VariableTrackingPrecision bddPrecision = (VariableTrackingPrecision) Precisions.asIterable(precision)
                                                                                   .filter(VariableTrackingPrecision
                                                                                           .isMatchingCPAClass(BDDCPA.class))
                                                                                   .get(0);


    Multimap<CFANode, MemoryLocation> increment = interpolatingRefiner.determinePrecisionIncrement(errorPath);
    Pair<ARGState, CFAEdge> refinementRoot = interpolatingRefiner.determineRefinementRoot(errorPath, increment, false);

    // no increment - value-analysis refinement was not successful
    if (increment.isEmpty()) {
      return false;
    }

    VariableTrackingPrecision refinedBDDPrecision = bddPrecision.withIncrement(increment);

    numberOfSuccessfulValueAnalysisRefinements++;
    reached.removeSubtree(refinementRoot.getFirst(), refinedBDDPrecision, VariableTrackingPrecision.isMatchingCPAClass(BDDCPA.class));
    return true;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
    pStatsCollection.add(interpolatingRefiner);
  }

  @Override
  public String getName() {
    return "BddDelegatingRefiner";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    out.println("  number of value analysis refinements:                " + numberOfValueAnalysisRefinements);
    out.println("  number of successful valueAnalysis refinements:      " + numberOfSuccessfulValueAnalysisRefinements);
  }

  /**
   * This method checks if the given path is feasible, when doing a full-precision check.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException if the path check gets interrupted
   */
  boolean isPathFeasable(ARGPath path) throws CPAException, InterruptedException {
      return checker.isFeasible(path);
  }
}
