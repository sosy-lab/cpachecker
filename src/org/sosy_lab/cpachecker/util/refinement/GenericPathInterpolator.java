/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.refinement;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import com.google.common.collect.Lists;

/**
 * Generic path interpolator. Always performs edge interpolation.
 *
 * @param <S> the state type to create interpolants for
 * @param <I> the type of the interpolants created by this class
 */
@Options(prefix="cpa.value.refinement")
public class GenericPathInterpolator<S extends ForgetfulState<?>, I extends Interpolant<S>>
    implements PathInterpolator<I> {

  @Option(secure=true, description="whether or not to perform path slicing before interpolation")
  private boolean pathSlicing = true;

  @Option(secure=true, description="which prefix of an actual counterexample trace should be used for interpolation")
  private PrefixPreference prefixPreference = PrefixPreference.DOMAIN_GOOD_SHORT;

  /**
   * the offset in the path from where to cut-off the subtree, and restart the analysis
   */
  protected int interpolationOffset = -1;

  // statistics
  private StatCounter totalInterpolations   = new StatCounter("Number of interpolations");
  private StatInt totalInterpolationQueries = new StatInt(StatKind.SUM, "Number of interpolation queries");
  private StatInt sizeOfInterpolant         = new StatInt(StatKind.AVG, "Size of interpolant");
  private StatTimer timerInterpolation      = new StatTimer("Time for interpolation");
  protected StatInt totalPrefixes = new StatInt(StatKind.SUM, "Number of sliced prefixes");
  private final StatTimer prefixExtractionTime    = new StatTimer("Extracting infeasible sliced prefixes");
  private final StatTimer prefixSelectionTime     = new StatTimer("Selecting infeasible sliced prefixes");

  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;

  private final EdgeInterpolator<S, I> interpolator;
  private final FeasibilityChecker<S> checker;
  private final GenericPrefixProvider<S> prefixProvider;

  public GenericPathInterpolator(
      final EdgeInterpolator<S, I> pEdgeInterpolator,
      final FeasibilityChecker<S> pFeasibilityChecker,
      final GenericPrefixProvider<S> pPrefixProvider,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    pConfig.inject(this);
    config = pConfig;

    logger             = pLogger;
    cfa                = pCfa;
    shutdownNotifier   = pShutdownNotifier;
    interpolator       = pEdgeInterpolator;
    checker = pFeasibilityChecker;

    prefixProvider = pPrefixProvider;
  }

  @Override
  public Map<ARGState, I> performInterpolation(
      final ARGPath errorPath,
      final I interpolant
  ) throws CPAException, InterruptedException {
    try {
      totalInterpolations.inc();
      timerInterpolation.start();

      interpolationOffset = -1;

      ARGPath errorPathPrefix = performRefinementSelection(errorPath, interpolant);

      Map<ARGState, I> interpolants =
          performEdgeBasedInterpolation(errorPathPrefix, interpolant);

      timerInterpolation.stop();

      return interpolants;

    } catch (InterruptedException e) {
      throw new CPAException("Interrupted while performing interpolation", e);
    }

  }

  /**
   * This method obtains a sliced infeasible prefix of the error path.
   *
   * @param pErrorPath the original error path
   * @param pInterpolant the initial interpolant, i.e. the initial state,
   *    with which to check the error path.
   * @return a sliced infeasible prefix of the error path
   * @throws CPAException
   */
  protected ARGPath performRefinementSelection(
      ARGPath pErrorPath,
      final I pInterpolant
  ) throws CPAException, InterruptedException {

    if(prefixPreference == PrefixPreference.NONE) {
      return pErrorPath;
    }

    List<InfeasiblePrefix> infeasilbePrefixes = extractInfeasibleSlicedPrefixes(pErrorPath, pInterpolant);

    if(!infeasilbePrefixes.isEmpty()) {
      totalPrefixes.setNextValue(infeasilbePrefixes.size());

      prefixSelectionTime.start();
      PrefixSelector selector = new PrefixSelector(cfa.getVarClassification(), cfa.getLoopStructure());
      pErrorPath = selector.selectSlicedPrefix(prefixPreference, infeasilbePrefixes).getPath();
      logger.logf(Level.FINER, "Sliced prefix selected:\n %s", pErrorPath);
      prefixSelectionTime.stop();
    }

    return pErrorPath;
  }

  private List<InfeasiblePrefix> extractInfeasibleSlicedPrefixes(
      final ARGPath pErrorPath,
      final I pInterpolant
  ) throws CPAException, InterruptedException {

    prefixExtractionTime.start();
    List<InfeasiblePrefix> prefixes =
        prefixProvider.extractInfeasiblePrefixes(pErrorPath, pInterpolant.reconstructState());
    prefixExtractionTime.stop();

    return prefixes;
  }

  /**
   * This method performs interpolation on each edge of the path, using the
   * {@link EdgeInterpolator} given to this object at construction.
   *
   * @param errorPathPrefix the error path prefix to interpolate
   * @param interpolant an initial interpolant
   *    (only non-trivial when interpolating error path suffixes in global refinement)
   * @return the mapping of {@link ARGState}s to {@link Interpolant}s
   *
   * @throws InterruptedException
   * @throws CPAException
   */
  protected Map<ARGState, I> performEdgeBasedInterpolation(
      ARGPath errorPathPrefix,
      I interpolant
  ) throws InterruptedException, CPAException {

    if (pathSlicing && prefixPreference != PrefixPreference.NONE) {
      errorPathPrefix = sliceErrorPath(errorPathPrefix);
    }

    Map<ARGState, I> pathInterpolants = new LinkedHashMap<>(errorPathPrefix.size());

    PathIterator pathIterator = errorPathPrefix.pathIterator();
    Deque<S> callstack = new ArrayDeque<>();

    while (pathIterator.hasNext()) {
      shutdownNotifier.shutdownIfNecessary();

      // interpolate at each edge as long as the previous interpolant is not false
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
        assert interpolant.isFalse()
            : "final interpolant is not false: " + interpolant;
      }
    }

    return pathInterpolants;
  }

  /**
   * This utility method checks if the given path is feasible.
   */
  private boolean isFeasible(ARGPath slicedErrorPathPrefix) throws CPAException {
    return checker.isFeasible(slicedErrorPathPrefix);
  }

  /**
   * This method returns a sliced error path (prefix). In case the sliced error path becomes feasible,
   * i.e., because slicing is not fully precise in presence of, e.g., structs or arrays, the original
   * error path (prefix) that was given as input is returned.
   *
   * @throws InterruptedException
   * @throws CPAException
   */
  private ARGPath sliceErrorPath(final ARGPath errorPathPrefix)
      throws CPAException, InterruptedException {

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

      CFAEdgeType typeOfOriginalEdge = originalEdge.getEdgeType();
      /*************************************/
      /** assure that call stack is valid **/
      /*************************************/
      // when entering into a function, remember if call is relevant or not
      if(typeOfOriginalEdge == CFAEdgeType.FunctionCallEdge) {
        boolean isAbstractEdgeFunctionCall =
            abstractEdges.get(iterator.getIndex()).getEdgeType() == CFAEdgeType.FunctionCallEdge;

        functionCalls.push((Pair.of((FunctionCallEdge)originalEdge, isAbstractEdgeFunctionCall)));
      }

      // when returning from a function, ...
      if(typeOfOriginalEdge == CFAEdgeType.FunctionReturnEdge) {
        Pair<FunctionCallEdge, Boolean> functionCallInfo = functionCalls.pop();
        // ... if call is relevant and return edge is now a blank edge, restore the original return edge
        if(functionCallInfo.getSecond()
            && abstractEdges.get(iterator.getIndex()).getEdgeType() == CFAEdgeType.BlankEdge) {
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

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out).beginLevel();
    writer.put(timerInterpolation);
    writer.put(totalInterpolations);
    writer.put(totalInterpolationQueries);
    writer.put(sizeOfInterpolant);
    writer.put(totalPrefixes);
  }

  public int getInterpolationOffset() {
    return interpolationOffset;
  }
}
