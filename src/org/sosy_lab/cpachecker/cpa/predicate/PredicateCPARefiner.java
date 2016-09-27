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
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.cwriter.LoopCollectingEdgeVisitor;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

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
        "use only atoms from generated invariants" + "as predicates, and not the whole invariant"
  )
  private boolean atomicInvariants = false;

  @Option(secure=true, description="use only the atoms from the interpolants"
                                 + "as predicates, and not the whole interpolant")
  private boolean atomicInterpolants = true;

  @Option(
    secure = true,
    description =
        "Should the path invariants be created and used (potentially additionally to the other invariants)"
  )
  private boolean usePathInvariants = false;

  // statistics
  private final StatInt totalPathLength = new StatInt(StatKind.AVG, "Avg. length of target path (in blocks)"); // measured in blocks
  private final StatTimer totalRefinement = new StatTimer("Time for refinement");
  private final StatTimer prefixExtractionTime = new StatTimer("Extracting infeasible sliced prefixes");

  private final StatTimer errorPathProcessing = new StatTimer("Error path post-processing");
  private final StatTimer getFormulasForPathTime = new StatTimer("Path-formulas extraction");

  private final StatInt totalPrefixes = new StatInt(StatKind.SUM, "Number of infeasible sliced prefixes");
  private final StatTimer prefixSelectionTime = new StatTimer("Selecting infeasible sliced prefixes");

  // the previously analyzed counterexample to detect repeated counterexamples
  private ImmutableList<CFANode> lastErrorPath = null;

  private final PathChecker pathChecker;

  private final PredicateCPAInvariantsManager invariantsManager;
  private final LoopCollectingEdgeVisitor loopFinder;

  private boolean wereInvariantsUsedInLastRefinement = false;
  private boolean wereInvariantsusedInCurrentRefinement = false;
  private final Map<CFANode, BooleanFormula> lastInvariantForNode = new HashMap<>();

  private final PrefixProvider prefixProvider;
  private final PrefixSelector prefixSelector;
  private final LogManager logger;
  private final BlockFormulaStrategy blockFormulaStrategy;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfmgr;
  private final InterpolationManager interpolationManager;
  private final RefinementStrategy strategy;

  public PredicateCPARefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final Optional<LoopStructure> pLoopStructure,
      final BlockFormulaStrategy pBlockFormulaStrategy,
      final Solver pSolver,
      final PathFormulaManager pPfgmr,
      final InterpolationManager pInterpolationManager,
      final PathChecker pPathChecker,
      final PrefixProvider pPrefixProvider,
      final PrefixSelector pPrefixSelector,
      final PredicateCPAInvariantsManager pInvariantsManager,
      final RefinementStrategy pStrategy)
      throws InvalidConfigurationException {
    pConfig.inject(this, PredicateCPARefiner.class);

    logger = pLogger;
    blockFormulaStrategy = pBlockFormulaStrategy;
    solver = pSolver;
    fmgr = solver.getFormulaManager();
    pfmgr = pPfgmr;

    interpolationManager = pInterpolationManager;
    pathChecker = pPathChecker;
    strategy = pStrategy;
    prefixProvider = pPrefixProvider;
    prefixSelector = pPrefixSelector;
    invariantsManager = pInvariantsManager;

    if (pLoopStructure.isPresent()) {
      loopFinder = new LoopCollectingEdgeVisitor(pLoopStructure.get(), pConfig);
    } else {
      loopFinder = null;
      if (invariantsManager.addToPrecision()) {
        logger.log(
            Level.WARNING,
            "Invariants should be used during refinement, but loop information is not present.");
      }
    }

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
      final ImmutableList<CFANode> errorPath =
          ImmutableList.copyOf(
              Lists.transform(allStatesTrace.asStatesList(), AbstractStates.EXTRACT_LOCATION));
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
      final List<ARGState> abstractionStatesTrace = filterAbstractionStates(allStatesTrace);
      totalPathLength.setNextValue(abstractionStatesTrace.size());

      logger.log(Level.ALL, "Abstraction trace is", abstractionStatesTrace);

      final List<BooleanFormula> formulas =
          createFormulasOnPath(allStatesTrace, abstractionStatesTrace);

      CounterexampleTraceInfo counterexample;

      // find new invariants (this is a noop if no invariants should be used/generated)
      invariantsManager.findInvariants(allStatesTrace, abstractionStatesTrace, pfmgr, solver);

      // Compute invariants if desired, and if the counterexample is not a repeated one
      // (otherwise invariants for the same location didn't help before, so they won't help now).
      if (!repeatedCounterexample && (invariantsManager.addToPrecision() || usePathInvariants)) {
        counterexample =
            performInvariantsRefinement(
                allStatesTrace,
                elementsOnPath,
                abstractionStatesTrace,
                formulas);

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
            repeatedCounterexample && !wereInvariantsUsedInLastRefinement);

        // set some invariants flags, they are necessary to make sure we
        // call performRefinement in a way that it doesn't think it is a repeated
        // counterexample due to weak invariants
        wereInvariantsUsedInLastRefinement = wereInvariantsusedInCurrentRefinement;
        wereInvariantsusedInCurrentRefinement = false;

        return CounterexampleInfo.spurious();

      } else {
        // we have a real error
        logger.log(Level.FINEST, "Error trace is not spurious");
        errorPathProcessing.start();
        try {
          return pathChecker.handleFeasibleCounterexample(allStatesTrace, counterexample, branchingOccurred);
        } finally {
          errorPathProcessing.stop();
        }
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

    return interpolationManager.buildCounterexampleTrace(
        formulas,
        Lists.<AbstractState>newArrayList(abstractionStatesTrace),
        elementsOnPath,
        true);
  }

  private CounterexampleTraceInfo performInvariantsRefinement(
      final ARGPath allStatesTrace,
      final Set<ARGState> elementsOnPath,
      final List<ARGState> abstractionStatesTrace,
      final List<BooleanFormula> formulas)
      throws CPAException, InterruptedException {

    CounterexampleTraceInfo counterexample =
        interpolationManager.buildCounterexampleTrace(
            formulas,
            Lists.<AbstractState>newArrayList(abstractionStatesTrace),
            elementsOnPath,
            false);

    // if error is spurious refine
    if (counterexample.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

      // add invariant precision increment if necessary
      List<BooleanFormula> precisionIncrement = Lists.newArrayList();
      if (invariantsManager.addToPrecision()) {
        precisionIncrement = addInvariants(abstractionStatesTrace);
      }

      if (usePathInvariants) {
        precisionIncrement =
            addPathInvariants(allStatesTrace, abstractionStatesTrace, precisionIncrement);
      }

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
        wereInvariantsusedInCurrentRefinement = true;
        return CounterexampleTraceInfo.infeasible(precisionIncrement);
      }

    } else {
      return counterexample;
    }
  }

  private List<BooleanFormula> addInvariants(final List<ARGState> abstractionStatesTrace) {
    List<BooleanFormula> precisionIncrement = new ArrayList<>();
    boolean invIsTriviallyTrue = true;

    // we do not need the last state from the trace, so we exclude it here
    for (ARGState state : from(abstractionStatesTrace).limit(abstractionStatesTrace.size() - 1)) {
      CFANode location = extractLocation(state);
      BooleanFormula inv = invariantsManager.getInvariantFor(location, fmgr, pfmgr, null);
      if (invIsTriviallyTrue
          && !fmgr.getBooleanFormulaManager().isTrue(inv)
          && (!lastInvariantForNode.containsKey(location)
              || !lastInvariantForNode.get(location).equals(inv))) {
        invIsTriviallyTrue = false;
        lastInvariantForNode.put(location, inv);
      }
      precisionIncrement.add(inv);
    }
    assert precisionIncrement.size() == abstractionStatesTrace.size() - 1;

    if (invIsTriviallyTrue) {
      precisionIncrement.clear();
    }
    return precisionIncrement;
  }

  private List<BooleanFormula> addPathInvariants(
      final ARGPath allStatesTrace,
      final List<ARGState> abstractionStatesTrace,
      List<BooleanFormula> precisionIncrement) {
    Set<Loop> loopsInPath = getRelevantLoops(allStatesTrace);
    if (!loopsInPath.isEmpty()) {
      List<BooleanFormula> pathInvariants =
          invariantsManager.findPathInvariants(
              allStatesTrace, abstractionStatesTrace, loopsInPath, pfmgr, solver);

      if (precisionIncrement.isEmpty()) {
        precisionIncrement = pathInvariants;

      } else {
        Preconditions.checkState(precisionIncrement.size() == pathInvariants.size());

        Iterator<BooleanFormula> invIt = precisionIncrement.iterator();
        Iterator<BooleanFormula> pathInvIt = pathInvariants.iterator();
        List<BooleanFormula> mergeFormulas = new ArrayList<>();
        while (invIt.hasNext()) {
          mergeFormulas.add(fmgr.getBooleanFormulaManager().and(invIt.next(), pathInvIt.next()));
        }
        precisionIncrement = mergeFormulas;
      }

    } else {
      logger.log(
          Level.WARNING, "Path invariants could not be computed, loop information is missing");
    }
    return precisionIncrement;
  }

  /**
   * This method returns the set of loops which are relevant for the given
   * ARGPath.
   */
  private Set<Loop> getRelevantLoops(final ARGPath allStatesTrace) {
    // in the case we have no loop informaion we cannot find loops
    if (loopFinder == null) {
      return Collections.emptySet();
    }

    loopFinder.reset();

    PathIterator pathIt = allStatesTrace.fullPathIterator();
    while (pathIt.hasNext()) {
      if (pathIt.isPositionWithState()) {
        loopFinder.visit(pathIt.getAbstractState(), pathIt.getOutgoingEdge(), null);
      } else {
        loopFinder.visit(pathIt.getPreviousAbstractState(), pathIt.getOutgoingEdge(), null);
      }
      pathIt.advance();
    }

    return loopFinder.getRelevantLoops().keySet();
  }

  /**
   * This method determines whether or not to perform refinement selection.
   *
   * @return true, if refinement selection has to be performed, else false
   */
  private boolean isRefinementSelectionEnabled() {
    return !prefixPreference.equals(PrefixSelector.NO_SELECTION);
  }

  static List<ARGState> filterAbstractionStates(ARGPath pPath) {
    List<ARGState> result =
        from(pPath.asStatesList())
            .skip(1)
            .filter(PredicateAbstractState.CONTAINS_ABSTRACTION_STATE)
            .toList();

    assert from(result).allMatch(state -> state.getParents().size() <= 1)
        : "PredicateCPARefiner expects abstraction states to have only one parent, but at least one state has more.";

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
        formulas.add(fmgr.getBooleanFormulaManager().makeTrue());
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
        interpolationManager.printStatistics(w1);

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
