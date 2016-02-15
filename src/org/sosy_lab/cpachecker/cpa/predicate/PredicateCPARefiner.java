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
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;

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
@Options(prefix="cpa.predicate.refinement")
public class PredicateCPARefiner extends AbstractARGBasedRefiner implements StatisticsProvider {

  @Option(secure=true, description="slice block formulas, experimental feature!")
  private boolean sliceBlockFormulas = false;

  @Option(secure=true, description="Conjunct the formulas that were computed as preconditions to get (infeasible) interpolation problems!")
  private boolean conjunctPreconditionFormulas = false;

  @Option(secure=true, description="which sliced prefix should be used for interpolation")
  private List<PrefixPreference> prefixPreference = PrefixSelector.NO_SELECTION;

  @Option(secure=true, description="use only the atoms from the interpolants"
                                 + "as predicates, and not the whole interpolant")
  protected boolean atomicInterpolants = true;

  @Option(secure=true, description="Call buildCounterexampleTrace() n times to produce"
      + " different interpolants with the solver")
  private int checkCounterexampleNTimes = 1;

  // statistics
  protected final StatInt totalPathLength = new StatInt(StatKind.AVG, "Avg. length of target path (in blocks)"); // measured in blocks
  protected final StatTimer totalRefinement = new StatTimer("Time for refinement");
  protected final StatTimer prefixExtractionTime = new StatTimer("Extracting infeasible sliced prefixes");

  private final StatTimer errorPathProcessing = new StatTimer("Error path post-processing");
  private final StatTimer getFormulasForPathTime = new StatTimer("Path-formulas extraction");

  private final StatInt totalPrefixes = new StatInt(StatKind.SUM, "Number of infeasible sliced prefixes");
  private final StatTimer prefixSelectionTime = new StatTimer("Selecting infeasible sliced prefixes");

  // the previously analyzed counterexample to detect repeated counterexamples
  private List<CFANode> lastErrorPath = null;

  private final PathChecker pathChecker;
  private final PredicateAssumeStore assumesStore;


  protected final Solver solver;
  protected final PrefixProvider prefixProvider;
  protected final LogManager logger;
  protected final PathFormulaManager pfmgr;
  protected final FormulaManagerView fmgr;
  protected final InterpolationManager formulaManager;
  protected final RefinementStrategy strategy;
  protected final CFA cfa;
  protected final ShutdownNotifier shutdownNotifier;

  public PredicateCPARefiner(final Configuration pConfig, final LogManager pLogger,
      final ConfigurableProgramAnalysis pCpa,
      final InterpolationManager pInterpolationManager,
      final PathChecker pPathChecker,
      final PrefixProvider pPrefixProvider,
      final PathFormulaManager pPathFormulaManager,
      final RefinementStrategy pStrategy,
      final Solver pSolver,
      final PredicateAssumeStore pAssumesStore,
      final CFA pCfa)
          throws InvalidConfigurationException {

    super(pCpa);

    pConfig.inject(this, PredicateCPARefiner.class);

    assumesStore = pAssumesStore;
    solver = pSolver;
    logger = pLogger;
    formulaManager = pInterpolationManager;
    pathChecker = pPathChecker;
    pfmgr = pPathFormulaManager;
    fmgr = solver.getFormulaManager();
    strategy = pStrategy;
    cfa = pCfa;
    shutdownNotifier = CPAs.retrieveCPA(pCpa, PredicateCPA.class).getShutdownNotifier();
    prefixProvider = pPrefixProvider;

    logger.log(Level.INFO, "Using refinement for predicate analysis with " + strategy.getClass().getSimpleName() + " strategy.");
  }

  /**
   * Extracts the elements on the given path. If no branching/merging occured
   * the returned Set is empty.
   */
  protected Set<ARGState> extractElementsOnPath(final ARGPath path) {
    Set<ARGState> elementsOnPath = getAllStatesOnPathsTo(path.getLastState());

    assert elementsOnPath.containsAll(path.getStateSet());
    assert elementsOnPath.size() >= path.size();

    return elementsOnPath;
  }

