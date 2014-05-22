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
package org.sosy_lab.cpachecker.cpa.apron.refiner;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cpa.apron.ApronCPA;
import org.sosy_lab.cpachecker.cpa.apron.ApronPrecision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

import apron.ApronException;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Refiner implementation that delegates to {@link ApronInterpolationBasedRefiner},
 * and if this fails, optionally delegates also to {@link PredicateCPARefiner}.
 */
@Options(prefix="cpa.octagon.refiner")
public class ApronDelegatingRefiner extends AbstractARGBasedRefiner implements Statistics, StatisticsProvider {

  /**
   * refiner used for value-analysis interpolation refinement
   */
  private ApronInterpolationBasedRefiner interpolatingRefiner;

  /**
   * the hash code of the previous error path
   */
  private int previousErrorPathID = -1;

  /**
   * the flag to determine whether or not to check for repeated refinements
   */
  @Option(description="whether or not to check for repeated refinements, to then reset the refinement root")
  private boolean checkForRepeatedRefinements = true;

  @Option(description="Timelimit (in seconds) for the backup feasibility check with the octagon analysis."
      + " Zero means there is no timelimit.")
  private int timeForOctagonFeasibilityCheck = 0;

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
  private final ApronCPA octagonCPA;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  public static ApronDelegatingRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ApronDelegatingRefiner.class.getSimpleName() + " could not find the OctagonCPA");
    }

    ApronCPA octagonCPA = ((WrapperCPA)cpa).retrieveWrappedCpa(ApronCPA.class);
    if (octagonCPA == null) {
      throw new InvalidConfigurationException(ApronDelegatingRefiner.class.getSimpleName() + " needs an OctagonCPA");
    }

    ApronDelegatingRefiner refiner = initialiseValueAnalysisRefiner(cpa, octagonCPA);

    return refiner;
  }

  private static ApronDelegatingRefiner initialiseValueAnalysisRefiner(
      ConfigurableProgramAnalysis cpa, ApronCPA pOctagonCPA)
          throws CPAException, InvalidConfigurationException {
    LogManager logger                 = pOctagonCPA.getLogger();

    return new ApronDelegatingRefiner(
        logger,
        pOctagonCPA.getShutdownNotifier(),
        cpa,
        pOctagonCPA);
  }

  protected ApronDelegatingRefiner(
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ConfigurableProgramAnalysis pCpa,
      final ApronCPA pOctagonCPA) throws CPAException, InvalidConfigurationException {
    super(pCpa);
    pOctagonCPA.getConfiguration().inject(this);

    cfa                   = pOctagonCPA.getCFA();
    logger                = pLogger;
    shutdownNotifier      = pShutdownNotifier;
    octagonCPA            = pOctagonCPA;
    interpolatingRefiner  = new ApronInterpolationBasedRefiner(pOctagonCPA.getConfiguration(), pLogger, pShutdownNotifier, cfa);
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if path is infeasible, try to refine the precision
    if (!isPathFeasable(errorPath) && !existsExplicitOctagonRefinement) {
      if (performValueAnalysisRefinement(reached, errorPath)) {
        return CounterexampleInfo.spurious();
      }
    }

    // if the path is infeasible, try to refine the precision, this time
    // only with octagons, this is more precise than only using the value analysis
    // refinement
    ApronAnalysisFeasabilityChecker octChecker;
    try {
      octChecker = createOctagonFeasibilityChecker(errorPath);
    } catch (ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
    if (!octChecker.isFeasible()) {
      if (performOctagonAnalysisRefinement(reached, octChecker)) {
        existsExplicitOctagonRefinement = true;
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
   * @throws InvalidConfigurationException
   */
  private boolean performValueAnalysisRefinement(final ARGReachedSet reached, final ARGPath errorPath) throws CPAException, InterruptedException {
    numberOfValueAnalysisRefinements++;

    UnmodifiableReachedSet reachedSet = reached.asReachedSet();
    Precision precision               = reachedSet.getPrecision(reachedSet.getLastState());
    ApronPrecision octPrecision         = Precisions.extractPrecisionByType(precision, ApronPrecision.class);

    ArrayList<Precision> refinedPrecisions = new ArrayList<>(1);
    ArrayList<Class<? extends Precision>> newPrecisionTypes = new ArrayList<>(1);

    ApronPrecision refinedOctPrecision;
    Pair<ARGState, CFAEdge> refinementRoot;


    Multimap<CFANode, MemoryLocation> increment = interpolatingRefiner.determinePrecisionIncrement(errorPath);
    refinementRoot                              = interpolatingRefiner.determineRefinementRoot(errorPath, increment, false);

    // no increment - value-analysis refinement was not successful
    if(increment.isEmpty()) {
      return false;
    }

    // if two subsequent refinements are similar (based on some fancy heuristic), choose a different refinement root
    if(checkForRepeatedRefinements && isRepeatedRefinement(increment, refinementRoot)) {
      refinementRoot = interpolatingRefiner.determineRefinementRoot(errorPath, increment, true);
    }

    refinedOctPrecision  = new ApronPrecision(octPrecision, increment);
    refinedPrecisions.add(refinedOctPrecision);
    newPrecisionTypes.add(ApronPrecision.class);

    if (valueAnalysisRefinementWasSuccessful(errorPath, octPrecision.getValueAnalysisPrecision(), refinedOctPrecision.getValueAnalysisPrecision())) {
      numberOfSuccessfulValueAnalysisRefinements++;
      reached.removeSubtree(refinementRoot.getFirst(), refinedPrecisions, newPrecisionTypes);
      Set<String> strIncrement = Sets.difference(refinedOctPrecision.getTrackedVars(), octPrecision.getTrackedVars());
      if (strIncrement.isEmpty()) {
        logger.log(Level.INFO, "Refinement successful, additional variables were not found, but a new error path.");
      } else {
        logger.log(Level.INFO, "Refinement successful, precision incremented, following variables are now tracked additionally:\n" + strIncrement);
      }

      return true;
    }
    else {
      return false;
    }
  }

  private boolean performOctagonAnalysisRefinement(final ARGReachedSet reached, final ApronAnalysisFeasabilityChecker checker) {
    UnmodifiableReachedSet reachedSet = reached.asReachedSet();
    Precision precision               = reachedSet.getPrecision(reachedSet.getLastState());
    ApronPrecision octPrecision         = Precisions.extractPrecisionByType(precision, ApronPrecision.class);

    Set<String> increment = checker.getPrecisionIncrement(octPrecision);
    // no newly tracked variables, so the refinement was not successful
    if (increment.isEmpty()) {
    //  return false;
    }

    ArrayList<Precision> refinedPrecisions = new ArrayList<>(1);
    ArrayList<Class<? extends Precision>> newPrecisionTypes = new ArrayList<>(1);
    refinedPrecisions.add(new ApronPrecision(octPrecision, increment));
    newPrecisionTypes.add(ApronPrecision.class);

    reached.removeSubtree(((ARGState)reachedSet.getFirstState()).getChildren().iterator().next(), refinedPrecisions, newPrecisionTypes);
    logger.log(Level.INFO, "Refinement successful, precision incremented, following variables are now tracked additionally:\n" + increment);

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
  private boolean valueAnalysisRefinementWasSuccessful(ARGPath errorPath, ValueAnalysisPrecision valueAnalysisPrecision,
      ValueAnalysisPrecision refinedValueAnalysisPrecision) {
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

  /**
   * Creates a new OctagonAnalysisPathChecker, which checks the given path at full precision.
   * @throws ApronException
   */
  private ApronAnalysisFeasabilityChecker createOctagonFeasibilityChecker(ARGPath path) throws CPAException, ApronException {
    try {
      ApronAnalysisFeasabilityChecker checker;

      // no specific timelimit set for octagon feasibility check
      if (timeForOctagonFeasibilityCheck == 0) {
        checker = new ApronAnalysisFeasabilityChecker(cfa, logger, shutdownNotifier, path, octagonCPA);

      } else {
        ShutdownNotifier notifier = ShutdownNotifier.createWithParent(shutdownNotifier);
        FeasabilityCheckTimeLimit l = new FeasabilityCheckTimeLimit(timeForOctagonFeasibilityCheck*(long)1000000);
        ResourceLimitChecker limits = new ResourceLimitChecker(notifier, Lists.newArrayList((ResourceLimit)l));

        limits.start();
        checker = new ApronAnalysisFeasabilityChecker(cfa, logger, notifier, path, octagonCPA);
        limits.cancel();
      }

      return checker;
    } catch (InterruptedException | InvalidConfigurationException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }

  static class FeasabilityCheckTimeLimit implements ResourceLimit {
    private final long duration;
    private final long endTime;

    private FeasabilityCheckTimeLimit(long pDuration) {
      duration = pDuration;
      endTime = getCurrentValue() + pDuration;
    }

    public static FeasabilityCheckTimeLimit fromNowOn(long time, TimeUnit unit) {
      checkArgument(time > 0);
      long nanoDuration = TimeUnit.NANOSECONDS.convert(time, unit);
      return new FeasabilityCheckTimeLimit(nanoDuration);
    }

    @Override
    public long getCurrentValue() {
      return System.nanoTime();
    }

    @Override
    public boolean isExceeded(long pCurrentValue) {
      return pCurrentValue >= endTime;
    }

    @Override
    public long nanoSecondsToNextCheck(long pCurrentValue) {
      return endTime - pCurrentValue;
    }

    @Override
    public String getName() {
      return "feasbility check timelimit of " + TimeUnit.NANOSECONDS.toSeconds(duration) + "s";
    }
  }
}
