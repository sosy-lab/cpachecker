// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getAllStatesOnPathsTo;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractOptionalCallstackWraper;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.cwriter.LoopCollectingEdgeVisitor;
import org.sosy_lab.cpachecker.util.predicates.NewtonRefinementManager;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.UCBRefinementManager;
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

/**
 * This class provides a basic refiner implementation for predicate analysis. When a counterexample
 * is found, it creates a path for it and checks it for feasibility, getting the interpolants if
 * possible.
 *
 * <p>It does not define any strategy for using the interpolants to update the abstraction, this is
 * left to an instance of {@link RefinementStrategy}.
 *
 * <p>It does, however, produce a nice error path in case of a feasible counterexample.
 */
@Options(prefix = "cpa.predicate.refinement")
public class PredicateCPARefiner implements ARGBasedRefiner, StatisticsProvider {

  @Option(secure = true, description = "which sliced prefix should be used for interpolation")
  private List<PrefixPreference> prefixPreference = PrefixSelector.NO_SELECTION;

  @Option(
      secure = true,
      description =
          "use only the atoms from the interpolants"
              + "as predicates, and not the whole interpolant")
  private boolean atomicInterpolants = true;

  @Option(
      secure = true,
      description =
          "Should the path invariants be created and used (potentially additionally to the other"
              + " invariants)")
  private boolean usePathInvariants = false;

  @Option(
      secure = true,
      description = "use Newton-based Algorithm for the CPA-Refinement, experimental feature!")
  private boolean useNewtonRefinement = false;

  @Option(
      secure = true,
      description = "use UCB predicates for the CPA-Refinement, experimental feature!")
  private boolean useUCBRefinement = false;

  @Option(
      secure = true,
      description =
          "Stop after refining the n-th spurious counterexample and export that. If 0, stop after"
              + " finding the first spurious counterexample but before refinement. If -1, never"
              + " stop. If this option is used with a value different from -1, option"
              + " counterexample.export.alwaysUseImpreciseCounterexamples=true should be set. Then,"
              + " an actually infeasible counterexample will be handed to export. So this option"
              + " will also not work with additional counterexample checks or similar, because"
              + " these may reject the (infeasible) counterexample.")
  @IntegerOption(min = -1)
  private int stopAfter = -1;

  // statistics
  private final StatInt totalPathLength =
      new StatInt(StatKind.AVG, "Avg. length of target path (in blocks)"); // measured in blocks
  private final StatTimer totalRefinement = new StatTimer("Time for refinement");
  private final StatTimer prefixExtractionTime =
      new StatTimer("Extracting infeasible sliced prefixes");

  private final StatTimer errorPathProcessing = new StatTimer("Error path post-processing");
  private final StatTimer getFormulasForPathTime = new StatTimer("Path-formulas extraction");

  private final StatInt totalPrefixes =
      new StatInt(StatKind.SUM, "Number of infeasible sliced prefixes");
  private final StatTimer prefixSelectionTime =
      new StatTimer("Selecting infeasible sliced prefixes");

  /** Number of performed refinements */
  private int refinements = 0;

