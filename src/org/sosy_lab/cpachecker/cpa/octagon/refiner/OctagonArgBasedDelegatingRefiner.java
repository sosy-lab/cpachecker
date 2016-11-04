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
package org.sosy_lab.cpachecker.cpa.octagon.refiner;

import com.google.common.collect.Multimap;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.octagon.OctagonCPA;
import org.sosy_lab.cpachecker.cpa.octagon.OctagonState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisPathInterpolator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisPathInterpolator},
 * and if this fails, optionally delegates also to {@link PredicateCPARefiner}.
 */
@Options(prefix="cpa.octagon.refiner")
class OctagonArgBasedDelegatingRefiner implements ARGBasedRefiner, Statistics, StatisticsProvider {

  /**
   * refiner used for value-analysis interpolation refinement
   */
  private final ValueAnalysisPathInterpolator interpolatingRefiner;

  /**
   * the hash code of the previous error path
   */
  private int previousErrorPathID = -1;

  /**
   * the flag to determine whether or not to check for repeated refinements
   */
  @Option(secure=true, description="whether or not to check for repeated refinements, to then reset the refinement root")
  private boolean checkForRepeatedRefinements = true;

  @Option(secure=true, description="Timelimit for the backup feasibility check with the octagon analysis."
      + "(use seconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
                  defaultUserUnit=TimeUnit.SECONDS,
                  min=0)
  private TimeSpan timeForOctagonFeasibilityCheck = TimeSpan.ofNanos(0);

  // statistics
  private int numberOfValueAnalysisRefinements           = 0;
  private int numberOfSuccessfulValueAnalysisRefinements = 0;

  /**
   * the identifier which is used to identify repeated refinements
   */
  private int previousRefinementId = 0;

  /** if this variable is toggled, only octagon refinements will be done as
   * value analysis refinements will make no sense any more because they are too
   * imprecise
   */
  private boolean existsExplicitOctagonRefinement = false;

  private final CFA cfa;

  private final OctagonManager octagonManager;
  private final TransferRelation octagonTransfer;
  private final FeasibilityChecker<ValueAnalysisState> valueChecker;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;

  OctagonArgBasedDelegatingRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa,
      final OctagonManager pOctagonManager,
      final TransferRelation pOctagonTransfer,
      final FeasibilityChecker<ValueAnalysisState> pValueAnalysisFeasibilityChecker,
      final ValueAnalysisPathInterpolator pValueAnalysisPathInterpolator)
      throws InvalidConfigurationException {

    pConfig.inject(this);
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;

    octagonManager = pOctagonManager;
    octagonTransfer = pOctagonTransfer;
    valueChecker = pValueAnalysisFeasibilityChecker;
    interpolatingRefiner = pValueAnalysisPathInterpolator;
  }

  @Override
  public CounterexampleInfo performRefinementForPath(
      final ARGReachedSet reached, final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    // if path is infeasible, try to refine the precision
    if (!isPathFeasable(pErrorPath) && !existsExplicitOctagonRefinement) {
      if (performValueAnalysisRefinement(reached, pErrorPath)) {
        return CounterexampleInfo.spurious();
      }
    }

    // if the path is infeasible, try to refine the precision, this time
    // only with octagons, this is more precise than only using the value analysis
    // refinement
    OctagonAnalysisFeasabilityChecker octChecker = createOctagonFeasibilityChecker(pErrorPath);

    if (!octChecker.isFeasible()) {
      if (performOctagonAnalysisRefinement(reached, octChecker)) {
        existsExplicitOctagonRefinement = true;
        return CounterexampleInfo.spurious();
      }
    }

    // we use the imprecise version of the CounterexampleInfo, due to the possible
    // merges which are done in the OctagonCPA
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

    UnmodifiableReachedSet reachedSet = reached.asReachedSet();
    Precision precision               = reachedSet.getPrecision(reachedSet.getLastState());
    VariableTrackingPrecision octPrecision         = (VariableTrackingPrecision) Precisions.asIterable(precision).filter(VariableTrackingPrecision.isMatchingCPAClass(OctagonCPA.class)).get(0);

    VariableTrackingPrecision refinedOctPrecision;
    Pair<ARGState, CFAEdge> refinementRoot;


    Multimap<CFANode, MemoryLocation> increment = interpolatingRefiner.determinePrecisionIncrement(errorPath);
    refinementRoot                              = interpolatingRefiner.determineRefinementRoot(errorPath, increment, false);

    // no increment - value-analysis refinement was not successful
    if (increment.isEmpty()) {
      return false;
    }

    // if two subsequent refinements are similar (based on some fancy heuristic), choose a different refinement root
    if (checkForRepeatedRefinements && isRepeatedRefinement(increment, refinementRoot)) {
      refinementRoot = interpolatingRefiner.determineRefinementRoot(errorPath, increment, true);
    }

    refinedOctPrecision  = octPrecision.withIncrement(increment);

    if (valueAnalysisRefinementWasSuccessful(errorPath, octPrecision, refinedOctPrecision)) {
      numberOfSuccessfulValueAnalysisRefinements++;
      reached.removeSubtree(refinementRoot.getFirst(),
                            refinedOctPrecision,
                            VariableTrackingPrecision.isMatchingCPAClass(OctagonCPA.class));
      return true;
    } else {
      return false;
    }
  }

  private boolean performOctagonAnalysisRefinement(
      final ARGReachedSet reached, final OctagonAnalysisFeasabilityChecker checker)
      throws InterruptedException {
    UnmodifiableReachedSet reachedSet = reached.asReachedSet();
    Precision precision               = reachedSet.getPrecision(reachedSet.getLastState());
    VariableTrackingPrecision octPrecision         = (VariableTrackingPrecision) Precisions.asIterable(precision).filter(VariableTrackingPrecision.isMatchingCPAClass(OctagonCPA.class)).get(0);

    Multimap<CFANode, MemoryLocation> increment = checker.getPrecisionIncrement();
    // no newly tracked variables, so the refinement was not successful, TODO why is this code commented out?
    if (increment.isEmpty()) {
    //  return false;
    }

    reached.removeSubtree(((ARGState)reachedSet.getFirstState()).getChildren().iterator().next(),
                           octPrecision.withIncrement(increment),
                          VariableTrackingPrecision.isMatchingCPAClass(OctagonCPA.class));
    logger.log(Level.INFO, "Refinement successful, precision incremented, following variables are now tracked additionally:\n" + new TreeSet<>(increment.values()));

    return true;
  }

  /**
   * The not-so-fancy heuristic to determine if two subsequent refinements are similar
   *
   * @param increment the precision increment
   * @param refinementRoot the current refinement root
   * @return true, if the current refinement is found to be similar to the previous one, else false
   */
  private boolean isRepeatedRefinement(Multimap<CFANode, MemoryLocation> increment, Pair<ARGState, CFAEdge> refinementRoot) {
    int currentRefinementId = refinementRoot.getSecond().getSuccessor().getNodeNumber();
    boolean result          = (previousRefinementId == currentRefinementId);
    previousRefinementId    = currentRefinementId;

    return result;
  }

  /**
   * This helper method checks if the refinement was successful, i.e.,
   * that either the counterexample is not a repeated counterexample, or that the precision did grow.
   *
   * Repeated counterexamples might occur when combining the analysis with thresholding,
   * or when ignoring variable classes, i.e. when combined with BDD analysis (i.e. cpa.value.precision.ignoreBoolean).
   *
   * @param errorPath the current error path
   * @param valueAnalysisPrecision the previous precision
   * @param refinedValueAnalysisPrecision the refined precision
   */
  private boolean valueAnalysisRefinementWasSuccessful(ARGPath errorPath, VariableTrackingPrecision valueAnalysisPrecision,
      VariableTrackingPrecision refinedValueAnalysisPrecision) {
    // new error path or precision refined -> success
    boolean success = (errorPath.toString().hashCode() != previousErrorPathID)
        || (refinedValueAnalysisPrecision.getSize() > valueAnalysisPrecision.getSize());

    previousErrorPathID = errorPath.toString().hashCode();

    return success;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
    pStatsCollection.add(interpolatingRefiner);
  }

  @Override
  public String getName() {
    return "OctagonAnalysisDelegatingRefiner";
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
    return valueChecker.isFeasible(path);
  }

  /**
   * Creates a new OctagonAnalysisPathChecker, which checks the given path at full precision.
   */
  private OctagonAnalysisFeasabilityChecker createOctagonFeasibilityChecker(ARGPath path) throws CPAException, InterruptedException {
    try {
      OctagonAnalysisFeasabilityChecker checker;

      // no specific timelimit set for octagon feasibility check
      if (timeForOctagonFeasibilityCheck.isEmpty()) {
        checker =
            new OctagonAnalysisFeasabilityChecker(
                config,
                shutdownNotifier,
                path,
                OctagonCPA.class,
                cfa.getVarClassification(),
                octagonTransfer,
                new OctagonState(logger, octagonManager));

      } else {
        ShutdownManager shutdown = ShutdownManager.createWithParent(shutdownNotifier);
        WalltimeLimit l = WalltimeLimit.fromNowOn(timeForOctagonFeasibilityCheck);
        ResourceLimitChecker limits = new ResourceLimitChecker(shutdown, Collections.<ResourceLimit>singletonList(l));

        limits.start();
        checker =
            new OctagonAnalysisFeasabilityChecker(
                config,
                shutdown.getNotifier(),
                path,
                OctagonCPA.class,
                cfa.getVarClassification(),
                octagonTransfer,
                new OctagonState(logger, octagonManager));
        limits.cancel();
      }

      return checker;
    } catch (InvalidConfigurationException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }
}
