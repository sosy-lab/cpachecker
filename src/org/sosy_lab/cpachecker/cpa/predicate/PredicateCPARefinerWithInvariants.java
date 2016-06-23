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
import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Lists.newArrayList;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.LocationFormulaInvariant.makeLocationInvariant;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.LocationFormulaInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.cwriter.LoopCollectingEdgeVisitor;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;

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
public class PredicateCPARefinerWithInvariants extends PredicateCPARefiner {

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

  @Option(secure=true, description="use only atoms from generated invariants"
                                 + "as predicates, and not the whole invariant")
  private boolean atomicInvariants = false;

  @Option(secure=true, description="Should the automata used for invariant"
                                 + " generation be dumped to files?")
  private boolean dumpInvariantGenerationAutomata = false;

  @Option(secure=true,
      description="Where to dump the automata that are used to narrow the"
                + " analysis used for invariant generation.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathCounterTemplate dumpInvariantGenerationAutomataFile = PathCounterTemplate.ofFormatString("invgen.%d.spc");

  @Option(secure=true, description="Try to generate inductive invariants, by using"
      + " sliced prefixs and check if they are invariants. If this failes invariant"
      + " generation via other CPAs will be done or interpolation, depending"
      + " on the option 'fallbackToInvGen'.")
  private boolean useKInduction = true;

  @Option(secure=true, description="If k-induction fails to generate invariants"
      + " we fall back to the usual invariant generation with this option toggled,"
      + " otherwise interpolation is used")
  private boolean fallbackToInvGen = true;

  @Option(secure=true, description="configuration file for bmc generation")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path bmcConfig = Paths.get("config/bmc-invgen.properties");

  @Option(secure=true, description="How often should generating invariants from"
      + " sliced prefixes with k-induction be tried?")
  private int kInductionTries = 3;

  private Map<Loop, Integer> loopOccurrences = new HashMap<>();
  private boolean wereInvariantsGenerated = false;
  private Configuration config;

  // the previously analyzed counterexample to detect repeated counterexamples
  private List<CFANode> lastErrorPath = null;

  // statistics
  private final StatTimer totalInvariantGeneration = new StatTimer("Time for invariant generation");
  private final StatInt totalInvariantRefinements = new StatInt(StatKind.COUNT, "Number of invariants refinements");
  private final StatInt succInvariantRefinements = new StatInt(StatKind.COUNT, "Number of successful invariants refinements");
  private final StatInt totalInductiveRefinements = new StatInt(StatKind.COUNT, "Number of invariant refinements with k-induction");
  private final StatInt succInductiveRefinements = new StatInt(StatKind.COUNT, "Number of successful refinements with k-induction");
  private final StatInt totalRepeatedCounterexamples = new StatInt(StatKind.COUNT, "Number of repeated counterexamples");

  class Stats extends AbstractStatistics {

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);

      int numberOfRefinements = totalRefinement.getUpdateCount();
      if (numberOfRefinements > 0) {
        w0.put(totalRepeatedCounterexamples);
        w0.put(totalInvariantRefinements);
        w0.beginLevel().put(succInvariantRefinements);
        w0.beginLevel().put(totalInvariantGeneration);
        w0.beginLevel().put(totalInductiveRefinements);
        w0.beginLevel().beginLevel().put(succInductiveRefinements);
      }
    }