  /**
   * Create list of formulas on path.
   */
  protected List<BooleanFormula> createFormulasOnPath(final ARGPath allStatesTrace,
                                                      final List<ARGState> abstractionStatesTrace)
                                                      throws CPAException, InterruptedException {
    List<BooleanFormula> formulas;
    try {
      formulas = (isRefinementSelectionEnabled())
        ? performRefinementSelection(allStatesTrace, abstractionStatesTrace)
        : getFormulasForPath(abstractionStatesTrace, allStatesTrace.getFirstState());
    } catch (SolverException e) {
      throw new CPAException("Solver Exception", e);
    }

    // a user would expect "abstractionStatesTrace.size() == formulas.size()+1",
    // however we do not have the very first state in the trace,
    // because the rootState has always abstraction "True".
    assert abstractionStatesTrace.size() == formulas.size()
               : abstractionStatesTrace.size() + " != " + formulas.size();

    logger.log(Level.ALL, "Error path formulas: ", formulas);
    return formulas;
  }

  @Override
  public CounterexampleInfo performRefinement(final ARGReachedSet pReached, final ARGPath allStatesTrace) throws CPAException, InterruptedException {
    totalRefinement.start();
    logger.log(Level.FINEST, "Starting interpolation-based refinement");

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

    final List<BooleanFormula> formulas = createFormulasOnPath(allStatesTrace, abstractionStatesTrace);


    final List<CFANode> errorPath = Lists.transform(allStatesTrace.asStatesList(), AbstractStates.EXTRACT_LOCATION);
    final boolean repeatedCounterexample = errorPath.equals(lastErrorPath);
    lastErrorPath = errorPath;

    CounterexampleTraceInfo counterexample = buildCounterexampleTrace(elementsOnPath,
        abstractionStatesTrace, formulas, strategy.needsInterpolants());

    // if error is spurious refine
    if (counterexample.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

      if (strategy instanceof PredicateAbstractionRefinementStrategy) {
        ((PredicateAbstractionRefinementStrategy)strategy).setUseAtomicPredicates(atomicInterpolants);
      }

      strategy.performRefinement(pReached, abstractionStatesTrace, counterexample.getInterpolants(), repeatedCounterexample);

      totalRefinement.stop();
      return CounterexampleInfo.spurious();

    } else {
      // we have a real error
      logger.log(Level.FINEST, "Error trace is not spurious");
      CounterexampleInfo cex = handleRealError(allStatesTrace, branchingOccurred, counterexample);

      totalRefinement.stop();
      return cex;
    }
  }

  /**
   * This method just calls buildCounterexampleTrace, however it reflects the
   * amount of calls to buildCounterexampleTrace that should be done according
   * to the configuration option.
   */
  protected CounterexampleTraceInfo buildCounterexampleTrace(Set<ARGState> elementsOnPath,
      final List<ARGState> abstractionStatesTrace, final List<BooleanFormula> formulas,
      boolean needsInterpolants) throws CPAException, InterruptedException {

    CounterexampleTraceInfo cex = null;
    for (int i = 0; i < checkCounterexampleNTimes; i++) {
      cex = formulaManager.buildCounterexampleTrace(formulas,
          Lists.<AbstractState>newArrayList(abstractionStatesTrace),
          elementsOnPath, needsInterpolants);
    }

    return cex;
  }

