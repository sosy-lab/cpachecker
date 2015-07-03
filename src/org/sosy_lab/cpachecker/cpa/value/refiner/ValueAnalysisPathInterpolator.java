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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.UseDefBasedInterpolator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.UseDefRelation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisEdgeInterpolator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.value.refinement")
public class ValueAnalysisPathInterpolator implements Statistics {
  /**
   * whether or not to do lazy-abstraction, i.e., when true, the re-starting node
   * for the re-exploration of the ARG will be the node closest to the root
   * where new information is made available through the current refinement
   */
  @Option(secure=true, description="whether or not to do lazy-abstraction")
  private boolean doLazyAbstraction = true;

  @Option(secure=true, description="whether or not to perform path slicing before interpolation")
  private boolean pathSlicing = true;

  @Option(secure=true, description="whether to perform (more precise) edge-based interpolation or (more efficient) path-based interpolation")
  private boolean performEdgeBasedInterpolation = true;

  @Option(secure=true, description="which prefix of an actual counterexample trace should be used for interpolation")
  private PrefixPreference prefixPreference = PrefixPreference.DOMAIN_GOOD_SHORT;

  /**
   * the offset in the path from where to cut-off the subtree, and restart the analysis
   */
  private int interpolationOffset = -1;

  /**
   * a reference to the assignment-counting state, to make the precision increment aware of thresholds
   */
  private UniqueAssignmentsInPathConditionState assignments = null;

  // statistics
  private StatCounter totalInterpolations   = new StatCounter("Number of interpolations");
  private StatInt totalInterpolationQueries = new StatInt(StatKind.SUM, "Number of interpolation queries");
  private StatInt sizeOfInterpolant         = new StatInt(StatKind.AVG, "Size of interpolant");
  private StatTimer timerInterpolation      = new StatTimer("Time for interpolation");

  private final StatInt totalPrefixes             = new StatInt(StatKind.SUM, "Number of infeasible sliced prefixes");
  private final StatTimer prefixExtractionTime    = new StatTimer("Extracting infeasible sliced prefixes");
  private final StatTimer prefixSelectionTime     = new StatTimer("Selecting infeasible sliced prefixes");

  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;

  private final ValueAnalysisEdgeInterpolator interpolator;

  public ValueAnalysisPathInterpolator(Configuration pConfig,
      final LogManager pLogger, final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;

    logger           = pLogger;
    cfa              = pCfa;
    shutdownNotifier = pShutdownNotifier;
    interpolator     = new ValueAnalysisEdgeInterpolator(pConfig, logger, shutdownNotifier, cfa);
  }

  protected Map<ARGState, ValueAnalysisInterpolant> performInterpolation(ARGPath errorPath,
      ValueAnalysisInterpolant interpolant) throws CPAException, InterruptedException {
    totalInterpolations.inc();
    timerInterpolation.start();

    interpolationOffset = -1;

    ARGPath errorPathPrefix = performRefinementSelection(errorPath, interpolant);

    Map<ARGState, ValueAnalysisInterpolant> interpolants =
        performEdgeBasedInterpolation
        ? performEdgeBasedInterpolation(errorPathPrefix, interpolant)
        : performPathBasedInterpolation(errorPathPrefix);

    timerInterpolation.stop();

    return interpolants;
  }

  /**
   * This path obtains a sliced infeasible prefix of the error path.
   *
   * @param errorPath the original error path
   * @param interpolant the initial interpolant, i.e. the initial state, with which to check the error path.
   * @return a sliced infeasible prefix of the error path
   * @throws CPAException
   */
  private ARGPath performRefinementSelection(ARGPath errorPath, final ValueAnalysisInterpolant interpolant) throws CPAException {

    if(prefixPreference == PrefixPreference.NONE) {
      return errorPath;
    }

    List<InfeasiblePrefix> infeasilbePrefixes = extractInfeasibleSlicedPrefixes(errorPath, interpolant);

    if(!infeasilbePrefixes.isEmpty()) {
      totalPrefixes.setNextValue(infeasilbePrefixes.size());

      prefixSelectionTime.start();
      PrefixSelector selector = new PrefixSelector(cfa.getVarClassification(), cfa.getLoopStructure());
      errorPath = selector.selectSlicedPrefix(prefixPreference, infeasilbePrefixes).getPath();
      prefixSelectionTime.stop();
    }

    return errorPath;
  }

  private List<InfeasiblePrefix> extractInfeasibleSlicedPrefixes(ARGPath errorPath, ValueAnalysisInterpolant interpolant)
      throws CPAException {

    try {
      ValueAnalysisPrefixProvider prefixProvider = new ValueAnalysisPrefixProvider(logger, cfa, config);

      prefixExtractionTime.start();
      List<InfeasiblePrefix> prefixes = prefixProvider.extractInfeasilbePrefixes(errorPath, interpolant.createValueAnalysisState());
      prefixExtractionTime.stop();

      return prefixes;
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Configuring ValueAnalysisFeasibilityChecker failed: " + e.getMessage(), e);
    }
  }

