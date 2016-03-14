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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getAllStatesOnPathsTo;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.cwriter.LoopCollectingEdgeVisitor;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/**
 * This class provides a basic refiner implementation for predicate analysis.
 * When a counterexample is found, it creates a path for it and checks it for
 * feasibility, getting the interpolants if possible.
 *
 * It does not define any strategy for using the interpolants to update the
 * abstraction, this is left to an instance of {@link RefinementStrategy}.
 *
 * It does, however, produce a nice error path in case of a feasible counterexample.
 */
@Options(prefix = "cpa.predicate.refinement")
public class PredicateCPARefiner implements ARGBasedRefiner, StatisticsProvider {

  @Option(secure=true, description="which sliced prefix should be used for interpolation")
  private List<PrefixPreference> prefixPreference = PrefixSelector.NO_SELECTION;

  @Option(
    secure = true,
    description =
        "For differing errorpaths, the loop for which"
            + " invariants should be generated may still be the same, with this option"
            + " you can set the maximal amount of invariant generation runs per loop."
            + " 0 means no upper limit given."
  )
  private int maxInvariantGenerationsPerLoop = 2;

  @Option(
    secure = true,
    description =
        "use only atoms from generated invariants" + "as predicates, and not the whole invariant"
  )
  private boolean atomicInvariants = false;

  @Option(secure=true, description="use only the atoms from the interpolants"
                                 + "as predicates, and not the whole interpolant")
  private boolean atomicInterpolants = true;

  // statistics
  private final StatInt totalPathLength = new StatInt(StatKind.AVG, "Avg. length of target path (in blocks)"); // measured in blocks
  private final StatTimer totalRefinement = new StatTimer("Time for refinement");
  private final StatTimer prefixExtractionTime = new StatTimer("Extracting infeasible sliced prefixes");

  private final StatTimer errorPathProcessing = new StatTimer("Error path post-processing");
  private final StatTimer getFormulasForPathTime = new StatTimer("Path-formulas extraction");

  private final StatInt totalPrefixes = new StatInt(StatKind.SUM, "Number of infeasible sliced prefixes");
  private final StatTimer prefixSelectionTime = new StatTimer("Selecting infeasible sliced prefixes");

  // the previously analyzed counterexample to detect repeated counterexamples
  private List<CFANode> lastErrorPath = null;

  private final PathChecker pathChecker;

  private final InvariantsManager invariantsManager;
  private final Optional<LoopStructure> loopStructure;
  private final Map<Loop, Integer> loopOccurrences = new HashMap<>();

  // TODO Configuration should not be used at runtime, only during constructor
  private final Configuration config;

  private final PrefixProvider prefixProvider;
  private final PrefixSelector prefixSelector;
  private final LogManager logger;
  private final BlockFormulaStrategy blockFormulaStrategy;
  private final FormulaManagerView fmgr;
  private final InterpolationManager formulaManager;
  private final RefinementStrategy strategy;
  private final ShutdownNotifier shutdownNotifier;