    @Override
    public String getName() {
      return strategy.getStatistics().getName() + " with Invariants";
    }
  }

  class InvCandidateGenerator implements CandidateGenerator {

    private int trieNum = 0;
    private List<CandidateInvariant> candidates = new ArrayList<>();

    private final ARGPath argPath;
    private final List<CFANode> abstractionNodes;
    private final Set<ARGState> elementsOnPath;
    private final List<ARGState> abstractionStatesTrace;
    private final List<InfeasiblePrefix> infeasiblePrefixes;
    private final List<LocationFormulaInvariant> foundInvariants = new ArrayList<>();

    private InvCandidateGenerator(ARGPath pPath, List<CFANode> pAbstractionNodes) throws CPAException, InterruptedException {
      argPath = pPath;
      abstractionNodes = pAbstractionNodes;
      elementsOnPath = extractElementsOnPath(argPath);
      abstractionStatesTrace = transformPath(argPath);

      prefixExtractionTime.start();
      infeasiblePrefixes = prefixProvider.extractInfeasiblePrefixes(argPath);
      prefixExtractionTime.stop();
    }

    @Override
    public boolean produceMoreCandidates() {
      if (trieNum >= kInductionTries) {
        return false;
      }

      if (infeasiblePrefixes.isEmpty()) {
        logger.log(Level.WARNING, "Could not create infeasible prefixes for invariant generation.");
        return false;
      }

      candidates = newArrayList(concat(from(infeasiblePrefixes).transform(TO_LOCATION_CANDIDATE_INVARIANT)));
      trieNum++;

      return true;
    }

    private final Function<InfeasiblePrefix, List<CandidateInvariant>> TO_LOCATION_CANDIDATE_INVARIANT
        = new Function<InfeasiblePrefix, List<CandidateInvariant>>() {
            @Override
            public List<CandidateInvariant> apply(InfeasiblePrefix pInput) {
              List<BooleanFormula> interpolants;
              try {
                interpolants = buildCounterexampleTrace(elementsOnPath, abstractionStatesTrace,
                    pInput.getPathFormulae(), true).getInterpolants();

              } catch (CPAException | InterruptedException e) {
                logger.logUserException(Level.WARNING, e, "Could not compute interpolants for k-induction inv-gen");
                return Collections.emptyList();
              }

              List<CandidateInvariant> invCandidates = new ArrayList<>();
              for (Pair<CFANode, BooleanFormula> nodeAndFormula : Pair.<CFANode, BooleanFormula>zipList(abstractionNodes, interpolants)) {
                invCandidates.add(makeLocationInvariant(nodeAndFormula.getFirst(), nodeAndFormula.getSecond()));
              }
              return invCandidates;
            }};

    @Override
    public boolean hasCandidatesAvailable() {
      return !candidates.isEmpty();
    }

    @Override
    public void confirmCandidates(Iterable<CandidateInvariant> pCandidates) {
      for (CandidateInvariant inv : pCandidates) {
        candidates.remove(inv);
        foundInvariants.add((LocationFormulaInvariant) inv);
      }
    }

    @Override
    public Iterator<CandidateInvariant> iterator() {
      if (trieNum == 0) {
        return Collections.<CandidateInvariant>emptyIterator();
      }
      return candidates.iterator();
    }

    public boolean hasFoundInvariants() {
      return !foundInvariants.isEmpty();
    }

    public List<BooleanFormula> retrieveConfirmedInvariants() {
      FluentIterable<LocationFormulaInvariant> found = from(foundInvariants);
      List<BooleanFormula> invariants = new ArrayList<>();
      for (final CFANode node : abstractionNodes) {
        invariants.add(
            found.filter(new Predicate<LocationFormulaInvariant>() {
                    @Override
                    public boolean apply(LocationFormulaInvariant pInput) {
                      return getOnlyElement(pInput.getLocations()).equals(node);
                    }})
                 .first()
                 .transform(new Function<LocationFormulaInvariant, BooleanFormula>() {
                    @Override
                    public BooleanFormula apply(LocationFormulaInvariant pInput) {
                      try {
                        return pInput.getFormula(fmgr, pfmgr);
                      } catch (CPATransferException | InterruptedException e) {
                        // this should never happen, if it does we log
                        // the exception and return TRUE as invariant
                        logger.logUserException(Level.WARNING, e, "Invariant could not be"
                            + " retrieved from InvariantGenerator");
                        return fmgr.getBooleanFormulaManager().makeBoolean(true);
                      }
                    }})
                 .or(fmgr.getBooleanFormulaManager().makeBoolean(true)));
      }

      // if we found invariants at least one of them may not be "TRUE"
      assert !from(invariants).allMatch(equalTo(fmgr.getBooleanFormulaManager().makeBoolean(true)));

      return invariants;
    }

  }

  public PredicateCPARefinerWithInvariants(final Configuration pConfig, final LogManager pLogger,
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

    super(pConfig, pLogger, pCpa, pInterpolationManager, pPathChecker, pPrefixProvider,
          pPathFormulaManager, pStrategy, pSolver, pAssumesStore, pCfa);

    pConfig.inject(this, PredicateCPARefinerWithInvariants.class);
    config = pConfig;
  }

  @Override
  public final CounterexampleInfo performRefinement(final ARGReachedSet pReached, final ARGPath allStatesTrace) throws CPAException, InterruptedException {
    final List<CFANode> errorPath = Lists.transform(allStatesTrace.asStatesList(), AbstractStates.EXTRACT_LOCATION);
    final boolean repeatedCounterexample = errorPath.equals(lastErrorPath);
    lastErrorPath = errorPath;

    if (repeatedCounterexample) {
      totalRepeatedCounterexamples.setNextValue(1);
    }

    // nothing was computed up to now, so just call refinement of
    // our super class if we have a repeated counter example
    // or we don't even need a precision increment
    if (repeatedCounterexample || !strategy.needsInterpolants()) {
      if (repeatedCounterexample && useStrongInvariantsOnly && wereInvariantsGenerated) {
          logger.log(Level.WARNING, "Repeated Countereample although generated invariants were strong"
              + " enough to refute it. Falling back to interpolation.");
      }

      // only interpolation or invariant-based refinements should be counted
      // as repeated error paths
      if (!strategy.needsInterpolants()) {
        lastErrorPath = null;
      }
      wereInvariantsGenerated = false;
      return super.performRefinement(pReached, allStatesTrace);
    }

    // get the relevant loops in the ARGPath and the number of occurrences of
    // the most often found one
    Set<Loop> loopsInPath = getRelevantLoops(allStatesTrace);
    int maxFoundLoop = getMaxCountOfOccuredLoop(loopsInPath);

    // no loops found, use normal interpolation refinement
    if (maxFoundLoop > maxInvariantGenerationsPerLoop || loopsInPath.isEmpty()) {
      wereInvariantsGenerated = false;
      return super.performRefinement(pReached, allStatesTrace);
    }

    // start refinement here, in the previous cases the time gets counted
    // in the super method
    totalInvariantGeneration.start();
    logger.log(Level.FINEST, "Starting invariant-generation-based refinement");

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

    // create list of formulas on path
    final List<BooleanFormula> formulas = createFormulasOnPath(allStatesTrace, abstractionStatesTrace);

    CounterexampleTraceInfo counterexample = buildCounterexampleTrace(elementsOnPath,
        abstractionStatesTrace, formulas, false);

    // if error is spurious refine
    if (counterexample.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

      totalInvariantRefinements.setNextValue(1);

      List<BooleanFormula> precisionIncrement = null;
      if (useKInduction) {
        precisionIncrement = generateInductiveInvariants(allStatesTrace, abstractionStatesTrace);
        totalInductiveRefinements.setNextValue(1);
      }

      if (!useKInduction || (precisionIncrement.isEmpty() && fallbackToInvGen)) {
        precisionIncrement = generateInvariants(allStatesTrace, abstractionStatesTrace, loopsInPath);

        // successful invariant generation with k-induction
      } else if (useKInduction && !precisionIncrement.isEmpty()) {
        succInductiveRefinements.setNextValue(1);
      }

      // fall-back to interpolation
      if (precisionIncrement.isEmpty()) {
        precisionIncrement = buildCounterexampleTrace(elementsOnPath, abstractionStatesTrace, formulas, true).getInterpolants();
      } else {
        succInvariantRefinements.setNextValue(1);
        wereInvariantsGenerated = true;
      }

      if (strategy instanceof PredicateAbstractionRefinementStrategy) {
        ((PredicateAbstractionRefinementStrategy)strategy).setUseAtomicPredicates(atomicInvariants);
      }

      strategy.performRefinement(pReached, abstractionStatesTrace, precisionIncrement, repeatedCounterexample);

      totalInvariantGeneration.stop();
      return CounterexampleInfo.spurious();

    } else {
      // we have a real error
      logger.log(Level.FINEST, "Error trace is not spurious");
      // we need interpolants for creating a precise error path
      counterexample = formulaManager.buildCounterexampleTrace(formulas,
          Lists.<AbstractState>newArrayList(abstractionStatesTrace),
          elementsOnPath,
          true);
      CounterexampleInfo cex = handleRealError(allStatesTrace, branchingOccurred, counterexample);

      totalInvariantGeneration.stop();
      return cex;
    }
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

  private List<BooleanFormula> generateInductiveInvariants(ARGPath pPath, List<ARGState> pAbstractionStatesTrace) throws CPAException, InterruptedException {
    InvCandidateGenerator candidateGenerator = new InvCandidateGenerator(pPath, from(pAbstractionStatesTrace)
                                                                                .transform(EXTRACT_LOCATION)
                                                                                .toList());
    ReachedSetFactory reached;
    try {
      reached = new ReachedSetFactory(config);

      ShutdownManager invariantShutdown = ShutdownManager.createWithParent(shutdownNotifier);
      ResourceLimitChecker limits = null;
      if (!timeForInvariantGeneration.isEmpty()) {
        WalltimeLimit l = WalltimeLimit.fromNowOn(timeForInvariantGeneration);
        limits = new ResourceLimitChecker(invariantShutdown, Collections.<ResourceLimit>singletonList(l));
        limits.start();
      }

      Configuration invariantConfig;
      try {
        invariantConfig = Configuration.builder().loadFromFile(bmcConfig).build();
      } catch (IOException e) {
        throw new InvalidConfigurationException("could not read configuration file for invariant generation: " + e.getMessage(), e);
      }

      KInductionInvariantGenerator invGen = KInductionInvariantGenerator.create(invariantConfig, logger, invariantShutdown, cfa, reached, candidateGenerator);

      invGen.start(cfa.getMainFunction());
      invGen.get(); // let invariant generator do the work

      List<BooleanFormula> invariants;

      if (candidateGenerator.hasFoundInvariants()) {
        // we do only want to use invariants that can be used to make the program safe
        if ((!useStrongInvariantsOnly || invGen.isProgramSafe())) {
          invariants = candidateGenerator.retrieveConfirmedInvariants();
        } else {
          invariants = Collections.emptyList();
          logger.log(Level.INFO, "Invariants found, but they are not strong enough to refute the counterexample");
        }
      } else {
        logger.log(Level.INFO, "No invariants were found.");
        invariants = Collections.emptyList();
      }

      if (!timeForInvariantGeneration.isEmpty()) {
        limits.cancel();
      }

      return invariants;

    } catch (InvalidConfigurationException | CPAException | InterruptedException e) {
      logger.logUserException(Level.WARNING, e, "Could not compute inductive invariants");
      return Collections.emptyList();
    }

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

        if (dumpInvariantGenerationAutomata) {
          Path logPath = dumpInvariantGenerationAutomataFile.getFreshPath();
          CharSink file = logPath.asCharSink(Charset.defaultCharset(), FileWriteMode.APPEND);
          file.openStream().append(spc).close();
        }

        Scope scope = cfa.getLanguage() == Language.C  ? new CProgramScope(cfa, logger)
                                                       : DummyScope.getInstance();

        List<Automaton> automata = AutomatonParser.parseAutomaton(new StringReader(spc.toString()),
                                                                  Optional.<Path>absent(),
                                                                  config, logger, cfa.getMachineModel(),
                                                                  scope, cfa.getLanguage());


        ShutdownManager invariantShutdown = ShutdownManager.createWithParent(shutdownNotifier);
        ResourceLimitChecker limits = null;
        if (!timeForInvariantGeneration.isEmpty()) {
          WalltimeLimit l = WalltimeLimit.fromNowOn(timeForInvariantGeneration);
          limits = new ResourceLimitChecker(invariantShutdown, Collections.<ResourceLimit>singletonList(l));
          limits.start();
        }

        InvariantGenerator invGen = CPAInvariantGenerator.create(config, logger, invariantShutdown, Optional.<ShutdownManager>absent(), cfa, automata);

        List<BooleanFormula> invariants = generateInvariants0(abstractionStatesTrace, invGen);

        if (!timeForInvariantGeneration.isEmpty()) {
          limits.cancel();
        }

        return invariants;

      } catch (InvalidConfigurationException | IOException | CPAException | InterruptedException e) {
        logger.logUserException(Level.WARNING, e, "Could not compute invariants");
        return Collections.emptyList();
      }
  }

  private List<BooleanFormula> generateInvariants0(final List<ARGState> abstractionStatesTrace,
      InvariantGenerator invGen) throws CPAException, InterruptedException {

    invGen.start(cfa.getMainFunction());
    InvariantSupplier invSup = invGen.get();

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
        return Collections.emptyList();
      }

      return invariants;

    } else {
      logger.log(Level.INFO, "Invariants found, but they are not strong enough to refute the counterexample");
      return Collections.emptyList();
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(new Stats());
  }
}