  /**
   * Creates a new CounterexampleInfo object out of the given parameters.
   */
  protected CounterexampleInfo handleRealError(
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

  static final Function<PredicateAbstractState, BooleanFormula> GET_BLOCK_FORMULA
                = new Function<PredicateAbstractState, BooleanFormula>() {
                    @Override
                    public BooleanFormula apply(PredicateAbstractState e) {
                      assert e.isAbstractionState();
                      return e.getAbstractionFormula().getBlockFormula().getFormula();
                    }
                  };

  /**
   * Get the block formulas from a path.
   * @param path A list of all abstraction elements
   * @param initialState The initial element of the analysis (= the root element of the ARG)
   * @return A list of block formulas for this path.
   */
  protected List<BooleanFormula> getFormulasForPath(List<ARGState> path, ARGState initialState)
      throws CPATransferException, InterruptedException, SolverException {
    getFormulasForPathTime.start();
    try {
      if (conjunctPreconditionFormulas) {
        ImmutableList<ARGState> predicateStates = from(path).toList();

        List<BooleanFormula> result = Lists.newArrayList();
        UnmodifiableIterator<ARGState> abstractionIt = predicateStates.iterator();

        final BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
        BooleanFormula traceFormula = bfmgr.makeBoolean(true);

        // each abstraction location has a corresponding block formula

        while (abstractionIt.hasNext()) {
          final ARGState argState = abstractionIt.next();

          final LocationState locState = AbstractStates.extractStateByType(argState, LocationState.class);
          final CFANode loc = locState.getLocationNode();

          final PredicateAbstractState predState = AbstractStates.extractStateByType(argState, PredicateAbstractState.class);
          assert predState.isAbstractionState();

          final BooleanFormula blockFormula = predState.getAbstractionFormula().getBlockFormula().getFormula();
          final SSAMap blockSsaMap = predState.getAbstractionFormula().getBlockFormula().getSsa();

          traceFormula = bfmgr.and(traceFormula, blockFormula);

          if (!BlockOperator.isFirstLocationInFunctionBody(loc) || solver.isUnsat(traceFormula)) { // Add the precondition only if the trace formula is SAT!!
            result.add(blockFormula);

          } else {
            final BooleanFormula eliminationResult = fmgr.eliminateDeadVariables(traceFormula, blockSsaMap);
            final BooleanFormula blockPrecondition = assumesStore.conjunctAssumeToLocation(loc, fmgr.makeNot(eliminationResult));

            result.add(bfmgr.and(blockFormula, blockPrecondition));
          }

        }
        return result;

      } else if (sliceBlockFormulas) {
        BlockFormulaSlicer bfs = new BlockFormulaSlicer(pfmgr);
        return bfs.sliceFormulasForPath(path, initialState);

      } else {
        return from(path)
            .transform(toState(PredicateAbstractState.class))
            .transform(GET_BLOCK_FORMULA)
            .toList();
      }
    } finally {
      getFormulasForPathTime.stop();
    }
  }

  private List<BooleanFormula> performRefinementSelection(final ARGPath pAllStatesTrace,
      final List<ARGState> pAbstractionStatesTrace)
      throws InterruptedException, CPAException, SolverException {

    prefixExtractionTime.start();
    List<InfeasiblePrefix> infeasiblePrefixes = prefixProvider.extractInfeasiblePrefixes(pAllStatesTrace);
    prefixExtractionTime.stop();

    totalPrefixes.setNextValue(infeasiblePrefixes.size());

    if (infeasiblePrefixes.isEmpty()) {
      return getFormulasForPath(pAbstractionStatesTrace, pAllStatesTrace.getFirstState());
    }

    else {
      PrefixSelector selector = new PrefixSelector(cfa.getVarClassification(), cfa.getLoopStructure());

      prefixSelectionTime.start();
      InfeasiblePrefix selectedPrefix = selector.selectSlicedPrefix(prefixPreference, infeasiblePrefixes);
      prefixSelectionTime.stop();

      List<BooleanFormula> formulas = selectedPrefix.getPathFormulae();
      while (formulas.size() < pAbstractionStatesTrace.size()) {
        formulas.add(solver.getFormulaManager().getBooleanFormulaManager().makeBoolean(true));
      }

      return formulas;
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }

  class Stats extends AbstractStatistics {

    private final Statistics statistics = strategy.getStatistics();

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);

      int numberOfRefinements = totalRefinement.getUpdateCount();
      if (numberOfRefinements > 0) {
        w0.put(totalPathLength)
          .put(totalPrefixes)
          .spacer()
          .put(totalRefinement);

        formulaManager.printStatistics(out);

        w0.beginLevel().put(errorPathProcessing);
        w0.beginLevel().put(getFormulasForPathTime);
        w0.beginLevel().put(prefixExtractionTime);
        w0.beginLevel().put(prefixSelectionTime);
      }

      statistics.printStatistics(out, result, reached);
    }

    @Override
    public String getName() {
      return strategy.getStatistics().getName();
    }
  }
}
