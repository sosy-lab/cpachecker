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

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.RichModel;
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
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.cwriter.LoopCollectingEdgeVisitor;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.solver.AssignableTerm;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Function;
import com.google.common.base.Optional;
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
  private PrefixPreference prefixPreference = PrefixPreference.NONE;

  @Option(secure=true, name="useInvariantRefinement",
      description="Should the refinement be done with invariants instead of"
          + " interpolation? This is currently a heuristic as we cannot be "
          + "sure that all invariants are good enough to refute a counterexample"
          + " therefore the fallback is still interpolation.")
  private boolean useInvariantRefinement = false;

  @Option(secure=true, description="Timelimit for invariant generation which may be"
                                 + " used during refinement.\n"
                                 + "(Use seconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
                  defaultUserUnit=TimeUnit.SECONDS,
                  min=0)
  private TimeSpan timeForInvariantGeneration = TimeSpan.ofNanos(0);

  @Option(secure=true, description="For differing errorpaths, the loop for which"
      + " invariants should be generated may still be the same, with this option"
      + " you can set the maximal amount of invariant generation runs per loop."
      + " 0 means no upper limit given.")
  private int maxInvariantGenerationsPerLoop = 2;

  @Option(secure=true, description="Invariants that are not strong enough to"
      + " refute the counterexample can be ignored with this option."
      + " (Weak invariants will lead to repeated counterexamples, thus taking"
      + " time which could be used for the rest of the analysis, however, the"
      + " found invariants may also be better for loops as interpolation.)")
  private boolean useStrongInvariantsOnly = true;

  @Option(secure=true, description="use only the atoms from the interpolants"
                                 + "as predicates, and not the whole interpolant")
  private boolean atomicInterpolants = true;

  @Option(secure=true, description="use only atoms from generated invariants"
                                 + "as predicates, and not the whole invariant")
  private boolean atomicInvariants = false;

  private Map<Loop, Integer> loopOccurrences = new HashMap<>();
  private boolean wereInvariantsGenerated = false;

  Configuration config;

  // the previously analyzed counterexample to detect repeated counterexamples
  private List<CFANode> lastErrorPath = null;

  // statistics
  private final StatInt totalPathLength = new StatInt(StatKind.AVG, "Avg. length of target path (in blocks)"); // measured in blocks
  private final StatTimer totalRefinement = new StatTimer("Time for refinement");
  private final StatTimer totalInvariantGeneration = new StatTimer("Time for invariant generation");
  private final StatTimer errorPathProcessing = new StatTimer("Error path post-processing");
  private final StatTimer getFormulasForPathTime = new StatTimer("Path-formulas extraction");
  private final StatTimer buildCounterexampeTraceTime = new StatTimer("Building the counterexample trace");
  private final StatTimer preciseCouterexampleTime = new StatTimer("Extracting precise counterexample");

  private final StatInt totalPrefixes = new StatInt(StatKind.SUM, "Number of infeasible sliced prefixes");
  private final StatInt totalSuccessfulInvariantRefinements = new StatInt(StatKind.COUNT, "Number of successful invariant refinements");
  private final StatTimer prefixExtractionTime = new StatTimer("Extracting infeasible sliced prefixes");
  private final StatTimer prefixSelectionTime = new StatTimer("Selecting infeasible sliced prefixes");

  class Stats extends AbstractStatistics {

    private final Statistics statistics = strategy.getStatistics();

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);

      int numberOfRefinements = totalRefinement.getUpdateCount();
      if (numberOfRefinements > 0) {
        w0.put(totalPathLength)
          .put(totalPrefixes)
          .put(totalSuccessfulInvariantRefinements)
          .spacer()
          .put(totalRefinement);

        formulaManager.printStatistics(out, result, reached);

        w0.beginLevel().put(errorPathProcessing);
        w0.beginLevel().put(getFormulasForPathTime);
        w0.beginLevel().put(buildCounterexampeTraceTime);
        w0.beginLevel().put(totalInvariantGeneration);
        w0.beginLevel().put(preciseCouterexampleTime);
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

  private final LogManager logger;

  protected final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final InterpolationManager formulaManager;
  private final PrefixProvider prefixProvider;
  private final PathChecker pathChecker;
  private final RefinementStrategy strategy;
  private final Solver solver;
  private final PredicateAssumeStore assumesStore;
  private final CFA cfa;
//  private final ARGPathExport witnessExporter;
//  private final AutomatonGraphmlParser witnessParser;
  private final ShutdownNotifier shutdownNotifier;

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
//    witnessExporter = new ARGPathExport(pConfig, logger, cfa.getMachineModel(), Language.C);
//    witnessParser = new AutomatonGraphmlParser(pConfig, logger, cfa.getMachineModel(), new CProgramScope(cfa, logger));

    prefixProvider = pPrefixProvider;

    config = pConfig;

    logger.log(Level.INFO, "Using refinement for predicate analysis with " + strategy.getClass().getSimpleName() + " strategy.");
  }

  @Override
  public final CounterexampleInfo performRefinement(final ARGReachedSet pReached, final ARGPath allStatesTrace) throws CPAException, InterruptedException {
    totalRefinement.start();

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

    logger.log(Level.FINEST, "Starting interpolation/invariant-based refinement");
    // create path with all abstraction location elements (excluding the initial element)
    // the last element is the element corresponding to the error location
    final List<ARGState> abstractionStatesTrace = transformPath(allStatesTrace);
    totalPathLength.setNextValue(abstractionStatesTrace.size());

    logger.log(Level.ALL, "Abstraction trace is", abstractionStatesTrace);

    // create list of formulas on path
    final List<BooleanFormula> formulas;
    try {
      formulas = (isRefinementSelectionEnabled())
        ? performRefinementSelection(allStatesTrace, abstractionStatesTrace)
        : getFormulasForPath(abstractionStatesTrace, allStatesTrace.getFirstState());
    } catch (SolverException e) {
      throw new CPAException("Solver Exception", e);
    }

    assert abstractionStatesTrace.size() == formulas.size() : abstractionStatesTrace.size() + " != " + formulas.size();
    // a user would expect "abstractionStatesTrace.size() == formulas.size()+1",
    // however we do not have the very first state in the trace,
    // because the rootState has always abstraction "True".

    logger.log(Level.ALL, "Error path formulas: ", formulas);

    final List<CFANode> errorPath = Lists.transform(allStatesTrace.asStatesList(), AbstractStates.EXTRACT_LOCATION);
    final boolean repeatedCounterexample = errorPath.equals(lastErrorPath);
    if (! ((useStrongInvariantsOnly && wereInvariantsGenerated && !repeatedCounterexample)
           || (useStrongInvariantsOnly && !wereInvariantsGenerated)
           || !useStrongInvariantsOnly)) {
      logger.log(Level.WARNING, "Repeated Countereample although generated invariants were strong enough to refute it.");
    }

    if (wereInvariantsGenerated && !repeatedCounterexample) {
      totalSuccessfulInvariantRefinements.setNextValue(1);
    }
    lastErrorPath = errorPath;

    // get the relevant loops in the ARGPath and the number of occurences of
    // the most often found one
    Set<Loop> loopsInPath = getRelevantLoops(allStatesTrace);
    int maxFoundLoop = getMaxCountOfOccuredLoop(loopsInPath);

    CounterexampleTraceInfo counterexample =
        buildCounterexampleTrace(elementsOnPath, abstractionStatesTrace, formulas,
                                 repeatedCounterexample, loopsInPath, maxFoundLoop);

    // if error is spurious refine
    if (counterexample.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

      List<BooleanFormula> precisionIncrement =
          computePrecisionIncrement(allStatesTrace, elementsOnPath, abstractionStatesTrace,
                                    formulas, loopsInPath, maxFoundLoop, counterexample);

      if (strategy instanceof PredicateAbstractionRefinementStrategy) {
        ((PredicateAbstractionRefinementStrategy)strategy).setUseAtomicPredicates(wereInvariantsGenerated ? atomicInvariants : atomicInterpolants);
      }

      strategy.performRefinement(pReached, abstractionStatesTrace, precisionIncrement, repeatedCounterexample);

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
   * Creates a new CounterexampleInfo object out of the given parameters.
   */
  private CounterexampleInfo handleRealError(final ARGPath allStatesTrace, boolean branchingOccurred,
      CounterexampleTraceInfo counterexample) throws InterruptedException, CPATransferException {
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
    return cex;
  }

  /**
   * Computes the precision increment out of the given parameters. Based on
   * some configuration options, either invariant refinement or earlier computed
   * interpolants will be used.
   */
  private List<BooleanFormula> computePrecisionIncrement(final ARGPath allStatesTrace, Set<ARGState> elementsOnPath,
      final List<ARGState> abstractionStatesTrace, final List<BooleanFormula> formulas, Set<Loop> loopsInPath,
      int maxFoundLoop, CounterexampleTraceInfo counterexample) throws CPAException, InterruptedException {
    List<BooleanFormula> precisionIncrement;

    if (counterexample.getInterpolants() == null && strategy.needsInterpolants()) {
        assert  useInvariantRefinement // if we are here invariants need to be used
                && maxFoundLoop <= maxInvariantGenerationsPerLoop // respect the configuration option
                && !loopsInPath.isEmpty(): // invariants only make sense for error paths with loops
                 "No interpolants were computed, although they are needed,"
                 + " check #buildCounterExampleTrace for logic-problems." ;

      logger.log(Level.INFO, "Using invariant generation for refinement");
      totalInvariantGeneration.start();
      precisionIncrement = generateInvariants(allStatesTrace, abstractionStatesTrace, loopsInPath);
      totalInvariantGeneration.stop();

      // invariant generation was not successful, fall-back to interpolation
      if (precisionIncrement == null) {
        logger.log(Level.INFO, "Invariant generation failed, falling back to interpolation.");
        counterexample = formulaManager.buildCounterexampleTrace(formulas,
            Lists.<AbstractState>newArrayList(abstractionStatesTrace),
            elementsOnPath, true);
        precisionIncrement = counterexample.getInterpolants();
        wereInvariantsGenerated = false;
      } else {
        wereInvariantsGenerated = true;
      }

      // using interpolants
    } else {
      logger.log(Level.INFO, "Using interpolation for refinement");
      precisionIncrement = counterexample.getInterpolants();
      wereInvariantsGenerated = false;
    }
    return precisionIncrement;
  }

  /**
   * Builds the CounterexampleTraceInfo object out of the given information.
   * Depending on some configuration options (e.g. invariant refinement usage)
   * and the refinement strategy interpolants may be computed during this step.
   */
  private CounterexampleTraceInfo buildCounterexampleTrace(Set<ARGState> elementsOnPath,
      final List<ARGState> abstractionStatesTrace, final List<BooleanFormula> formulas,
      final boolean repeatedCounterexample, Set<Loop> loopsInPath, int maxFoundLoop)
          throws CPAException, InterruptedException {
    // build the counterexample, we only need interpolants if the cexPath has
    // not occured twice
    buildCounterexampeTraceTime.start();
    CounterexampleTraceInfo counterexample;

    // last refinement was static or we have a new counterexample, so we can try
    // with invariants and therefore don't need interpolants
    if (useInvariantRefinement
        && maxFoundLoop <= maxInvariantGenerationsPerLoop // respect limit from configuration option
        && !loopsInPath.isEmpty()                 // invariants make sense only for loops
        && ((strategy instanceof PredicateAbstractionRefinementStrategy
                 && ((PredicateAbstractionRefinementStrategy)strategy).wasLastRefinementStatic())
            || !repeatedCounterexample)) {
      counterexample = formulaManager.buildCounterexampleTrace(formulas,
          Lists.<AbstractState>newArrayList(abstractionStatesTrace),
          elementsOnPath,
          false);

      // this is a repeated counterexample so we ask the strategy about
      // the interpolation needs
    } else {
      counterexample = formulaManager.buildCounterexampleTrace(formulas,
          Lists.<AbstractState>newArrayList(abstractionStatesTrace),
          elementsOnPath,
          strategy.needsInterpolants());
    }

    buildCounterexampeTraceTime.stop();
    return counterexample;
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
      loopFinder = new LoopCollectingEdgeVisitor(cfa.getLoopStructure().get(), config);
    } catch (InvalidConfigurationException e1) {
      // this will never happen, but for the case it does, we just return
      // the empty set, therefore the refinement will be done without invariant
      // generation definitely and only with interpolation / static refinement
      return Collections.emptySet();
    }

    while(pathIt.hasNext()) {
      loopFinder.visit(pathIt.getAbstractState(), pathIt.getOutgoingEdge(), null);
      pathIt.advance();
    }

    return loopFinder.getRelevantLoops().keySet();
  }

  /**
   * This method generates the invariants used for refinement.
   * @return the list of generated invariants or null
   */
  private List<BooleanFormula> generateInvariants(final ARGPath allStatesTrace,
      final List<ARGState> abstractionStatesTrace, final Set<Loop> pLoopsInPath) {

      try {
        StringBuilder spc = new StringBuilder();
        ARGUtils.producePathAutomatonWithLoops(spc, allStatesTrace.getFirstState(), allStatesTrace.getStateSet(), "invGen", pLoopsInPath);

        Scope scope = cfa.getLanguage() == Language.C  ? new CProgramScope(cfa, logger)
                                                       : DummyScope.getInstance();

        List<Automaton> automata = AutomatonParser.parseAutomaton(new StringReader(spc.toString()),
                                                                  Optional.<Path>absent(),
                                                                  config, logger, cfa.getMachineModel(),
                                                                  scope, cfa.getLanguage());

        ShutdownNotifier notifier = shutdownNotifier;
        ResourceLimitChecker limits = null;
        if (!timeForInvariantGeneration.isEmpty()) {
          notifier = ShutdownNotifier.createWithParent(shutdownNotifier);
          WalltimeLimit l = WalltimeLimit.fromNowOn(timeForInvariantGeneration);
          limits = new ResourceLimitChecker(notifier, Lists.newArrayList((ResourceLimit)l));
          limits.start();
        }

        CPAInvariantGenerator invGen = CPAInvariantGenerator.create(config, logger, notifier, Optional.<ShutdownNotifier>absent(), cfa, automata);
        invGen.start(cfa.getMainFunction());
        InvariantSupplier invSup = invGen.get();

        if (!timeForInvariantGeneration.isEmpty()) {
          limits.cancel();
        }

        // we do only want to use invariants that can be used to make the program safe
        if (!useStrongInvariantsOnly || invGen.isProgramSafe()) {
          List<BooleanFormula> invariants = new ArrayList<>();
          for (ARGState s : abstractionStatesTrace) {
            // the last one will always be false, we don't need it here
            if (s != abstractionStatesTrace.get(abstractionStatesTrace.size()-1)) {
              invariants.add(invSup.getInvariantFor(extractLocation(s), fmgr, pfmgr));
              logger.log(Level.ALL, "Precision increment for location", extractLocation(s), "is", invSup.getInvariantFor(extractLocation(s), fmgr, pfmgr));
            }
          }

          if (from(invariants).allMatch(equalTo(fmgr.getBooleanFormulaManager().makeBoolean(true)))) {
            logger.log(Level.FINEST, "All invariants were TRUE, ignoring result.");
            return null;
          }

          return invariants;

        } else {
          logger.log(Level.INFO, "Invariants found, but they are not strong enough to refute the counterexample");
          return null;
        }

      } catch (InvalidConfigurationException | IOException | CPAException | InterruptedException e) {
        logger.log(Level.WARNING, "Could not compute invariants", e);
        return null;
      }
  }

  /**
   * This method determines whether or not to perform refinement selection.
   *
   * @return true, if refinement selection has to be performed, else false
   */
  private boolean isRefinementSelectionEnabled() {
    return prefixPreference != PrefixPreference.NONE;
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

    RichModel model = counterexample.getModel();

    Pair<CFAPathWithAssumptions, Multimap<CFAEdge, AssignableTerm>> pathAndTerms =
        pathChecker.extractVariableAssignment(edges, ssamaps, model);

    CFAPathWithAssumptions pathWithAssignments = pathAndTerms.getFirst();

    model = model.withAssignmentInformation(pathWithAssignments);
    return CounterexampleTraceInfo.feasible(counterexample.getCounterExampleFormulas(), model, counterexample.getBranchingPredicates());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }
}
