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

import java.io.PrintStream;
import java.util.Collection;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.Multimap;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisInterpolationBasedRefiner}.
 */
public class BddRefiner extends AbstractARGBasedRefiner implements Statistics, StatisticsProvider {

  /**
   * refiner used for value-analysis interpolation refinement
   */
  private final ValueAnalysisInterpolationBasedRefiner interpolatingRefiner;

  private final CFA cfa;

  private final LogManager logger;

  private int previousErrorPathId = -1;

  // statistics
  private int numberOfValueAnalysisRefinements           = 0;
  private int numberOfSuccessfulValueAnalysisRefinements = 0;

  public static BddRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(BddRefiner.class.getSimpleName() + " could not find the BDDCPA");
    }

    WrapperCPA wrapperCpa = ((WrapperCPA)cpa);

    BDDCPA bddCpa = wrapperCpa.retrieveWrappedCpa(BDDCPA.class);
    if (bddCpa == null) {
      throw new InvalidConfigurationException(BddRefiner.class.getSimpleName() + " needs a BDDCPA");
    }

    BddRefiner refiner = initialiseValueAnalysisRefiner(cpa, bddCpa);

    return refiner;
  }

  private static BddRefiner initialiseValueAnalysisRefiner(
      ConfigurableProgramAnalysis cpa, BDDCPA pBddCpa)
          throws CPAException, InvalidConfigurationException {
    Configuration config  = pBddCpa.getConfiguration();
    LogManager logger     = pBddCpa.getLogger();

    return new BddRefiner(
        config,
        logger,
        pBddCpa.getShutdownNotifier(),
        cpa,
        pBddCpa.getCFA());
  }

  protected BddRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ConfigurableProgramAnalysis pCpa,
      final CFA pCfa) throws CPAException, InvalidConfigurationException {
    super(pCpa);

    interpolatingRefiner  = new ValueAnalysisInterpolationBasedRefiner(pConfig, pLogger, pShutdownNotifier, pCfa);
    cfa                   = pCfa;
    logger                = pLogger;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if path is infeasible, try to refine the precision
    if (!isPathFeasable(errorPath)) {
      if (performValueAnalysisRefinement(reached, errorPath)) {
        return CounterexampleInfo.spurious();
      }
    }

    return CounterexampleInfo.feasible(errorPath, null);
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
    if(currentErrorPathId == previousErrorPathId) {
      throw new RefinementFailedException(Reason.RepeatedCounterexample, errorPath);
    }

    previousErrorPathId = currentErrorPathId;

    UnmodifiableReachedSet reachedSet = reached.asReachedSet();
    Precision precision               = reachedSet.getPrecision(reachedSet.getLastState());
    BDDPrecision bddPrecision         = Precisions.extractPrecisionByType(precision, BDDPrecision.class);

    Multimap<CFANode, MemoryLocation> increment = interpolatingRefiner.determinePrecisionIncrement(errorPath);
    Pair<ARGState, CFAEdge> refinementRoot = interpolatingRefiner.determineRefinementRoot(errorPath, increment, false);

    // no increment - value-analysis refinement was not successful
    if(increment.isEmpty()) {
      return false;
    }

    BDDPrecision refinedBDDPrecision = new BDDPrecision(bddPrecision, increment);

    numberOfSuccessfulValueAnalysisRefinements++;
    reached.removeSubtree(refinementRoot.getFirst(), refinedBDDPrecision, BDDPrecision.class);
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
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
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
  boolean isPathFeasable(ARGPath path) throws CPAException {
    try {
      // create a new ValueAnalysisPathChecker, which does check the given path at full precision
      ValueAnalysisFeasibilityChecker checker = new ValueAnalysisFeasibilityChecker(logger, cfa);

      return checker.isFeasible(path);
    }
    catch (InterruptedException | InvalidConfigurationException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }
}