  // the previously analyzed counterexample to detect repeated counterexamples
  private final Set<ImmutableList<CFANode>> lastErrorPaths = new HashSet<>();

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
  private final Optional<NewtonRefinementManager> newtonManager;
  private final Optional<UCBRefinementManager> ucbManager;

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
      loopFinder = new LoopCollectingEdgeVisitor(pLoopStructure.orElseThrow(), pConfig);
    } else {
      loopFinder = null;
      if (invariantsManager.addToPrecision()) {
        logger.log(
            Level.WARNING,
            "Invariants should be used during refinement, but loop information is not present.");
      }
    }

    // Create the NewtonRefinementManager iff Newton-based refinement is selected
    if (useNewtonRefinement) {
      newtonManager = Optional.of(new NewtonRefinementManager(logger, solver, pfmgr, pConfig));
    } else {
      newtonManager = Optional.empty();
    }

    if (useUCBRefinement) {
      ucbManager = Optional.of(new UCBRefinementManager(logger, solver, pfmgr));
    } else {
      ucbManager = Optional.empty();
    }

    logger.log(
        Level.INFO,
        "Using refinement for predicate analysis with "
            + strategy.getClass().getSimpleName()
            + " strategy.");
  }

  /**
   * Extracts the elements on the given path. If no branching/merging occured the returned Set is
   * empty.
   */
  private Set<ARGState> extractElementsOnPath(final ARGPath path) {
    Set<ARGState> elementsOnPath = getAllStatesOnPathsTo(path.getLastState());

    assert elementsOnPath.containsAll(path.getStateSet());
    assert elementsOnPath.size() >= path.size();

    return elementsOnPath;
  }

  /** Create list of formulas on path. */
  private BlockFormulas createFormulasOnPath(
      final ARGPath allStatesTrace, final List<ARGState> abstractionStatesTrace)
      throws CPAException, InterruptedException {
    BlockFormulas formulas =
        isRefinementSelectionEnabled()
            ? performRefinementSelection(allStatesTrace, abstractionStatesTrace)
            : getFormulasForPath(abstractionStatesTrace, allStatesTrace.getFirstState());

    // a user would expect "abstractionStatesTrace.size() == formulas.size()+1",
    // however we do not have the very first state in the trace,
    // because the rootState has always abstraction "True".
    assert abstractionStatesTrace.size() == formulas.getSize()
        : abstractionStatesTrace.size() + " != " + formulas.getSize();

    logger.log(Level.ALL, "Error path formulas: ", formulas);
    return formulas;
  }

  @Override
  public CounterexampleInfo performRefinementForPath(
      final ARGReachedSet pReached, final ARGPath allStatesTrace)
      throws CPAException, InterruptedException {
    totalRefinement.start();

    try {
      refinements++;
      BlockFormulas formulas;
      final boolean repeatedCounterexample;
      List<ARGState> abstractionStatesTrace;
      boolean branchingOccurred;
      ImmutableList<CFANode> errorPath =
          allStatesTrace.asStatesList().stream()
              .map(AbstractStates::extractLocation)
              .filter(x -> x != null)
              .collect(ImmutableList.toImmutableList());
      repeatedCounterexample = lastErrorPaths.contains(errorPath);
      lastErrorPaths.add(errorPath);

      Set<ARGState> elementsOnPath = extractElementsOnPath(allStatesTrace);
      // No branches/merges in path, it is precise.
      // We don't need to care about creating extra predicates for branching etc.
      branchingOccurred = true;
      if (elementsOnPath.size() == allStatesTrace.size()
          && !containsBranchingInPath(elementsOnPath)) {
        branchingOccurred = false;
      }

      // create path with all abstraction location elements (excluding the initial element)
      // the last element is the element corresponding to the error location
      abstractionStatesTrace = filterAbstractionStates(allStatesTrace);
      totalPathLength.setNextValue(abstractionStatesTrace.size());

      logger.log(Level.ALL, "Abstraction trace is", abstractionStatesTrace);

      formulas = createFormulasOnPath(allStatesTrace, abstractionStatesTrace);

      // find new invariants (this is a noop if no invariants should be used/generated)
      invariantsManager.findInvariants(allStatesTrace, abstractionStatesTrace, pfmgr, solver);

      CounterexampleTraceInfo counterexample =
          checkCounterexample(
              allStatesTrace, abstractionStatesTrace, formulas, repeatedCounterexample);

      // if error is spurious refine
      if (counterexample.isSpurious() && (stopAfter < 0 || refinements <= stopAfter)) {
        logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

        boolean trackFurtherCEX =
            strategy.performRefinement(
                pReached,
                abstractionStatesTrace,
                counterexample.getInterpolants(),
                repeatedCounterexample && !wereInvariantsUsedInLastRefinement);

        if (!trackFurtherCEX) {
          // when trackFurtherCEX is false, we only track 'one' CEX, otherwise we track all of them.
          lastErrorPaths.clear();
          lastErrorPaths.add(errorPath);
        }

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
          return pathChecker.handleFeasibleCounterexample(
              allStatesTrace, counterexample, branchingOccurred);
        } finally {
          errorPathProcessing.stop();
        }
      }

    } finally {
      totalRefinement.stop();
    }
  }

  /**
   * Check whether the path contains states A, B, C with successor relations A->B, B->C, A->C.
   * Branching like this would not be detected otherwise.
   */
  private boolean containsBranchingInPath(Set<ARGState> pElementsOnPath) {
    for (ARGState state : pElementsOnPath) {
      boolean alreadyFoundOneChild = false;
      for (ARGState child : state.getChildren()) {
        if (pElementsOnPath.contains(child)) {
          if (alreadyFoundOneChild) {
            // already found another child in the path, second child must be a branching.
            return true;
          } else {
            alreadyFoundOneChild = true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Check the given trace (or traces in the DAG) for feasibility and collect information why it is
   * feasible or why not.
   *
   * @param allStatesTrace a concrete path in the ARG.
   * @param abstractionStatesTrace the list of abstraction states along the path.
   * @param formulas the list of block formulas for the abstraction states along the path.
   * @param repeatedCounterexample whether the current counterexample was seen before.
   * @return information about the counterexample, for example a model (feasible CEX) or
   *     interpolants (infeasible CEX).
   */
  private CounterexampleTraceInfo checkCounterexample(
      final ARGPath allStatesTrace,
      final List<ARGState> abstractionStatesTrace,
      final BlockFormulas formulas,
      final boolean repeatedCounterexample)
      throws CPAException, InterruptedException {

    Preconditions.checkArgument(
        abstractionStatesTrace.size() == formulas.getSize(),
        "each abstraction state should have a block formula");
    Preconditions.checkArgument(
        abstractionStatesTrace.size() <= allStatesTrace.size(),
        "each abstraction state should have a state in the counterexample trace");

    // Set the atomic Predicates configuration in the RefinementStrategy
    if (strategy instanceof PredicateAbstractionRefinementStrategy) {
      ((PredicateAbstractionRefinementStrategy) strategy)
          .setUseAtomicPredicates(atomicInterpolants);
    }

    if (!repeatedCounterexample && (invariantsManager.addToPrecision() || usePathInvariants)) {
      // Compute invariants if desired, and if the counterexample is not a repeated one
      // (otherwise invariants for the same location didn't help before, so they won't help now).
      return performInvariantsRefinement(allStatesTrace, abstractionStatesTrace, formulas);

    } else if (useNewtonRefinement) {
      assert newtonManager.isPresent();
      if (!repeatedCounterexample) {
        try {
          logger.log(Level.FINEST, "Starting Newton-based refinement");
          return performNewtonRefinement(allStatesTrace, formulas);
        } catch (RefinementFailedException e) {
          if (e.getReason() == Reason.SequenceOfAssertionsToWeak
              && newtonManager.orElseThrow().fallbackToInterpolation()) {
            logger.log(
                Level.FINEST,
                "Fallback from Newton-based refinement to interpolation-based refinement");
            return performInterpolatingRefinement(allStatesTrace, abstractionStatesTrace, formulas);
          } else {
            throw e;
          }
        }
      } else {
        logger.log(
            Level.FINEST,
            "Fallback from Newton-based refinement to interpolation-based refinement");
        return performInterpolatingRefinement(allStatesTrace, abstractionStatesTrace, formulas);
      }
    } else if (useUCBRefinement) {
      logger.log(Level.FINEST, "Starting unsat-core-based refinement");
      return performUCBRefinement(allStatesTrace, abstractionStatesTrace, formulas);

    } else {
      logger.log(Level.FINEST, "Starting interpolation-based refinement.");
      return performInterpolatingRefinement(allStatesTrace, abstractionStatesTrace, formulas);
    }
  }

  private CounterexampleTraceInfo performInterpolatingRefinement(
      final ARGPath allStatesTrace,
      final List<ARGState> abstractionStatesTrace,
      final BlockFormulas formulas)
      throws CPAException, InterruptedException {

    return interpolationManager.buildCounterexampleTrace(
        formulas, ImmutableList.copyOf(abstractionStatesTrace), Optional.of(allStatesTrace));
  }

  private CounterexampleTraceInfo performInvariantsRefinement(
      final ARGPath allStatesTrace,
      final List<ARGState> abstractionStatesTrace,
      final BlockFormulas formulas)
      throws CPAException, InterruptedException {

    CounterexampleTraceInfo counterexample =
        interpolationManager.buildCounterexampleTraceWithoutInterpolation(
            formulas, Optional.of(allStatesTrace));

    // if error is spurious refine
    if (counterexample.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

      // add invariant precision increment if necessary
      List<BooleanFormula> precisionIncrement = new ArrayList<>();
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
            "Starting interpolation-based refinement because invariant generation was not"
                + " successful.");
        return performInterpolatingRefinement(allStatesTrace, abstractionStatesTrace, formulas);

      } else {
        wereInvariantsusedInCurrentRefinement = true;
        return CounterexampleTraceInfo.infeasible(precisionIncrement);
      }

    } else {
      return counterexample;
    }
  }

  private CounterexampleTraceInfo performNewtonRefinement(
      final ARGPath pAllStatesTrace, final BlockFormulas pFormulas)
      throws CPAException, InterruptedException {
    // Delegate the refinement task to the NewtonManager
    return newtonManager.orElseThrow().buildCounterexampleTrace(pAllStatesTrace, pFormulas);
  }

  private CounterexampleTraceInfo performUCBRefinement(
      final ARGPath allStatesTrace,
      final List<ARGState> pAbstractionStatesTrace,
      final BlockFormulas pFormulas)
      throws CPAException, InterruptedException {

    assert ucbManager.isPresent();
    return ucbManager
        .orElseThrow()
        .buildCounterexampleTrace(allStatesTrace, pAbstractionStatesTrace, pFormulas);
  }

  private List<BooleanFormula> addInvariants(final List<ARGState> abstractionStatesTrace)
      throws InterruptedException {
    List<BooleanFormula> precisionIncrement = new ArrayList<>();
    boolean invIsTriviallyTrue = true;

    // we do not need the last state from the trace, so we exclude it here
    for (ARGState state : from(abstractionStatesTrace).limit(abstractionStatesTrace.size() - 1)) {
      CFANode location = extractLocation(state);
      Optional<CallstackStateEqualsWrapper> callstack = extractOptionalCallstackWraper(state);
      BooleanFormula inv =
          invariantsManager.getInvariantFor(location, callstack, fmgr, pfmgr, null);
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

  /** This method returns the set of loops which are relevant for the given ARGPath. */
  private Set<Loop> getRelevantLoops(final ARGPath allStatesTrace) {
    // in the case we have no loop informaion we cannot find loops
    if (loopFinder == null) {
      return ImmutableSet.of();
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
            .filter(PredicateAbstractState::containsAbstractionState)
            .toList();

    // This assertion does not hold anymore for slicing abstractions.
    // TODO: Find a way to still check this for when we do not use slicing!
    // assert from(result).allMatch(state -> state.getParents().size() <= 1)
    //    : "PredicateCPARefiner expects abstraction states to have only one parent, but at least
    // one state has more.";

    assert Objects.equals(pPath.getLastState(), result.get(result.size() - 1));
    return result;
  }

  /**
   * Get the block formulas from a path.
   *
   * @param path A list of all abstraction elements
   * @param initialState The initial element of the analysis (= the root element of the ARG)
   * @return A list of block formulas for this path.
   */
  private BlockFormulas getFormulasForPath(List<ARGState> path, ARGState initialState)
      throws CPATransferException, InterruptedException {
    getFormulasForPathTime.start();
    try {
      return blockFormulaStrategy.getFormulasForPath(initialState, path);
    } finally {
      getFormulasForPathTime.stop();
    }
  }

  private BlockFormulas performRefinementSelection(
      final ARGPath pAllStatesTrace, final List<ARGState> pAbstractionStatesTrace)
      throws InterruptedException, CPAException {

    prefixExtractionTime.start();
    List<InfeasiblePrefix> infeasiblePrefixes =
        prefixProvider.extractInfeasiblePrefixes(pAllStatesTrace);
    prefixExtractionTime.stop();

    totalPrefixes.setNextValue(infeasiblePrefixes.size());

    if (infeasiblePrefixes.isEmpty()) {
      return getFormulasForPath(pAbstractionStatesTrace, pAllStatesTrace.getFirstState());
    } else {
      prefixSelectionTime.start();
      InfeasiblePrefix selectedPrefix =
          prefixSelector.selectSlicedPrefix(prefixPreference, infeasiblePrefixes);
      prefixSelectionTime.stop();

      List<BooleanFormula> formulas = new ArrayList<>(selectedPrefix.getPathFormulae());
      while (formulas.size() < pAbstractionStatesTrace.size()) {
        formulas.add(fmgr.getBooleanFormulaManager().makeTrue());
      }

      return new BlockFormulas(formulas);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
    if (strategy instanceof StatisticsProvider) {
      ((StatisticsProvider) strategy).collectStatistics(pStatsCollection);
    }
    if (useNewtonRefinement) {
      newtonManager.orElseThrow().collectStatistics(pStatsCollection);
    }
  }

  private class Stats implements Statistics {

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);

      int numberOfRefinements = totalRefinement.getUpdateCount();
      w0.put("Number of predicate refinements", totalRefinement.getUpdateCount());
      if (numberOfRefinements > 0) {
        w0.put(totalPathLength).put(totalPrefixes).spacer().put(totalRefinement);

        StatisticsWriter w1 = w0.beginLevel();
        interpolationManager.printStatistics(w1);

        w1.put(getFormulasForPathTime);
        if (isRefinementSelectionEnabled()) {
          w1.put(prefixExtractionTime);
          w1.put(prefixSelectionTime);
        }
        w1.put(errorPathProcessing);
      }
    }

    @Override
    public String getName() {
      return "PredicateCPARefiner";
    }
  }
}