  /**
   * This method performs interpolation on each edge of the path, using the
   * {@link ValueAnalysisEdgeInterpolator}.
   *
   * @param errorPathPrefix the error path prefix to interpolate
   * @param interpolant an initial interpolant (only non-trivial when interpolating error path suffixes in global refinement)
   * @return the mapping of {@link ARGState}s to {@link ValueAnalysisInterpolant}s
   * @throws InterruptedException
   * @throws CPAException
   */
  private Map<ARGState, ValueAnalysisInterpolant> performEdgeBasedInterpolation(ARGPath errorPathPrefix,
      ValueAnalysisInterpolant interpolant)
      throws InterruptedException, CPAException {

    if (pathSlicing && prefixPreference != PrefixPreference.NONE) {
      errorPathPrefix = sliceErrorPath(errorPathPrefix);
    }

    Map<ARGState, ValueAnalysisInterpolant> pathInterpolants = new LinkedHashMap<>(errorPathPrefix.size());

    PathIterator pathIterator = errorPathPrefix.pathIterator();
    Deque<ValueAnalysisState> callstack = new ArrayDeque<>();
    while (pathIterator.hasNext()) {
      shutdownNotifier.shutdownIfNecessary();

      // interpolate at each edge as long the previous interpolant is not false
      if (!interpolant.isFalse()) {
        interpolant = interpolator.deriveInterpolant(errorPathPrefix,
            pathIterator.getOutgoingEdge(),
            callstack,
            pathIterator.getIndex(),
            interpolant);
      }

      totalInterpolationQueries.setNextValue(interpolator.getNumberOfInterpolationQueries());

      if (!interpolant.isTrivial() && interpolationOffset == -1) {
        interpolationOffset = pathIterator.getIndex();
      }

      sizeOfInterpolant.setNextValue(interpolant.getSize());

      pathIterator.advance();

      pathInterpolants.put(pathIterator.getAbstractState(), interpolant);

      if (!pathIterator.hasNext()) {
        assert interpolant.isFalse() : "final interpolant is not false: " + interpolant;
      }
    }

    return pathInterpolants;
  }

  /**
   * This method performs interpolation on the complete path, based on the
   * use-def-relation. It creates fake interpolants that are not inductive.
   *
   * @param errorPathPrefix the error path prefix to interpolate
   * @return
   */
  private Map<ARGState, ValueAnalysisInterpolant> performPathBasedInterpolation(ARGPath errorPathPrefix) {

    assert(prefixPreference != PrefixPreference.NONE)
    : "static path-based interpolation requires a sliced infeasible prefix"
    + " - set cpa.value.refiner.prefixPreference, e.g. to " + PrefixPreference.DOMAIN_GOOD_LONG;

    UseDefRelation useDefRelation = new UseDefRelation(errorPathPrefix,
        cfa.getVarClassification().isPresent()
          ? cfa.getVarClassification().get().getIntBoolVars()
          : Collections.<String>emptySet());

    Map<ARGState, ValueAnalysisInterpolant> interpolants = new UseDefBasedInterpolator(
        logger,
        errorPathPrefix,
        useDefRelation,
        cfa.getMachineModel()).obtainInterpolantsAsMap();

    totalInterpolationQueries.setNextValue(1);

    int size = 0;
    for(ValueAnalysisInterpolant itp : interpolants.values()) {
      size = size + itp.getSize();
    }
    sizeOfInterpolant.setNextValue(size);

    return interpolants;
  }

  /**
   * This utility method checks if the given path is feasible.
   */
  private boolean isFeasible(ARGPath slicedErrorPathPrefix) throws CPAException, InterruptedException {
    try {
       return new ValueAnalysisFeasibilityChecker(logger, cfa, config).isFeasible(slicedErrorPathPrefix);
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Configuring ValueAnalysisFeasibilityChecker failed: " + e.getMessage(), e);
    }
  }