  public PredicateCPARefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Optional<LoopStructure> pLoopStructure,
      final BlockFormulaStrategy pBlockFormulaStrategy,
      final FormulaManagerView pFmgr,
      final InterpolationManager pInterpolationManager,
      final PathChecker pPathChecker,
      final PrefixProvider pPrefixProvider,
      final PrefixSelector pPrefixSelector,
      final InvariantsManager pInvariantsManager,
      final RefinementStrategy pStrategy)
      throws InvalidConfigurationException {
    pConfig.inject(this, PredicateCPARefiner.class);

    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    loopStructure = pLoopStructure;
    blockFormulaStrategy = pBlockFormulaStrategy;
    fmgr = pFmgr;

    formulaManager = pInterpolationManager;
    pathChecker = pPathChecker;
    strategy = pStrategy;
    prefixProvider = pPrefixProvider;
    prefixSelector = pPrefixSelector;
    invariantsManager = pInvariantsManager;

    logger.log(Level.INFO, "Using refinement for predicate analysis with " + strategy.getClass().getSimpleName() + " strategy.");
  }

  /**
   * Extracts the elements on the given path. If no branching/merging occured
   * the returned Set is empty.
   */
  private Set<ARGState> extractElementsOnPath(final ARGPath path) {
    Set<ARGState> elementsOnPath = getAllStatesOnPathsTo(path.getLastState());

    assert elementsOnPath.containsAll(path.getStateSet());
    assert elementsOnPath.size() >= path.size();

    return elementsOnPath;
  }

  /**
   * Create list of formulas on path.
   */
  private List<BooleanFormula> createFormulasOnPath(final ARGPath allStatesTrace,
                                                      final List<ARGState> abstractionStatesTrace)
                                                      throws CPAException, InterruptedException {
    List<BooleanFormula> formulas = (isRefinementSelectionEnabled())
        ? performRefinementSelection(allStatesTrace, abstractionStatesTrace)
        : getFormulasForPath(abstractionStatesTrace, allStatesTrace.getFirstState());

    // a user would expect "abstractionStatesTrace.size() == formulas.size()+1",
    // however we do not have the very first state in the trace,
    // because the rootState has always abstraction "True".
    assert abstractionStatesTrace.size() == formulas.size()
               : abstractionStatesTrace.size() + " != " + formulas.size();

    logger.log(Level.ALL, "Error path formulas: ", formulas);
    return formulas;
  }

  @Override
  public CounterexampleInfo performRefinementForPath(final ARGReachedSet pReached, final ARGPath allStatesTrace) throws CPAException, InterruptedException {
    totalRefinement.start();
    try {
      final List<CFANode> errorPath =
          Lists.transform(allStatesTrace.asStatesList(), AbstractStates.EXTRACT_LOCATION);
      final boolean repeatedCounterexample = errorPath.equals(lastErrorPath);
      lastErrorPath = errorPath;

      Set<ARGState> elementsOnPath = extractElementsOnPath(allStatesTrace);
      // No branches/merges in path, it is precise.
      // We don't need to care about creating extra predicates for branching etc.
      boolean branchingOccurred = true;
      if (elementsOnPath.size() == allStatesTrace.size()) {
        elementsOnPath = Collections.emptySet();
        branchingOccurred = false;
      }

      // create path with all abstraction location elements (excluding the initial element)
      // the last element is the element corresponding to the error location
      final List<ARGState> abstractionStatesTrace = transformPath(allStatesTrace);
      totalPathLength.setNextValue(abstractionStatesTrace.size());

      logger.log(Level.ALL, "Abstraction trace is", abstractionStatesTrace);

      final List<BooleanFormula> formulas =
          createFormulasOnPath(allStatesTrace, abstractionStatesTrace);

      CounterexampleTraceInfo counterexample;

      // no invariants should be generated, we can do an interpolating refinement immediately
      if (invariantsManager.shouldInvariantsBeComputed()) {
        counterexample =
            performInvariantsRefinement(
                allStatesTrace,
                elementsOnPath,
                abstractionStatesTrace,
                formulas,
                repeatedCounterexample);

      } else {
        logger.log(Level.FINEST, "Starting interpolation-based refinement.");
        counterexample =
            performInterpolatingRefinement(abstractionStatesTrace, elementsOnPath, formulas);
      }

      // if error is spurious refine
      if (counterexample.isSpurious()) {
        logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

        strategy.performRefinement(
            pReached,
            abstractionStatesTrace,
            counterexample.getInterpolants(),
            repeatedCounterexample);
        return CounterexampleInfo.spurious();

      } else {
        // we have a real error
        logger.log(Level.FINEST, "Error trace is not spurious");
        return handleRealError(allStatesTrace, branchingOccurred, counterexample);
      }

    } finally {
      totalRefinement.stop();
    }
  }

  private CounterexampleTraceInfo performInterpolatingRefinement(
      final List<ARGState> abstractionStatesTrace,
      final Set<ARGState> elementsOnPath,
      final List<BooleanFormula> formulas)
      throws CPAException, InterruptedException {

    if (strategy instanceof PredicateAbstractionRefinementStrategy) {
      ((PredicateAbstractionRefinementStrategy) strategy)
          .setUseAtomicPredicates(atomicInterpolants);
    }

    return formulaManager.buildCounterexampleTrace(
        formulas,
        Lists.<AbstractState>newArrayList(abstractionStatesTrace),
        elementsOnPath,
        strategy.needsInterpolants());
  }

  private CounterexampleTraceInfo performInvariantsRefinement(
      final ARGPath allStatesTrace,
      final Set<ARGState> elementsOnPath,
      final List<ARGState> abstractionStatesTrace,
      final List<BooleanFormula> formulas,
      final boolean repeatedCounterexample)
      throws CPAException, InterruptedException {

    Set<Loop> loopsInPath;

    // check if invariants can be used at all
    if ((loopsInPath = canInvariantsBeUsed(allStatesTrace, repeatedCounterexample)).isEmpty()) {
      logger.log(
          Level.FINEST,
          "Starting interpolation-based refinement because invariants cannot be generated.");
      return performInterpolatingRefinement(abstractionStatesTrace, elementsOnPath, formulas);
    }

    CounterexampleTraceInfo counterexample =
        formulaManager.buildCounterexampleTrace(
            formulas,
            Lists.<AbstractState>newArrayList(abstractionStatesTrace),
            elementsOnPath,
            !invariantsManager.shouldInvariantsBeUsedForRefinement());

    // if error is spurious refine
    if (counterexample.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

      List<Pair<PathFormula, CFANode>> argForPathFormulaBasedGeneration = new ArrayList<>();
      for (ARGState state : abstractionStatesTrace) {
        CFANode node = extractLocation(state);
        // TODO what if loop structure does not exist?
        if (loopStructure.get().getAllLoopHeads().contains(node)) {
          PredicateAbstractState predState =
              extractStateByType(state, PredicateAbstractState.class);
          PathFormula pathFormula = predState.getPathFormula();
          argForPathFormulaBasedGeneration.add(Pair.of(pathFormula, node));
        } else if (!node.equals(
            extractLocation(abstractionStatesTrace.get(abstractionStatesTrace.size() - 1)))) {
          argForPathFormulaBasedGeneration.add(Pair.<PathFormula, CFANode>of(null, node));
        }
      }

      Triple<ARGPath, List<ARGState>, Set<Loop>> argForErrorPathBasedGeneration =
          Triple.of(allStatesTrace, abstractionStatesTrace, loopsInPath);

      invariantsManager.findInvariants(
          argForPathFormulaBasedGeneration, argForErrorPathBasedGeneration, shutdownNotifier);

      // add invariant precision increment if necessary
      if (invariantsManager.shouldInvariantsBeUsedForRefinement()) {
        List<BooleanFormula> precisionIncrement = invariantsManager.getInvariantsForRefinement();

        if (precisionIncrement.isEmpty()) {
          // fall-back to interpolation
          logger.log(
              Level.FINEST,
              "Starting interpolation-based refinement because invariant generation was not successful.");
          return performInterpolatingRefinement(abstractionStatesTrace, elementsOnPath, formulas);

        } else {
          if (strategy instanceof PredicateAbstractionRefinementStrategy) {
            ((PredicateAbstractionRefinementStrategy) strategy)
                .setUseAtomicPredicates(atomicInvariants);
          }
          return CounterexampleTraceInfo.infeasible(precisionIncrement);
        }

      } else {
        if (strategy instanceof PredicateAbstractionRefinementStrategy) {
          ((PredicateAbstractionRefinementStrategy) strategy)
              .setUseAtomicPredicates(atomicInterpolants);
        }
        return counterexample;
      }

    } else {
      return counterexample;
    }
  }

  /**
   * Checks if necessary conditions for invariant generation are met. These are
   * - the counterexample may not be a repeated one (invariants for the same location
   *     didn't help before, so they won't help now)
   * - the loops for which invariants should be generated was not in the counter
   *     example path too often (depending on configuration). Most likely computing
   *     invariants over and over for the same loop doesn't make much sense, this
   *     is almost the same as for repeated counterexamples.
   *
   * @return An empty set signalizes that invariants cannot be used. Otherwise the
   *         loops occuring in the current path are given
   */
  private Set<Loop> canInvariantsBeUsed(
      final ARGPath allStatesTrace, final boolean repeatedCounterexample) {
    // nothing was computed up to now, so just call refinement of
    // our super class if we have a repeated counter example
    // or we don't even need a precision increment
    if (repeatedCounterexample || !strategy.needsInterpolants()) {

      // only interpolation or invariant-based refinements should be counted
      // as repeated error paths
      if (!strategy.needsInterpolants()) {
        lastErrorPath = null;
      }
      return Collections.emptySet();
    }

    // get the relevant loops in the ARGPath and the number of occurrences of
    // the most often found one
    Set<Loop> loopsInPath = getRelevantLoops(allStatesTrace);
    int maxFoundLoop = getMaxCountOfOccuredLoop(loopsInPath);

    // no loops found, use normal interpolation refinement
    if (maxFoundLoop > maxInvariantGenerationsPerLoop || loopsInPath.isEmpty()) {
      return Collections.emptySet();
    }
    return loopsInPath;
  }

  /**
   * Returns the maximal number of occurences of one of the loops given in the
   * parameter. This method takes loops found in earlier refinements into account.
   */
  private int getMaxCountOfOccuredLoop(Set<Loop> loopsInPath) {
    int maxFoundLoop = 0;
    for (Loop loop : loopsInPath) {
      if (loopOccurrences.containsKey(loop)) {
        int tmpFoundLoop = loopOccurrences.get(loop) + 1;
        if (tmpFoundLoop > maxFoundLoop) {
          maxFoundLoop = tmpFoundLoop;
        }
        loopOccurrences.put(loop, tmpFoundLoop);
      } else {
        loopOccurrences.put(loop, 1);
        if (maxFoundLoop == 0) {
          maxFoundLoop = 1;
        }
      }
    }
    return maxFoundLoop;
  }

  /**
   * This method returns the set of loops which are relevant for the given
   * ARGPath.
   */
  private Set<Loop> getRelevantLoops(final ARGPath allStatesTrace) {
    PathIterator pathIt = allStatesTrace.pathIterator();
    LoopCollectingEdgeVisitor loopFinder = null;

    try {
      // TODO what if loop structure does not exist?
      loopFinder = new LoopCollectingEdgeVisitor(loopStructure.get(), config);
    } catch (InvalidConfigurationException e1) {
      // this will never happen, but for the case it does, we just return
      // the empty set, therefore the refinement will be done without invariant
      // generation definitely and only with interpolation / static refinement
      // TODO of course this can happen and it should not be swallowed!
      return Collections.emptySet();
    }

    while (pathIt.hasNext()) {
      loopFinder.visit(pathIt.getAbstractState(), pathIt.getOutgoingEdge(), null);
      pathIt.advance();
    }

    return loopFinder.getRelevantLoops().keySet();
  }

  /**
   * Creates a new CounterexampleInfo object out of the given parameters.
   */
  private CounterexampleInfo handleRealError(
      final ARGPath allStatesTrace,
      boolean branchingOccurred,
      CounterexampleTraceInfo counterexample)
      throws InterruptedException {

    errorPathProcessing.start();
    try {
      ARGPath targetPath;
      if (branchingOccurred) {
        Map<Integer, Boolean> preds = counterexample.getBranchingPredicates();
        if (preds.isEmpty()) {
          logger.log(Level.WARNING, "No information about ARG branches available!");
          return pathChecker.createImpreciseCounterexample(allStatesTrace, counterexample);
        }

        // find correct path
        try {
          ARGState root = allStatesTrace.getFirstState();
          ARGState target = allStatesTrace.getLastState();
          Set<ARGState> pathElements = ARGUtils.getAllStatesOnPathsTo(target);

          targetPath = ARGUtils.getPathFromBranchingInformation(root, target, pathElements, preds);

        } catch (IllegalArgumentException e) {
          logger.logUserException(Level.WARNING, e, null);
          logger.log(Level.WARNING, "The error path and the satisfying assignment may be imprecise!");

          return pathChecker.createImpreciseCounterexample(allStatesTrace, counterexample);
        }

      } else {
        targetPath = allStatesTrace;
      }

      return pathChecker.createCounterexample(targetPath, counterexample, branchingOccurred);
    } finally {
      errorPathProcessing.stop();
    }
  }

  /**
   * This method determines whether or not to perform refinement selection.
   *
   * @return true, if refinement selection has to be performed, else false
   */
  private boolean isRefinementSelectionEnabled() {
    return !prefixPreference.equals(PrefixSelector.NO_SELECTION);
  }

  static List<ARGState> transformPath(ARGPath pPath) {
    List<ARGState> result = from(pPath.asStatesList())
      .skip(1)
      .filter(Predicates.compose(PredicateAbstractState.FILTER_ABSTRACTION_STATES,
                                 toState(PredicateAbstractState.class)))
      .toList();

    assert from(result).allMatch(new Predicate<ARGState>() {
      @Override
      public boolean apply(ARGState pInput) {
        boolean correct = pInput.getParents().size() <= 1;
        assert correct : "PredicateCPARefiner expects abstraction states to have only one parent, but this state has more:" + pInput;
        return correct;
      }
    });

    assert pPath.getLastState() == result.get(result.size()-1);
    return result;
  }

  /**
   * Get the block formulas from a path.
   * @param path A list of all abstraction elements
   * @param initialState The initial element of the analysis (= the root element of the ARG)
   * @return A list of block formulas for this path.
   */
  private List<BooleanFormula> getFormulasForPath(List<ARGState> path, ARGState initialState)
      throws CPATransferException, InterruptedException {
    getFormulasForPathTime.start();
    try {
      return blockFormulaStrategy.getFormulasForPath(initialState, path);
    } finally {
      getFormulasForPathTime.stop();
    }
  }

  private List<BooleanFormula> performRefinementSelection(final ARGPath pAllStatesTrace,
      final List<ARGState> pAbstractionStatesTrace)
      throws InterruptedException, CPAException {

    prefixExtractionTime.start();
    List<InfeasiblePrefix> infeasiblePrefixes = prefixProvider.extractInfeasiblePrefixes(pAllStatesTrace);
    prefixExtractionTime.stop();

    totalPrefixes.setNextValue(infeasiblePrefixes.size());

    if (infeasiblePrefixes.isEmpty()) {
      return getFormulasForPath(pAbstractionStatesTrace, pAllStatesTrace.getFirstState());
    }

    else {
      prefixSelectionTime.start();
      InfeasiblePrefix selectedPrefix =
          prefixSelector.selectSlicedPrefix(prefixPreference, infeasiblePrefixes);
      prefixSelectionTime.stop();

      List<BooleanFormula> formulas = selectedPrefix.getPathFormulae();
      while (formulas.size() < pAbstractionStatesTrace.size()) {
        formulas.add(fmgr.getBooleanFormulaManager().makeBoolean(true));
      }

      return formulas;
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }

  private class Stats implements Statistics {

    private final Statistics statistics = strategy.getStatistics();

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);

      int numberOfRefinements = totalRefinement.getUpdateCount();
      w0.put("Number of predicate refinements", totalRefinement.getUpdateCount());
      if (numberOfRefinements > 0) {
        w0.put(totalPathLength)
          .put(totalPrefixes)
          .spacer()
          .put(totalRefinement);

        StatisticsWriter w1 = w0.beginLevel();
        formulaManager.printStatistics(w1);

        w1.put(getFormulasForPathTime);
        if (isRefinementSelectionEnabled()) {
          w1.put(prefixExtractionTime);
          w1.put(prefixSelectionTime);
        }
        w1.put(errorPathProcessing);

        statistics.printStatistics(out, result, reached);
      }
    }

    @Override
    public String getName() {
      return strategy.getStatistics().getName();
    }
  }
}
