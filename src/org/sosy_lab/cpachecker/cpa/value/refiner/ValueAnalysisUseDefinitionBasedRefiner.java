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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.AssumptionUseDefinitionCollector;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisInterpolationBasedRefiner},
 * and if this fails, optionally delegates also to {@link PredicatingExplicitRefiner}.
 */
@Options(prefix="cpa.explicit.refiner")
public class ValueAnalysisUseDefinitionBasedRefiner extends AbstractARGBasedRefiner implements Statistics, StatisticsProvider {
  // statistics
  private int numberOfExplicitRefinements           = 0;
  private int numberOfSuccessfulExplicitRefinements = 0;

  private final CFA cfa;

  private final LogManager logger;

  public static ValueAnalysisUseDefinitionBasedRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ValueAnalysisUseDefinitionBasedRefiner.class.getSimpleName() + " could not find the ExplicitCPA");
    }

    ValueAnalysisCPA explicitCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(ValueAnalysisCPA.class);
    if (explicitCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisUseDefinitionBasedRefiner.class.getSimpleName() + " needs a ExplicitCPA");
    }

    ValueAnalysisUseDefinitionBasedRefiner refiner = initialiseRefiner(cpa, explicitCpa);
    explicitCpa.getStats().addRefiner(refiner);

    return refiner;
  }

  private static ValueAnalysisUseDefinitionBasedRefiner initialiseRefiner(
      ConfigurableProgramAnalysis cpa, ValueAnalysisCPA explicitCpa)
          throws CPAException, InvalidConfigurationException {
    Configuration config              = explicitCpa.getConfiguration();
    LogManager logger                 = explicitCpa.getLogger();

    return new ValueAnalysisUseDefinitionBasedRefiner(
        config,
        logger,
        explicitCpa.getShutdownNotifier(),
        cpa,
        explicitCpa.getStaticRefiner(),
        explicitCpa.getCFA());
  }

  protected ValueAnalysisUseDefinitionBasedRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ConfigurableProgramAnalysis pCpa,
      ValueAnalysisStaticRefiner pExplicitStaticRefiner,
      final CFA pCfa) throws CPAException, InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);

    cfa                   = pCfa;
    logger                = pLogger;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if path is infeasible, try to refine the precision
    if (isPathFeasable(errorPath)) {
      return CounterexampleInfo.feasible(errorPath, null);
    }

    else if (performExplicitRefinement(reached, errorPath)) {
      return CounterexampleInfo.spurious();
    }

    throw new RefinementFailedException(Reason.RepeatedCounterexample, errorPath);
  }

  /**
   * This method performs an explicit refinement.
   *
   * @param reached the current reached set
   * @param errorPath the current error path
   * @returns true, if the explicit refinement was successful, else false
   * @throws CPAException when explicit interpolation fails
   */
  private boolean performExplicitRefinement(final ARGReachedSet reached, final ARGPath errorPath) throws CPAException, InterruptedException {
    numberOfExplicitRefinements++;

    UnmodifiableReachedSet reachedSet   = reached.asReachedSet();
    Precision precision                 = reachedSet.getPrecision(reachedSet.getLastState());
    ValueAnalysisPrecision explicitPrecision = Precisions.extractPrecisionByType(precision, ValueAnalysisPrecision.class);

    List<CFAEdge> cfaTrace = Lists.newArrayList();
    for(Pair<ARGState, CFAEdge> elem : errorPath) {
      cfaTrace.add(elem.getSecond());
    }

    Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();
    for(String var : new AssumptionUseDefinitionCollector().obtainUseDefInformation(cfaTrace)) {
      String[] s = var.split("::");
     
      // just add to BOGUS LOCATION
      increment.put(cfaTrace.get(0).getSuccessor(), (s.length == 1)
                                                      ? MemoryLocation.valueOf(s[0])
                                                      : MemoryLocation.valueOf(s[0], s[1], 0));
    }
    
    // no increment - Refinement was not successful
    if(increment.isEmpty()) {
      return false;
    }

    ValueAnalysisPrecision refinedExplicitPrecision  = new ValueAnalysisPrecision(explicitPrecision, increment);
    
    ArrayList<Precision> refinedPrecisions = new ArrayList<>(2);
    refinedPrecisions.add(refinedExplicitPrecision);
    
    ArrayList<Class<? extends Precision>> newPrecisionTypes = new ArrayList<>(2);
    newPrecisionTypes.add(ValueAnalysisPrecision.class);

    numberOfSuccessfulExplicitRefinements++;
    reached.removeSubtree(errorPath.get(1).getFirst(), refinedPrecisions, newPrecisionTypes);
    return true;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public String getName() {
    return "Use-Definition-Based Explicit-Refiner";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println("  number of explicit refinements:                      " + numberOfExplicitRefinements);
    out.println("  number of successful explicit refinements:           " + numberOfSuccessfulExplicitRefinements);
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
      // create a new ExplicitPathChecker, which does check the given path at full precision
      ValueAnalysisFeasibilityChecker checker = new ValueAnalysisFeasibilityChecker(logger, cfa);

      return checker.isFeasible(path);
    }
    catch (InterruptedException | InvalidConfigurationException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }
}