  /**
   * This method returns a sliced error path (prefix). In case the sliced error path becomes feasible,
   * i.e., because slicing is not fully precise in presence of, e.g., structs or arrays, the original
   * error path (prefix) that was given as input is returned.
   *
   * @throws InterruptedException
   * @throws CPAException
   */
  private ARGPath sliceErrorPath(final ARGPath errorPathPrefix) throws CPAException, InterruptedException {

    Set<ARGState> useDefStates = new UseDefRelation(errorPathPrefix,
        cfa.getVarClassification().isPresent()
          ? cfa.getVarClassification().get().getIntBoolVars()
          : Collections.<String>emptySet()).getUseDefStates();

    ArrayDeque<Pair<FunctionCallEdge, Boolean>> functionCalls = new ArrayDeque<>();
    ArrayList<CFAEdge> abstractEdges = Lists.newArrayList(errorPathPrefix.asEdgesList());

    PathIterator iterator = errorPathPrefix.pathIterator();
    while (iterator.hasNext()) {
      CFAEdge originalEdge = iterator.getOutgoingEdge();

      // slice edge if there is neither a use nor a definition at the current state
      if (!useDefStates.contains(iterator.getAbstractState())) {
        abstractEdges.set(iterator.getIndex(), BlankEdge.buildNoopEdge(
            originalEdge.getPredecessor(),
            originalEdge.getSuccessor()));
      }

      /*************************************/
      /** assure that call stack is valid **/
      /*************************************/
      // when entering into a function, remember if call is relevant or not
      if(originalEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        functionCalls.push((Pair.of((FunctionCallEdge)originalEdge, abstractEdges.get(iterator.getIndex()).getEdgeType() == CFAEdgeType.FunctionCallEdge)));
      }

      // when returning from a function, ...
      if(originalEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
        Pair<FunctionCallEdge, Boolean> functionCallInfo = functionCalls.pop();
        // ... if call is relevant and return edge is now a blank edge, restore the original return edge
        if(functionCallInfo.getSecond() && abstractEdges.get(iterator.getIndex()).getEdgeType() == CFAEdgeType.BlankEdge) {
          abstractEdges.set(iterator.getIndex(), originalEdge);
        }

        // ... if call is irrelevant and return edge is not sliced, restore the call edge
        else if(!functionCallInfo.getSecond() && abstractEdges.get(iterator.getIndex()).getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          for(int j = iterator.getIndex(); j >= 0; j--) {
            if(functionCallInfo.getFirst() == abstractEdges.get(j)) {
              abstractEdges.set(j, functionCallInfo.getFirst());
              break;
            }
          }
        }
      }

      iterator.advance();
    }

    ARGPath slicedErrorPathPrefix = new ARGPath(errorPathPrefix.asStatesList(), abstractEdges);

    return (isFeasible(slicedErrorPathPrefix))
        ? errorPathPrefix
        : slicedErrorPathPrefix;

  }

  public Multimap<CFANode, MemoryLocation> determinePrecisionIncrement(MutableARGPath errorPath)
      throws CPAException, InterruptedException {

    assignments = AbstractStates.extractStateByType(errorPath.getLast().getFirst(),
        UniqueAssignmentsInPathConditionState.class);

    Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();

    Map<ARGState, ValueAnalysisInterpolant> itps = performInterpolation(errorPath.immutableCopy(),
        ValueAnalysisInterpolant.createInitial());

    for (Map.Entry<ARGState, ValueAnalysisInterpolant> itp : itps.entrySet()) {
      addToPrecisionIncrement(increment, AbstractStates.extractLocation(itp.getKey()), itp.getValue());
    }

    return increment;
  }

  /**
   * This method adds the given variable at the given location to the increment.
   *
   * @param increment the current increment
   * @param currentNode the current node for which to add a new variable
   * @param memoryLocation the name of the variable to add to the increment at the given edge
   */
  private void addToPrecisionIncrement(Multimap<CFANode, MemoryLocation> increment,
      CFANode currentNode,
      ValueAnalysisInterpolant itp) {
    for (MemoryLocation memoryLocation : itp.getMemoryLocations()) {
      if (assignments == null || !assignments.exceedsHardThreshold(memoryLocation)) {
        increment.put(currentNode, memoryLocation);
      }
    }
  }

  /**
   * This method determines the new refinement root.
   *
   * @param errorPath the error path from where to determine the refinement root
   * @param increment the current precision increment
   * @param isRepeatedRefinement the flag to determine whether or not this is a repeated refinement
   * @return the new refinement root
   * @throws RefinementFailedException if no refinement root can be determined
   */
  public Pair<ARGState, CFAEdge> determineRefinementRoot(MutableARGPath errorPath, Multimap<CFANode, MemoryLocation> increment,
      boolean isRepeatedRefinement) throws RefinementFailedException {

    if (interpolationOffset == -1) {
      throw new RefinementFailedException(Reason.InterpolationFailed, errorPath.immutableCopy());
    }

    // if doing lazy abstraction, use the node closest to the root node where new information is present
    if (doLazyAbstraction) {
      return errorPath.get(interpolationOffset);
    }

    // otherwise, just use the successor of the root node
    else {
      return errorPath.get(1);
    }
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out).beginLevel();
    writer.put(timerInterpolation)
      .put(totalInterpolations)
      .put(totalInterpolationQueries)
      .put(sizeOfInterpolant)
      .put(totalPrefixes);
    writer.put(prefixExtractionTime);
    writer.put(prefixSelectionTime);
  }

  public int getInterpolationOffset() {
    return interpolationOffset;
  }
}
