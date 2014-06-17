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

import static com.google.common.collect.FluentIterable.from;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.InitialAssumptionUseDefinitionCollector;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Refiner implementation that extracts a precision increment solely based on syntactical information from error traces.
 */
public class ValueAnalysisUseDefinitionBasedRefiner extends AbstractARGBasedRefiner implements Statistics, StatisticsProvider {
  // statistics
  private int numberOfRefinements           = 0;
  private int numberOfSuccessfulRefinements = 0;

  private final CFA cfa;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  public static ValueAnalysisUseDefinitionBasedRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ValueAnalysisUseDefinitionBasedRefiner.class.getSimpleName() + " could not find the ValueAnalysisCPA");
    }

    ValueAnalysisCPA valueAnalysisCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisUseDefinitionBasedRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    ValueAnalysisUseDefinitionBasedRefiner refiner = initialiseRefiner(cpa, valueAnalysisCpa);
    valueAnalysisCpa.getStats().addRefiner(refiner);
    valueAnalysisCpa.injectRefinablePrecision();

    return refiner;
  }

  private static ValueAnalysisUseDefinitionBasedRefiner initialiseRefiner(
      ConfigurableProgramAnalysis cpa, ValueAnalysisCPA pValueAnalysisCpa)
          throws CPAException, InvalidConfigurationException {
    LogManager logger = pValueAnalysisCpa.getLogger();

    return new ValueAnalysisUseDefinitionBasedRefiner(
        logger,
        pValueAnalysisCpa.getShutdownNotifier(),
        cpa,
        pValueAnalysisCpa.getStaticRefiner(),
        pValueAnalysisCpa.getCFA());
  }

  protected ValueAnalysisUseDefinitionBasedRefiner(
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ConfigurableProgramAnalysis pCpa,
      ValueAnalysisStaticRefiner pValueAnalysisStaticRefiner,
      final CFA pCfa) throws CPAException, InvalidConfigurationException {
    super(pCpa);

    cfa               = pCfa;
    logger            = pLogger;
    shutdownNotifier  = pShutdownNotifier;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if path is infeasible, try to refine the precision
    if (isPathFeasable(errorPath)) {
      ValueAnalysisConcreteErrorPathAllocator va = new ValueAnalysisConcreteErrorPathAllocator(logger, shutdownNotifier);

      Model model = va.allocateAssignmentsToPath(errorPath, cfa.getMachineModel());

      return CounterexampleInfo.feasible(errorPath, model);
    }

    else if (performValueAnalysisRefinement(reached, errorPath)) {
      return CounterexampleInfo.spurious();
    }

    throw new RefinementFailedException(Reason.RepeatedCounterexample, errorPath);
  }

  /**
   * This method performs an value-analysis refinement.
   *
   * @param reached the current reached set
   * @param errorPath the current error path
   * @returns true, if the value-analysis refinement was successful, else false
   * @throws CPAException when value-analysis interpolation fails
   */
  private boolean performValueAnalysisRefinement(final ARGReachedSet reached, ARGPath errorPath) throws CPAException, InterruptedException {
    numberOfRefinements++;

    try {
      ValueAnalysisFeasibilityChecker checker = new ValueAnalysisFeasibilityChecker(logger, cfa);
      List<ARGPath> prefixes = checker.getInfeasilbePrefixes(errorPath,
          ValueAnalysisPrecision.createDefaultPrecision(),
          new ValueAnalysisState());

      errorPath = new ErrorPathClassifier(cfa.getVarClassification()).obtainPrefixWithLowestScore(prefixes);
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Configuring ValueAnalysisFeasibilityChecker failed: " + e.getMessage(), e);
    }

    Multimap<CFANode, MemoryLocation> increment = obtainPrecisionIncrement(errorPath);

    // no increment - refinement was not successful
    if(increment.isEmpty()) {
      return false;
    }

    UnmodifiableReachedSet reachedSet             = reached.asReachedSet();
    Precision precision                           = reachedSet.getPrecision(reachedSet.getLastState());
    ValueAnalysisPrecision valueAnalysisPrecision = Precisions.extractPrecisionByType(precision, ValueAnalysisPrecision.class);

    reached.removeSubtree(errorPath.get(1).getFirst(),
        new ValueAnalysisPrecision(valueAnalysisPrecision, increment),
        ValueAnalysisPrecision.class);

    numberOfSuccessfulRefinements++;
    return true;
  }

  private Multimap<CFANode, MemoryLocation> obtainPrecisionIncrement(ARGPath errorPath) {
    List<CFAEdge> cfaTrace = from(errorPath).transform(Pair.<CFAEdge>getProjectionToSecond()).toList();
    Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();

    // just add each variable referenced in the use-def set to a BOGUS PROGRAM LOCATION (i.e., initial location)
    for(String referencedVariable : new InitialAssumptionUseDefinitionCollector().obtainUseDefInformation(cfaTrace)) {
      increment.put(cfaTrace.get(0).getSuccessor(), MemoryLocation.valueOf(referencedVariable));
    }

    return increment;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public String getName() {
    return "ValueAnalysisUseDefinitionBasedRefiner";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println("  number of refinements:                      " + numberOfRefinements);
    out.println("  number of successful refinements:           " + numberOfSuccessfulRefinements);
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
      // create a new ValueAnalysisChecker, which does check the given path at full precision
      ValueAnalysisFeasibilityChecker checker = new ValueAnalysisFeasibilityChecker(logger, cfa);

      return checker.isFeasible(path);
    }
    catch (InterruptedException | InvalidConfigurationException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }
}
