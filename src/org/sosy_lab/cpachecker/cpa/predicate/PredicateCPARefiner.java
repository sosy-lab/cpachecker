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
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier.PrefixPreference;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.UseDefRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.PrefixProvider;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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

  @Option(secure=true,
      description="where to dump the counterexample formula in case the error location is reached")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpCounterexampleFile = PathTemplate.ofFormatString("ErrorPath.%d.smt2");

  @Option(secure=true, description="which sliced prefix should be used for interpolation")
  private PrefixPreference prefixPreference = PrefixPreference.DEFAULT;

  @Option(secure=true, description="whether or not to perform path slicing before interpolation")
  private boolean pathSlicing = false;

  @Option(secure=true, description="defines how to handle feasible assume edges during path slicing", values={"NONE", "EQUALITY", "ALL"})
  private String handleFeasibleAssumeEdges = "NONE";

  Configuration config;

  // the previously analyzed counterexample to detect repeated counterexamples
  private List<BooleanFormula> lastErrorPath = null;

  // statistics
  private final StatInt totalPathLength = new StatInt(StatKind.AVG, "Avg. length of target path (in blocks)"); // measured in blocks
  private final StatTimer totalRefinement = new StatTimer("Time for refinement");
  private final StatTimer errorPathProcessing = new StatTimer("Error path post-processing");
  private final StatTimer getFormulasForPathTime = new StatTimer("Path-formulas extraction");
  private final StatTimer buildCounterexampeTraceTime = new StatTimer("Building the counterexample trace");
  private final StatTimer preciseCouterexampleTime = new StatTimer("Extracting precise counterexample");

  class Stats extends AbstractStatistics {

    private final Statistics statistics = strategy.getStatistics();

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);

      int numberOfRefinements = totalRefinement.getUpdateCount();
      if (numberOfRefinements > 0) {
        w0.put(totalPathLength)
          .spacer()
          .put(totalRefinement);

        formulaManager.printStatistics(out, result, reached);

        w0.beginLevel().put(errorPathProcessing);
        w0.beginLevel().put(getFormulasForPathTime);
        w0.beginLevel().put(buildCounterexampeTraceTime);
        w0.beginLevel().put(preciseCouterexampleTime);
      }

      statistics.printStatistics(out, result, reached);
    }

    @Override
    public String getName() {
      return strategy.getStatistics().getName();
    }
  }

  private final LogManager logger;

  protected final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final InterpolationManager formulaManager;
  private final PathChecker pathChecker;
  private final RefinementStrategy strategy;
  private final Solver solver;
  private final PredicateAssumeStore assumesStore;
  private final CFA cfa;

  public PredicateCPARefiner(final Configuration pConfig, final LogManager pLogger,
      final ConfigurableProgramAnalysis pCpa,
      final InterpolationManager pInterpolationManager,
      final PathChecker pPathChecker,
      final PathFormulaManager pPathFormulaManager,
      final RefinementStrategy pStrategy,
      final Solver pSolver,
      final PredicateAssumeStore pAssumesStore,
      final CFA pCfa)
          throws CPAException, InvalidConfigurationException {

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

    config = pConfig;

    logger.log(Level.INFO, "Using refinement for predicate analysis with " + strategy.getClass().getSimpleName() + " strategy.");
  }

  @Override
  public final CounterexampleInfo performRefinement(final ARGReachedSet pReached, ARGPath allStatesTrace) throws CPAException, InterruptedException {
    totalRefinement.start();

    if (isRefinementSelectionEnabled(allStatesTrace)) {
      allStatesTrace = performRefinementSelection(allStatesTrace);
    }

    Set<ARGState> elementsOnPath = ARGUtils.getAllStatesOnPathsTo(allStatesTrace.getLastState());
    assert elementsOnPath.containsAll(allStatesTrace.getStateSet());
    assert elementsOnPath.size() >= allStatesTrace.size();

    boolean branchingOccurred = true;
    if (elementsOnPath.size() == allStatesTrace.size()) {
      // No branches/merges in path, it is precise.
      // We don't need to care about creating extra predicates for branching etc.
      elementsOnPath = Collections.emptySet();
      branchingOccurred = false;
    }

    logger.log(Level.FINEST, "Starting interpolation-based refinement");
    // create path with all abstraction location elements (excluding the initial element)
    // the last element is the element corresponding to the error location
    final List<ARGState> abstractionStatesTrace = transformPath(allStatesTrace);
    totalPathLength.setNextValue(abstractionStatesTrace.size());

    logger.log(Level.ALL, "Abstraction trace is", abstractionStatesTrace);

    // create list of formulas on path
    final List<BooleanFormula> formulas = (isRefinementSelectionEnabled(allStatesTrace))
      ? recomputePathFormulae(allStatesTrace)
      : getFormulasForPath(abstractionStatesTrace, allStatesTrace.getFirstState());

    assert abstractionStatesTrace.size() == formulas.size();
    // a user would expect "abstractionStatesTrace.size() == formulas.size()+1",
    // however we do not have the very first state in the trace,
    // because the rootState has always abstraction "True".

    logger.log(Level.ALL, "Error path formulas: ", formulas);

    // build the counterexample
    buildCounterexampeTraceTime.start();
    final CounterexampleTraceInfo counterexample = formulaManager.buildCounterexampleTrace(
            formulas, Lists.<AbstractState>newArrayList(abstractionStatesTrace), elementsOnPath, strategy.needsInterpolants());
    buildCounterexampeTraceTime.stop();

    // if error is spurious refine
    if (counterexample.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

      boolean repeatedCounterexample = formulas.equals(lastErrorPath);
      lastErrorPath = formulas;

      strategy.performRefinement(pReached, abstractionStatesTrace, counterexample.getInterpolants(), repeatedCounterexample);

      totalRefinement.stop();
      return CounterexampleInfo.spurious();

    } else {
      // we have a real error
      logger.log(Level.FINEST, "Error trace is not spurious");
      final ARGPath targetPath;
      final CounterexampleTraceInfo preciseCounterexample;

      preciseCouterexampleTime.start();
      if (branchingOccurred) {
        Pair<ARGPath, CounterexampleTraceInfo> preciseInfo = findPreciseErrorPath(allStatesTrace, counterexample);

        if (preciseInfo != null) {
          targetPath = preciseInfo.getFirst();
          if (preciseInfo.getSecond() != null) {
            preciseCounterexample = preciseInfo.getSecond();
          } else {
            logger.log(Level.WARNING, "The satisfying assignment may be imprecise!");
            preciseCounterexample = counterexample;
          }
        } else {
          logger.log(Level.WARNING, "The error path and the satisfying assignment may be imprecise!");
          targetPath = allStatesTrace;
          preciseCounterexample = counterexample;
        }
      } else {
        targetPath = allStatesTrace;
        preciseCounterexample = addVariableAssignmentToCounterexample(counterexample, targetPath);
      }
      preciseCouterexampleTime.stop();

      CounterexampleInfo cex = CounterexampleInfo.feasible(targetPath, preciseCounterexample.getModel());
      cex.addFurtherInformation(formulaManager.dumpCounterexample(preciseCounterexample),
          dumpCounterexampleFile);

      totalRefinement.stop();
      return cex;
    }
  }

  private ARGPath performRefinementSelection(ARGPath allStatesTrace) throws CPAException, InterruptedException {
    PrefixProvider provider = new PredicateBasedPrefixProvider(logger, solver, pfmgr);
    List<ARGPath> infeasilbePrefixes = provider.getInfeasilbePrefixes(allStatesTrace);

    if(allStatesTrace != infeasilbePrefixes.get(0)) {
      ErrorPathClassifier classifier = new ErrorPathClassifier(cfa.getVarClassification(), cfa.getLoopStructure());

      allStatesTrace = classifier.obtainSlicedPrefix(prefixPreference, allStatesTrace, infeasilbePrefixes);

      if (pathSlicing) {
        ARGPath slicedAllStatesTrace = sliceErrorPath(allStatesTrace);

        PathFormula formula = pfmgr.makeEmptyPathFormula();
        PathIterator iterator = slicedAllStatesTrace.pathIterator();
        while (iterator.hasNext()) {
          if(iterator.getOutgoingEdge().getEdgeType() != CFAEdgeType.BlankEdge) {
            formula = pfmgr.makeAnd(formula, iterator.getOutgoingEdge());
          }
          iterator.advance();
        }

        if(solver.isUnsat(formula.getFormula())) {
          allStatesTrace = slicedAllStatesTrace;
        }
      }
    }
    return allStatesTrace;
  }

  /**
   * This method removes further edges from the error path (prefix).
   */
  private ARGPath sliceErrorPath(final ARGPath errorPathPrefix) {

    Set<ARGState> useDefStates = new UseDefRelation(errorPathPrefix,
        cfa.getVarClassification().isPresent()
          ? cfa.getVarClassification().get().getIntBoolVars()
          : Collections.<String>emptySet(),
        handleFeasibleAssumeEdges).getUseDefStates();

    ArrayList<CFAEdge> abstractEdges = Lists.newArrayList(errorPathPrefix.asEdgesList());

    PathIterator iterator = errorPathPrefix.pathIterator();
    while (iterator.hasNext()) {

      // slice edge if there is neither a use nor a definition at the current state
      if (!useDefStates.contains(iterator.getAbstractState())) {
        abstractEdges.set(iterator.getIndex(), new BlankEdge("",
            FileLocation.DUMMY,
            iterator.getOutgoingEdge().getPredecessor(),
            iterator.getOutgoingEdge().getSuccessor(),
            ErrorPathClassifier.SUFFIX_REPLACEMENT));
      }

      iterator.advance();
    }

    return new ARGPath(errorPathPrefix.asStatesList(), abstractEdges);
  }

  /**
   * This method determines whether or not to perform refinement selection.
   *
   * For this to be possible, a prefix preference has to be set, and the analysis
   * has to be configured such that every state is an abstraction state (i.e, SBE).
   *
   * @param errorPath to check whether or not block-encoding is set to SBE
   * @return true, if refinement selection has to be performed, else false
   */
  private boolean isRefinementSelectionEnabled(ARGPath errorPath) {
    return prefixPreference != PrefixPreference.DEFAULT
        && (errorPath.size() - transformPath(errorPath).size()) == 1;
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

  private static final Function<PredicateAbstractState, BooleanFormula> GET_BLOCK_FORMULA
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
   * @throws SolverException
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

  private List<BooleanFormula> recomputePathFormulae(ARGPath pAllStatesTrace)
      throws CPATransferException, InterruptedException {
    ArrayList<BooleanFormula> list = new ArrayList<>(pAllStatesTrace.size() - 1);

    PathFormula pathFormula = pfmgr.makeEmptyPathFormula();
    for (CFAEdge edge : pAllStatesTrace.getInnerEdges()) {
      pathFormula = pfmgr.makeAnd(pfmgr.makeEmptyPathFormula(pathFormula), edge);
      list.add(pathFormula.getFormula());
    }

    assert(list.size() == (pAllStatesTrace.size() - 1));

    return list;
  }

  private Pair<ARGPath, CounterexampleTraceInfo> findPreciseErrorPath(ARGPath pPath, CounterexampleTraceInfo counterexample) throws InterruptedException {
    errorPathProcessing.start();
    try {
      Map<Integer, Boolean> preds = counterexample.getBranchingPredicates();
      if (preds.isEmpty()) {
        logger.log(Level.WARNING, "No information about ARG branches available!");
        return null;
      }

      // find correct path
      ARGPath targetPath;
      try {
        ARGState root = pPath.getFirstState();
        ARGState target = pPath.getLastState();
        Set<ARGState> pathElements = ARGUtils.getAllStatesOnPathsTo(target);

        targetPath = ARGUtils.getPathFromBranchingInformation(root, target,
            pathElements, preds);

      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, null);
        return null;
      }

      // try to create a better satisfying assignment by replaying this single path
      CounterexampleTraceInfo info2;
      try {
        info2 = pathChecker.checkPath(targetPath.getInnerEdges());

      } catch (SolverException | CPATransferException e) {
        // path is now suddenly a problem
        logger.logUserException(Level.WARNING, e, "Could not replay error path");
        return null;
      }

      if (info2.isSpurious()) {
        logger.log(Level.WARNING, "Inconsistent replayed error path!");
        return Pair.of(targetPath, null);
      } else {
        return Pair.of(targetPath, info2);
      }

    } finally {
      errorPathProcessing.stop();
    }
  }

  private CounterexampleTraceInfo addVariableAssignmentToCounterexample(
      final CounterexampleTraceInfo counterexample, final ARGPath targetPath) throws CPATransferException, InterruptedException {

    List<CFAEdge> edges = targetPath.getInnerEdges();

    List<SSAMap> ssamaps = pathChecker.calculatePreciseSSAMaps(edges);

    Model model = counterexample.getModel();

    Pair<CFAPathWithAssumptions, Multimap<CFAEdge, AssignableTerm>> pathAndTerms =
        pathChecker.extractVariableAssignment(edges, ssamaps, model);

    CFAPathWithAssumptions pathWithAssignments = pathAndTerms.getFirst();
    Multimap<CFAEdge, AssignableTerm> termsPerEdge = pathAndTerms.getSecond();

    model = model.withAssignmentInformation(pathWithAssignments, termsPerEdge);

    return CounterexampleTraceInfo.feasible(counterexample.getCounterExampleFormulas(), model, counterexample.getBranchingPredicates());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }
}
