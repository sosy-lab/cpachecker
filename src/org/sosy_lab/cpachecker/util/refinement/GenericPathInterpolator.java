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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

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
  private List<PrefixPreference> prefixPreference = ImmutableList.of(PrefixPreference.DOMAIN_MIN, PrefixPreference.LENGTH_MIN);

  /**
   * the offset in the path from where to cut-off the subtree, and restart the analysis
   */
  protected int interpolationOffset = -1;

  // statistics
  protected final StatCounter totalInterpolations   = new StatCounter("Number of interpolations");
  protected final StatInt totalInterpolationQueries = new StatInt(StatKind.SUM, "Number of interpolation queries");
  protected final StatInt sizeOfInterpolant         = new StatInt(StatKind.AVG, "Size of interpolant");
  protected final StatTimer timerInterpolation      = new StatTimer("Time for interpolation");
  private final StatInt totalPrefixes               = new StatInt(StatKind.SUM, "Number of sliced prefixes");
  private final StatTimer prefixExtractionTime      = new StatTimer("Extracting infeasible sliced prefixes");
  private final StatTimer prefixSelectionTime       = new StatTimer("Selecting infeasible sliced prefixes");

  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final EdgeInterpolator<S, I> interpolator;
  private final FeasibilityChecker<S> checker;
  private final GenericPrefixProvider<S> prefixProvider;
  private final InterpolantManager<S, I> interpolantManager;

  public GenericPathInterpolator(
      final EdgeInterpolator<S, I> pEdgeInterpolator,
      final FeasibilityChecker<S> pFeasibilityChecker,
      final GenericPrefixProvider<S> pPrefixProvider,
      final InterpolantManager<S, I> pInterpolantManager,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    pConfig.inject(this, GenericPathInterpolator.class);

    logger             = pLogger;
    cfa                = pCfa;
    shutdownNotifier   = pShutdownNotifier;
    interpolator       = pEdgeInterpolator;
    checker = pFeasibilityChecker;
    interpolantManager = pInterpolantManager;

    prefixProvider = pPrefixProvider;
  }

  @Override
  public Map<ARGState, I> performInterpolation(
      final ARGPath errorPath,
      final I interpolant
  ) throws CPAException, InterruptedException {
    totalInterpolations.inc();

    interpolationOffset = -1;

    ARGPath errorPathPrefix = performRefinementSelection(errorPath, interpolant);

    timerInterpolation.start();

    Map<ARGState, I> interpolants =
        performEdgeBasedInterpolation(errorPathPrefix, interpolant);

    timerInterpolation.stop();

    propagateFalseInterpolant(errorPath, errorPathPrefix, interpolants);

    return interpolants;
  }

  /**
   * This method obtains a sliced infeasible prefix of the error path.
   *
   * @param pErrorPath the original error path
   * @param pInterpolant the initial interpolant, i.e. the initial state,
   *    with which to check the error path.
   * @return a sliced infeasible prefix of the error path
   */
  protected ARGPath performRefinementSelection(
      ARGPath pErrorPath,
      final I pInterpolant
  ) throws CPAException, InterruptedException {

    if(!isRefinementSelectionEnabled()) {
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
   * @param pErrorPathPrefix the error path prefix to interpolate
   * @param pInterpolant an initial interpolant
   *    (only non-trivial when interpolating error path suffixes in global refinement)
   * @return the mapping of {@link ARGState}s to {@link Interpolant}s
   */
  protected Map<ARGState, I> performEdgeBasedInterpolation(
      ARGPath pErrorPathPrefix,
      I pInterpolant
  ) throws InterruptedException, CPAException {

    pErrorPathPrefix = sliceErrorPath(pErrorPathPrefix);

    Map<ARGState, I> pathInterpolants = new LinkedHashMap<>(pErrorPathPrefix.size());

    PathIterator pathIterator = pErrorPathPrefix.pathIterator();
    Deque<S> callstack = new ArrayDeque<>();

    while (pathIterator.hasNext()) {
      shutdownNotifier.shutdownIfNecessary();

      // interpolate at each edge as long as the previous interpolant is not false
      if (!pInterpolant.isFalse()) {
        pInterpolant = interpolator.deriveInterpolant(pErrorPathPrefix,
                                                     pathIterator.getOutgoingEdge(),
                                                     callstack,
                                                     pathIterator.getPosition(),
                                                     pInterpolant);
      }

      totalInterpolationQueries.setNextValue(interpolator.getNumberOfInterpolationQueries());

      if (!pInterpolant.isTrivial() && interpolationOffset == -1) {
        interpolationOffset = pathIterator.getIndex();
      }

      sizeOfInterpolant.setNextValue(pInterpolant.getSize());

      pathIterator.advance();

      pathInterpolants.put(pathIterator.getAbstractState(), pInterpolant);

      if (!pathIterator.hasNext()) {
        assert pInterpolant.isFalse()
            : "final interpolant is not false: " + pInterpolant;
      }
    }

    return pathInterpolants;
  }

  /**
   * This utility method checks if the given path is feasible.
   */
  private boolean isFeasible(ARGPath slicedErrorPathPrefix)
      throws CPAException, InterruptedException {
    return checker.isFeasible(slicedErrorPathPrefix);
  }

  /**
   * This method returns a sliced error path (prefix). In case the sliced error path becomes feasible,
   * i.e., because slicing is not fully precise in presence of, e.g., structs or arrays, the original
   * error path (prefix) that was given as input is returned.
   */
  private ARGPath sliceErrorPath(final ARGPath pErrorPathPrefix)
      throws CPAException, InterruptedException {

    if (!isPathSlicingPossible(pErrorPathPrefix)) {
      return pErrorPathPrefix;
    }

    Set<ARGState> useDefStates = new UseDefRelation(pErrorPathPrefix,
        cfa.getVarClassification().isPresent()
          ? cfa.getVarClassification().get().getIntBoolVars()
          : Collections.<String>emptySet(), false).getUseDefStates();

    ArrayDeque<Pair<FunctionCallEdge, Boolean>> functionCalls = new ArrayDeque<>();
    ArrayList<CFAEdge> abstractEdges = Lists.newArrayList(pErrorPathPrefix.getInnerEdges());

    PathIterator iterator = pErrorPathPrefix.pathIterator();
    while (iterator.hasNext()) {
      CFAEdge originalEdge = iterator.getOutgoingEdge();

      // slice edge if there is neither a use nor a definition at the current state
      if (!useDefStates.contains(iterator.getAbstractState())) {
        CFANode startNode;
        CFANode endNode;
        if (originalEdge == null) {
          startNode = AbstractStates.extractLocation(iterator.getAbstractState());
          endNode = AbstractStates.extractLocation(iterator.getNextAbstractState());
        } else {
          startNode = originalEdge.getPredecessor();
          endNode = originalEdge.getSuccessor();
        }
        abstractEdges.set(iterator.getIndex(), BlankEdge.buildNoopEdge(startNode, endNode));
      }

      if (originalEdge != null) {
        CFAEdgeType typeOfOriginalEdge = originalEdge.getEdgeType();
        /*************************************/
        /** assure that call stack is valid **/
        /*************************************/
        // when entering into a function, remember if call is relevant or not
        if (typeOfOriginalEdge == CFAEdgeType.FunctionCallEdge) {
          boolean isAbstractEdgeFunctionCall =
              abstractEdges.get(iterator.getIndex()).getEdgeType() == CFAEdgeType.FunctionCallEdge;

          functionCalls.push(
              (Pair.of((FunctionCallEdge) originalEdge, isAbstractEdgeFunctionCall)));
        }

        // when returning from a function, ...
        if (typeOfOriginalEdge == CFAEdgeType.FunctionReturnEdge) {
          Pair<FunctionCallEdge, Boolean> functionCallInfo = functionCalls.pop();
          // ... if call is relevant and return edge is now a blank edge, restore the original return edge
          if (functionCallInfo.getSecond()
              && abstractEdges.get(iterator.getIndex()).getEdgeType() == CFAEdgeType.BlankEdge) {
            abstractEdges.set(iterator.getIndex(), originalEdge);
          }

          // ... if call is irrelevant and return edge is not sliced, restore the call edge
          else if (!functionCallInfo.getSecond()
              && abstractEdges.get(iterator.getIndex()).getEdgeType()
                  == CFAEdgeType.FunctionReturnEdge) {
            for (int j = iterator.getIndex(); j >= 0; j--) {
              if (functionCallInfo.getFirst() == abstractEdges.get(j)) {
                abstractEdges.set(j, functionCallInfo.getFirst());
                break;
              }
            }
          }
        }
      }

      iterator.advance();
    }

    ARGPath slicedErrorPathPrefix = new ARGPath(pErrorPathPrefix.asStatesList(), abstractEdges);

    return (isFeasible(slicedErrorPathPrefix))
        ? pErrorPathPrefix
        : slicedErrorPathPrefix;

  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  /**
   * This method propagates the interpolant "false" to all states that are in
   * the original error path, but are not anymore in the (shorter) prefix.
   *
   * The property that every state on the path beneath the first state with an
   * false interpolant is needed by some code in ValueAnalysisInterpolationTree
   * a subclass of {@link InterpolationTree}, i.e., for global refinement. This
   * property could also be enforced there, but interpolant creation should only
   * happen during interpolation, and not in the data structure holding the interpolants.
   *
   * @param errorPath the original error path
   * @param pErrorPathPrefix the possible shorter error path prefix
   * @param pInterpolants the current interpolant map
   */
  protected final void propagateFalseInterpolant(final ARGPath errorPath,
      final ARGPath pErrorPathPrefix,
      final Map<ARGState, I> pInterpolants
  ) {
    if (pErrorPathPrefix.size() < errorPath.size()) {
      PathIterator it = errorPath.pathIterator();
      for (int i = 0; i < pErrorPathPrefix.size(); i++) {
        it.advance();
      }
      for (ARGState state : it.getSuffixInclusive().asStatesList()) {
        pInterpolants.put(state, interpolantManager.getFalseInterpolant());
      }
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out).beginLevel();
    writer.put(timerInterpolation)
        .put(totalInterpolations)
        .put(totalInterpolationQueries)
        .put(sizeOfInterpolant)
        .put(totalPrefixes);
    writer.put(prefixExtractionTime);
    writer.put(prefixSelectionTime);
  }

  /**
   * This method checks if refinement selection is enabled.
   *
   * @return true, if if refinement selection is enabled, else false
   */
  protected boolean isRefinementSelectionEnabled() {
    return !prefixPreference.equals(PrefixSelector.NO_SELECTION);
  }

  /**
   * This method decides if path slicing is possible.
   *
   * It is only possible if the respective option is set,
   * and a prefix us selected (single reason for infeasibility),
   * and it is not a path suffix (from global refinement), i.e.,
   * it starts with the initial program state.
   *
   * @param pErrorPathPrefix the error path prefix to be sliced
   * @return true, if slicing is possible, else, false
   */
  private boolean isPathSlicingPossible(final ARGPath pErrorPathPrefix) {
    return pathSlicing
        && isRefinementSelectionEnabled()
        && pErrorPathPrefix.getFirstState().getParents().isEmpty();
  }
}
