/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.AbstractLocationFormulaInvariant.makeLocationInvariant;
import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getAllStatesOnPathsTo;
import static org.sosy_lab.cpachecker.util.AbstractStates.EXTRACT_LOCATION;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.FormatMethod;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.ForwardingLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.bmc.AbstractLocationFormulaInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.StaticCandidateProvider;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantChecker;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.cpa.formulaslicing.LoopTransitionFinder;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.RCNFManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.invariants.FormulaInvariantsSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@Options(prefix = "cpa.predicate.invariants", deprecatedPrefix = "cpa.predicate")
class PredicateCPAInvariantsManager implements StatisticsProvider, InvariantSupplier {

  private static enum InvariantGenerationStrategy {
    /**
     * Applies inductive weakening on the pathformula (which is converted to a CNF like form)
     * to filter out the invariant part.
     */
    PF_INDUCTIVE_WEAKENING,

    /**
     * Converts the pathformula to a CNF like form and checks the conjuncts on
     * invariants with k-Induction.
     */
    PF_CNF_KIND,

    /**
     * Creates interpolants for a given error path and checks them on invariance
     * with k-Induction.
     */
    RF_INTERPOLANT_KIND,
  }

  @Option(
    secure = true,
    description =
        "Which strategy should be used for generating invariants, a comma separated"
            + " list can be specified. Usually later specified strategies serve as"
            + " fallback for earlier ones. (default is no invariant generation at all)"
  )
  private List<InvariantGenerationStrategy> generationStrategy = new ArrayList<>();

  @Option(
    secure = true,
    description =
        "Should the strategies be used all-together or only as fallback. If all together,"
            + " the computation is done until the timeout is hit and the results up to this"
            + " point are taken."
  )
  private boolean useAllStrategies = false;

  @Option(
    secure = true,
    description =
        "Timelimit for invariant generation which may be used during refinement.\n"
            + "(Use seconds or specify a unit; 0 for infinite)"
  )
  @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = 0)
  private TimeSpan timeForInvariantGeneration = TimeSpan.ofSeconds(10);

  @Option(
    secure = true,
    description =
        "Invariants that are not strong enough to refute the counterexample can be ignored"
            + " with this option. (Weak invariants will lead to repeated counterexamples,"
            + " thus taking time which could be used for the rest of the analysis, however,"
            + " the found invariants may also be better for loops as interpolation.)"
  )
  private boolean useStrongInvariantsOnly = true;

  @Option(
    secure = true,
    description = "Should the automata used for invariant generation be dumped to files?"
  )
  private boolean dumpInvariantGenerationAutomata = false;

  @Option(
    secure = true,
    deprecatedName = "useInvariantsForAbstraction",
    description =
        "Strengthen the pathformula during abstraction with invariants if some are generated. Invariants"
            + " do not need to be generated with the PredicateCPA they can also be given from outside."
  )
  private boolean appendToPathFormula = false;

  @Option(
    secure = true,
    description =
        "Strengthen the abstraction formula during abstraction with invariants if some are generated. Invariants"
            + " do not need to be generated with the PredicateCPA they can also be given from outside."
  )
  private boolean appendToAbstractionFormula = false;

  @Option(
    secure = true,
    description =
        "Add computed invariants to the precision. Invariants"
            + " do not need to be generated with the PredicateCPA they can also be given from outside."
  )
  private boolean addToPrecision = false;

  @Option(
    secure = true,
    description =
        "Provide invariants generated with other analyses via the PredicateCPAInvariantsManager."
  )
  private boolean useGlobalInvariants = true;

  @Option(
    secure = true,
    description =
        "Where to dump the automata that are used to narrow the analysis used for"
            + " invariant generation."
  )
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathCounterTemplate dumpInvariantGenerationAutomataFile =
      PathCounterTemplate.ofFormatString("invgen.%d.spc");

  @Option(
    secure = true,
    description =
        "How often should generating invariants from sliced prefixes with k-induction be tried?"
  )
  private int kInductionTries = 3;

  private Solver solver;
  private FormulaManagerView fmgr;
  private BooleanFormulaManager bfmgr;
  private PathFormulaManager pfmgr;

  private final RCNFManager semiCNFConverter;
  private final CFA cfa;

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Stats stats = new Stats();

  private final Map<CFANode, Set<BooleanFormula>> locationInvariantsCache = new HashMap<>();

  private final FormulaInvariantsSupplier globalInvariants;
  private final Specification specification;

  public PredicateCPAInvariantsManager(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    specification = pSpecification;
    cfa = pCfa;

    globalInvariants = new FormulaInvariantsSupplier(pAggregatedReachedSets, logger);
    updateGlobalInvariants();

    semiCNFConverter = new RCNFManager(pConfig);
  }

  public boolean appendToAbstractionFormula() {
    return appendToAbstractionFormula;
  }

  public boolean appendToPathFormula() {
    return appendToPathFormula;
  }

  public boolean addToPrecision() {
    return addToPrecision;
  }

  public void updateGlobalInvariants() {
    globalInvariants.updateInvariants();
  }

  @Override
  public BooleanFormula getInvariantFor(
      CFANode pNode, FormulaManagerView pFmgr, PathFormulaManager pPfmgr, PathFormula pContext) {
    BooleanFormulaManager bfmgr = pFmgr.getBooleanFormulaManager();
    Set<BooleanFormula> localInvariants =
        locationInvariantsCache.getOrDefault(pNode, ImmutableSet.of());
    BooleanFormula globalInvariant = bfmgr.makeTrue();

    if (useGlobalInvariants) {
      globalInvariant = globalInvariants.getInvariantFor(pNode, pFmgr, pPfmgr, pContext);
    }

    return bfmgr.and(globalInvariant, bfmgr.and(localInvariants));
  }

  /**
   * Runs an additional analysis restricted to the given error path and takes
   * the invariants generated by it. Note that these invariants can only be used
   * for refinement, they are path specific and will most likely lead to invalid
   * TRUE results when used during abstraction.
   *
   * @return The list of invariants for the abstraction trace or an empty list
   *  if all invariants are trivially true
   */
  public List<BooleanFormula> findPathInvariants(
      final ARGPath allStatesTrace,
      final List<ARGState> abstractionStatesTrace,
      final Set<Loop> loopsInPath,
      final PathFormulaManager pPfmgr,
      final Solver pSolver) {
    pfmgr = Preconditions.checkNotNull(pPfmgr);
    solver = Preconditions.checkNotNull(pSolver);
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();

    // start with invariant generation
    stats.invgenTime.start();
    stats.totalInvGenTries.inc();

    boolean pathInvariantGenerationSuccessful = false;

    List<BooleanFormula> foundInvariants = new ArrayList<>();
    try {
      ShutdownManager invariantShutdown = ShutdownManager.createWithParent(shutdownNotifier);
      ResourceLimitChecker limits = null;
      if (!timeForInvariantGeneration.isEmpty()) {
        WalltimeLimit l = WalltimeLimit.fromNowOn(timeForInvariantGeneration);
        limits =
            new ResourceLimitChecker(
                invariantShutdown, Collections.<ResourceLimit>singletonList(l));
        limits.start();
      }

      logger.log(Level.INFO, "Starting path invariant generation");

      // reset flag for per try check on success
      pathInvariantGenerationSuccessful =
          findInvariantsWithGenerator(
              allStatesTrace,
              abstractionStatesTrace,
              loopsInPath,
              invariantShutdown,
              foundInvariants);

      if (pathInvariantGenerationSuccessful) {
        logger.log(Level.INFO, "Invariant generation successful");
      } else {
        logger.log(Level.INFO, "All invariants were TRUE, ignoring result.");
      }

      if (!timeForInvariantGeneration.isEmpty()) {
        limits.cancel();
      }

    } catch (CPAException | InterruptedException | InvalidConfigurationException | IOException e) {
      logger.logUserException(Level.INFO, e, "No invariants could be computed");
      foundInvariants.clear();

    } finally {
      if (pathInvariantGenerationSuccessful) {
        stats.successfulInvGenTries.inc();
      }
    }

    stats.invgenTime.stop();
    return foundInvariants;
  }

  /**
   * This method finds invariants for usage during refinement or precision
   * adjustment of the PredicateAnalysisCPA. The exact use case can be configured.
   *
   * For better performance this method should only be called during refinement.
   * The computed invariants (if there are some) are cached for later usage in
   * precision adjustment.
   */
  public void findInvariants(
      final ARGPath allStatesTrace,
      final List<ARGState> abstractionStatesTrace,
      final PathFormulaManager pPfmgr,
      final Solver pSolver) {

    updateGlobalInvariants(); // we want to have the newest global invariants available

    // skip without doing anything if we do not have a strategy for invariant generation
    if (generationStrategy.isEmpty()) {
      return;
    }

    pfmgr = Preconditions.checkNotNull(pPfmgr);
    solver = Preconditions.checkNotNull(pSolver);
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();

    List<Pair<PathFormula, CFANode>> argForPathFormulaBasedGeneration = new ArrayList<>();

    for (ARGState state : abstractionStatesTrace) {
      CFANode node = extractLocation(state);
      // TODO what if loop structure does not exist?
      if (cfa.getLoopStructure().get().getAllLoopHeads().contains(node)) {
        PredicateAbstractState predState = PredicateAbstractState.getPredicateState(state);
        argForPathFormulaBasedGeneration.add(
            Pair.of(predState.getAbstractionFormula().getBlockFormula(), node));
      } else if (!node.equals(
          extractLocation(abstractionStatesTrace.get(abstractionStatesTrace.size() - 1)))) {
        argForPathFormulaBasedGeneration.add(Pair.of(null, node));
      }
    }

    // clear refinementCache
    boolean atLeastOneStrategyFinished = false;
    boolean atLeastOneSuccessful = false;
    int numUsedStrategies = 0;

    // start with invariant generation
    stats.invgenTime.start();
    stats.totalInvGenTries.inc();

    try {
      ShutdownManager invariantShutdown = ShutdownManager.createWithParent(shutdownNotifier);
      ResourceLimitChecker limits = null;
      if (!timeForInvariantGeneration.isEmpty()) {
        WalltimeLimit l = WalltimeLimit.fromNowOn(timeForInvariantGeneration);
        limits =
            new ResourceLimitChecker(
                invariantShutdown, Collections.<ResourceLimit>singletonList(l));
        limits.start();
      }

      for (InvariantGenerationStrategy generation : generationStrategy) {
        logger.log(Level.INFO, "Starting invariant generation with", generation);
        numUsedStrategies++;
        // reset flag for per try check on success
        boolean wasSuccessful = false;

        switch (generation) {
          case PF_CNF_KIND:
              for (Pair<PathFormula, CFANode> pair : argForPathFormulaBasedGeneration) {
                if (pair.getFirst() != null) {
                wasSuccessful =
                    findInvariantPartOfPathFormulaWithKInduction(
                            pair.getSecond(), pair.getFirst(), invariantShutdown.getNotifier())
                        || wasSuccessful;
                } else {
                  addResultToCache(bfmgr.makeTrue(), pair.getSecond());
                }
              }
            break;

          case PF_INDUCTIVE_WEAKENING:
              for (Pair<PathFormula, CFANode> pair : argForPathFormulaBasedGeneration) {
                if (pair.getFirst() != null) {
                wasSuccessful =
                    findInvariantPartOfPathFormulaWithWeakening(
                            pair.getSecond(), pair.getFirst(), invariantShutdown.getNotifier())
                        || wasSuccessful;
                } else {
                  addResultToCache(bfmgr.makeTrue(), pair.getSecond());
                }
              }
            break;

          case RF_INTERPOLANT_KIND:
            wasSuccessful =
                findInvariantInterpolants(
                    allStatesTrace,
                    abstractionStatesTrace,
                    invariantShutdown.getNotifier());
            break;

          default:
            throw new AssertionError("Unhandled case statement");
        }

        if (wasSuccessful) {
          logger.log(Level.INFO, "Invariant generation successful");
          atLeastOneSuccessful = true;
        } else {
          logger.log(Level.INFO, "All invariants were TRUE, ignoring result.");
        }

        atLeastOneStrategyFinished = true;

        // if we did successfully compute invariants we do not need to
        // compute them once more with a different strategy
        if (useAllStrategies
            || (!useAllStrategies && atLeastOneSuccessful)
            || invariantShutdown.getNotifier().shouldShutdown()) {
          break;
        }
      }

      if (!timeForInvariantGeneration.isEmpty()) {
        limits.cancel();
      }

    } catch (CPAException
        | SolverException
        | InterruptedException
        | InvalidConfigurationException e) {
      if (atLeastOneStrategyFinished) {
        logger.logUserException(Level.INFO, e, "Subsequent run of invariant generation failed");
      } else {
        logger.logUserException(Level.INFO, e, "No invariants could be computed");
        atLeastOneSuccessful = false;
      }

      // update the refinement cache at the very end of the computation
      // resetting it also makes sure that a partial computation (e.g. in case
      // of the shutdown notifier hitting the time limit in the middle of a run
      // of CPAInvariantGenerator) is not influencing the overall result due to
      // missing invariants (the list has to have an exact number of items
      // otherwise the refinement will fail)
    } finally {
      if (atLeastOneStrategyFinished) {
        stats.terminatingInvGenTries.inc();
      }
      if (atLeastOneSuccessful) {
        stats.successfulInvGenTries.inc();
      }
      stats.usedStrategiesPerTrie.setNextValue(numUsedStrategies);
    }

    stats.invgenTime.stop();
  }

  private void addResultToCache(BooleanFormula pInvariant, CFANode pLocation) {

    // add to this cache for combination and for Abstraction Formula
    // we do only want to add something if the formula is not trivially TRUE
    // (TRUE is an invariant, but it is not useful)
    if (!bfmgr.isTrue(pInvariant)) {
      Set<BooleanFormula> foundInvariants = locationInvariantsCache.get(pLocation);

      if (foundInvariants != null) {
        foundInvariants.add(pInvariant);
      } else {
        Set<BooleanFormula> invariantSet = new HashSet<>();
        invariantSet.add(pInvariant);
        locationInvariantsCache.put(pLocation, invariantSet);
      }
    }
  }

  /**
   * This method finds the invariant part of a pathformula and creates a region
   * out of it.
   *
   * @param pBlockFormula A path formula that determines which variables and predicates are relevant.
   * @param pLocation the node of the current loop head
   *
   * @throws CPATransferException may be thrown during loop transition creation
   */
  private boolean findInvariantPartOfPathFormulaWithWeakening(
      final CFANode pLocation, final PathFormula pBlockFormula, ShutdownNotifier pInvariantShutdown)
      throws SolverException, InterruptedException, CPATransferException,
          InvalidConfigurationException {

    try {
      stats.pfWeakeningTime.start();

      PointerTargetSet pts = pBlockFormula.getPointerTargetSet();
      SSAMap ssa = pBlockFormula.getSsa();
      PathFormula loopFormula =
          new LoopTransitionFinder(
                  config, cfa.getLoopStructure().get(), pfmgr, fmgr, logger, pInvariantShutdown)
              .generateLoopTransition(ssa, pts, pLocation);

      Set<BooleanFormula> lemmas =
          semiCNFConverter
              .toLemmasInstantiated(pBlockFormula, fmgr)
              .stream()
              .map(s -> fmgr.uninstantiate(s))
              .collect(Collectors.toSet());

      Set<BooleanFormula> inductiveLemmas =
          new InductiveWeakeningManager(config, solver, logger, shutdownNotifier)
              .findInductiveWeakeningForRCNF(ssa, loopFormula, lemmas);

      if (lemmas.isEmpty()) {
        logger.log(Level.FINER, "Invariant for location", pLocation, "is true, ignoring it");
        return false;
      } else {
        addResultToCache(bfmgr.and(inductiveLemmas), pLocation);
        logger.log(Level.FINER, "Generated invariant: ", inductiveLemmas);
        return true;
      }
    } finally {
      stats.pfWeakeningTime.stop();
    }
  }

  private boolean findInvariantPartOfPathFormulaWithKInduction(
      final CFANode pLocation, PathFormula pPathFormula, ShutdownNotifier pInvariantShutdown)
      throws InterruptedException, CPAException, InvalidConfigurationException {

    try {
      stats.pfKindTime.start();

      Set<BooleanFormula> conjuncts =
          semiCNFConverter
              .toLemmasInstantiated(pPathFormula, fmgr)
              .stream()
              .map(s -> fmgr.uninstantiate(s))
              .collect(Collectors.toSet());

      final Map<String, BooleanFormula> formulaToRegion = new HashMap<>();
      StaticCandidateProvider candidateGenerator =
          new StaticCandidateProvider(
              from(conjuncts)
                  .transform(
                      (Function<BooleanFormula, CandidateInvariant>)
                          pInput -> {
                            String dumpedFormula = fmgr.dumpFormula(pInput).toString();
                            formulaToRegion.put(dumpedFormula, pInput);
                            return makeLocationInvariant(pLocation, dumpedFormula);
                          }));

      new KInductionInvariantChecker(
              config,
              pInvariantShutdown,
              new OnlyWarningsLogmanager(logger),
              cfa,
              specification,
              candidateGenerator)
          .checkCandidates();

      Set<CandidateInvariant> invariants = candidateGenerator.getConfirmedCandidates();

      BooleanFormula invariant = bfmgr.makeTrue();
      for (CandidateInvariant candidate : invariants) {
        invariant = bfmgr.and(invariant, formulaToRegion.get(candidate.toString()));
      }

      if (bfmgr.isTrue(invariant)) {
        logger.log(Level.FINER, "Invariant for location", pLocation, "is true, ignoring it");
        return false;
      } else {
        addResultToCache(invariant, pLocation);
        return true;
      }

    } finally {
      stats.pfKindTime.stop();
    }
  }

  /**
   * This method generates invariants by using another CPA and extracting invariants
   * out of it.
   */
  private boolean findInvariantsWithGenerator(
      final ARGPath allStatesTrace,
      final List<ARGState> abstractionStatesTrace,
      final Set<Loop> pLoopsInPath,
      ShutdownManager pInvariantShutdown,
      List<BooleanFormula> foundInvariants)
      throws IOException, InvalidConfigurationException, CPAException, InterruptedException {

    try {
      stats.rfInvGenTime.start();

      StringBuilder spc = new StringBuilder();
      ARGUtils.producePathAutomatonWithLoops(
          spc,
          allStatesTrace.getFirstState(),
          allStatesTrace.getStateSet(),
          "invGen",
          pLoopsInPath);

      // may be null when -noout is specified
      if (dumpInvariantGenerationAutomata && dumpInvariantGenerationAutomataFile != null) {
        Path logPath = dumpInvariantGenerationAutomataFile.getFreshPath();
        MoreFiles.writeFile(logPath, Charset.defaultCharset(), spc);
      }

      Scope scope =
          cfa.getLanguage() == Language.C
              ? new CProgramScope(cfa, logger)
              : DummyScope.getInstance();

      List<Automaton> automata =
          AutomatonParser.parseAutomaton(
              new StringReader(spc.toString()),
              Optional.empty(),
              config,
              logger,
              cfa.getMachineModel(),
              scope,
              cfa.getLanguage());

      InvariantGenerator invGen =
          CPAInvariantGenerator.create(
              config,
              new OnlyWarningsLogmanager(logger),
              pInvariantShutdown,
              Optional.empty(),
              cfa,
              specification,
              automata);

      return generateInvariants0(abstractionStatesTrace, invGen, foundInvariants);
    } finally {
      stats.rfInvGenTime.stop();
    }
  }

  private boolean generateInvariants0(
      final List<ARGState> abstractionStatesTrace,
      InvariantGenerator invGen,
      List<BooleanFormula> foundInvariants)
      throws CPAException, InterruptedException {

    invGen.start(cfa.getMainFunction());
    InvariantSupplier invSup = new FormulaInvariantsSupplier(invGen.get(), logger);

    // we do only want to use invariants that can be used to make the program safe
    if (!useStrongInvariantsOnly || invGen.isProgramSafe()) {
      List<Pair<BooleanFormula, CFANode>> invariants = new ArrayList<>();
      for (ARGState s : abstractionStatesTrace) {
        // the last one will always be false, we don't need it here
        if (s != abstractionStatesTrace.get(abstractionStatesTrace.size() - 1)) {
          CFANode location = extractLocation(s);
          PredicateAbstractState pas = PredicateAbstractState.getPredicateState(s);
          BooleanFormula invariant = invSup.getInvariantFor(location, fmgr, pfmgr, pas.getPathFormula());
          invariants.add(Pair.of(invariant, location));
          logger.log(Level.FINEST, "Invariant for location", location, "is", invariant);
        }
      }

      boolean wasSuccessful =
          !from(invariants)
              .transform(Pair::getFirst)
              .allMatch(equalTo(fmgr.getBooleanFormulaManager().makeTrue()));

      if (wasSuccessful) {
        from(invariants).transform(Pair::getFirst).copyInto(foundInvariants);
      }

      return wasSuccessful;

    } else {
      logger.log(
          Level.INFO,
          "Invariants found, but they are not strong enough to refute the counterexample"
              + " consider setting \"useStrongInvariantsOnly\" to false if you want to use"
              + " them. At least for usage during precision adjustment this should be better"
              + " than nothing.");
    }

    return false;
  }

  private boolean findInvariantInterpolants(
      ARGPath pPath, List<ARGState> pAbstractionStatesTrace, ShutdownNotifier pInvariantShutdown)
      throws CPAException, InterruptedException, InvalidConfigurationException {

    stats.rfKindTime.start();

    try {
      InvCandidateGenerator candidateGenerator =
          new InvCandidateGenerator(pPath, pAbstractionStatesTrace);

      KInductionInvariantChecker invChecker =
          new KInductionInvariantChecker(
              config,
              pInvariantShutdown,
              new OnlyWarningsLogmanager(logger),
              cfa,
              specification,
              candidateGenerator);
      invChecker.checkCandidates();

      if (candidateGenerator.hasFoundInvariants()) {
        List<Pair<BooleanFormula, CFANode>> invariants =
            candidateGenerator.retrieveConfirmedInvariants();
        // if we found invariants at least one of them may not be "TRUE"
        boolean wasSuccessful =
            !from(invariants)
                .transform(Pair::getFirst)
                .allMatch(equalTo(fmgr.getBooleanFormulaManager().makeTrue()));
        if (wasSuccessful) {
          for (Pair<BooleanFormula, CFANode> invariant : invariants) {
            addResultToCache(fmgr.uninstantiate(invariant.getFirst()), invariant.getSecond());
          }
        }

        return wasSuccessful;

      } else {
        logger.log(Level.INFO, "No invariants were found.");
      }
    } finally {
      stats.rfKindTime.stop();
    }

    return false;
  }

  private class InvCandidateGenerator implements CandidateGenerator {

    private int trieNum = 0;
    private List<CandidateInvariant> candidates = new ArrayList<>();

    private final ARGPath argPath;
    private final List<CFANode> abstractionNodes;
    private final Set<ARGState> elementsOnPath;
    private final List<ARGState> abstractionStatesTrace;
    private final List<InfeasiblePrefix> infeasiblePrefixes;
    private final List<AbstractLocationFormulaInvariant> foundInvariants = new ArrayList<>();
    private final InterpolationManager imgr;

    private InvCandidateGenerator(ARGPath pPath, List<ARGState> pAbstractionStatesTrace)
        throws CPAException, InterruptedException, InvalidConfigurationException {
      argPath = pPath;
      abstractionNodes = from(pAbstractionStatesTrace).transform(EXTRACT_LOCATION).toList();
      elementsOnPath = getAllStatesOnPathsTo(argPath.getLastState());
      abstractionStatesTrace = pAbstractionStatesTrace;
      imgr =
          new InterpolationManager(
              pfmgr,
              solver,
              cfa.getLoopStructure(),
              cfa.getVarClassification(),
              config,
              shutdownNotifier,
              logger);

      infeasiblePrefixes =
          new PredicateBasedPrefixProvider(config, logger, solver, pfmgr, shutdownNotifier)
              .extractInfeasiblePrefixes(argPath);
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

      candidates =
          newArrayList(
              from(infeasiblePrefixes).transformAndConcat(TO_LOCATION_CANDIDATE_INVARIANT));
      trieNum++;

      return true;
    }

    private final Function<InfeasiblePrefix, List<CandidateInvariant>>
        TO_LOCATION_CANDIDATE_INVARIANT =
            new Function<InfeasiblePrefix, List<CandidateInvariant>>() {
              @Override
              public List<CandidateInvariant> apply(InfeasiblePrefix pInput) {
                List<BooleanFormula> interpolants;
                try {
                  List<BooleanFormula> pathFormula = pInput.getPathFormulae();
                  // the prefix is not filled up with trues if it is shorter than
                  // the path so we need to do it ourselves
                  while (pathFormula.size() < abstractionStatesTrace.size()) {
                    pathFormula.add(bfmgr.makeTrue());
                  }
                  interpolants =
                      imgr.buildCounterexampleTrace(
                              pInput.getPathFormulae(),
                              ImmutableList.copyOf(abstractionStatesTrace),
                              elementsOnPath,
                              true)
                          .getInterpolants();

                } catch (CPAException | InterruptedException e) {
                  logger.logUserException(
                      Level.WARNING, e, "Could not compute interpolants for k-induction inv-gen");
                  return Collections.emptyList();
                }

                List<CandidateInvariant> invCandidates = new ArrayList<>();
                // add false as last interpolant for the error location
                interpolants = new ArrayList<>(interpolants);
                interpolants.add(bfmgr.makeFalse());

                for (Pair<CFANode, BooleanFormula> nodeAndFormula :
                    Pair.<CFANode, BooleanFormula>zipList(abstractionNodes, interpolants)) {
                  invCandidates.add(
                      makeLocationInvariant(
                          nodeAndFormula.getFirst(),
                          fmgr.dumpFormula(fmgr.uninstantiate(nodeAndFormula.getSecond()))
                              .toString()));
                }
                return invCandidates;
              }
            };

    @Override
    public boolean hasCandidatesAvailable() {
      return !candidates.isEmpty();
    }

    @Override
    public void confirmCandidates(Iterable<CandidateInvariant> pCandidates) {
      for (CandidateInvariant inv : pCandidates) {
        candidates.remove(inv);
        foundInvariants.add((AbstractLocationFormulaInvariant) inv);
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

    @Override
    public Set<AbstractLocationFormulaInvariant> getConfirmedCandidates() {
      return new HashSet<>(foundInvariants);
    }

    public List<Pair<BooleanFormula, CFANode>> retrieveConfirmedInvariants() {
      FluentIterable<AbstractLocationFormulaInvariant> found = from(foundInvariants);
      List<Pair<BooleanFormula, CFANode>> invariants = new ArrayList<>();
      for (final CFANode node : abstractionNodes) {
        // we don't want the last node to be here, as the invariant will always
        // be FALSE
        if (node.equals(abstractionNodes.get(abstractionNodes.size() - 1))) {
          continue;
        }
        invariants.add(
            Pair.of(
                found
                    .filter(
                        new Predicate<AbstractLocationFormulaInvariant>() {
                          @Override
                          public boolean apply(AbstractLocationFormulaInvariant pInput) {
                            return getOnlyElement(pInput.getLocations()).equals(node);
                          }
                        })
                    .first()
                    .transform(
                        new Function<AbstractLocationFormulaInvariant, BooleanFormula>() {
                          @Override
                          public BooleanFormula apply(AbstractLocationFormulaInvariant pInput) {
                            try {
                              return pInput.getFormula(fmgr, pfmgr, null);
                            } catch (CPATransferException | InterruptedException e) {
                              // this should never happen, if it does we log
                              // the exception and return TRUE as invariant
                              logger.logUserException(
                                  Level.WARNING,
                                  e,
                                  "Invariant could not be" + " retrieved from InvariantGenerator");
                              return fmgr.getBooleanFormulaManager().makeTrue();
                            }
                          }
                        })
                    .or(fmgr.getBooleanFormulaManager().makeTrue()),
                node));
      }

      return invariants;
    }
  }

  /**
   * This class is used for removing all logging output besides warnings and higher
   * from the standard output of the analysis. This is useful when running the
   * Invariant Generation or the KInduction invariant checker. Their output is
   * not really interesting for the User, but instead our LogMessages are and they
   * could have easily been overlooked before (due to the potentially high amount
   * of logging messages in analyses used there).
   */
  private static class OnlyWarningsLogmanager extends ForwardingLogManager {

    private final LogManager logger;

    public OnlyWarningsLogmanager(LogManager pLogger) {
      logger = pLogger;
    }

    @Override
    public LogManager withComponentName(String pName) {
      return new OnlyWarningsLogmanager(logger.withComponentName(pName));
    }

    @Override
    protected LogManager delegate() {
      return logger;
    }

    @Override
    public boolean wouldBeLogged(Level pPriority) {
      return pPriority.intValue() >= Level.WARNING.intValue() && logger.wouldBeLogged(pPriority);
    }

    @Override
    public void log(Level pPriority, Object... pArgs) {
      if (wouldBeLogged(pPriority)) {
        logger.log(pPriority, pArgs);
      }
    }

    @Override
    public void log(Level pPriority, Supplier<String> pMsgSupplier) {
      if (wouldBeLogged(pPriority)) {
        super.log(pPriority, pMsgSupplier);
      }
    }

    @Override
    @FormatMethod
    public void logf(Level pPriority, String pFormat, Object... pArgs) {
      if (wouldBeLogged(pPriority)) {
        logger.logf(pPriority, pFormat, pArgs);
      }
    }

    @Override
    public void logUserException(
        Level pPriority, Throwable pE, @Nullable String pAdditionalMessage) {
      if (wouldBeLogged(pPriority)) {
        logger.logUserException(pPriority, pE, pAdditionalMessage);
      }
    }

    @Override
    public void logException(Level pPriority, Throwable pE, @Nullable String pAdditionalMessage) {
      if (wouldBeLogged(pPriority)) {
        logger.logException(pPriority, pE, pAdditionalMessage);
      }
    }
  }

  private class Stats extends AbstractStatistics {
    private final StatTimer invgenTime = new StatTimer("Total time for invariant generation");
    private final StatTimer rfKindTime =
        new StatTimer("Time for checking interpolants with k-induction");
    private final StatTimer rfInvGenTime =
        new StatTimer("Time for generation invariants with CPAchecker");
    private final StatTimer pfKindTime =
        new StatTimer("Time for checking pathformula with k-induction");
    private final StatTimer pfWeakeningTime = new StatTimer("Time for inductive weakening");
    private final StatCounter totalInvGenTries =
        new StatCounter("Total invariant generation tries");
    private final StatInt usedStrategiesPerTrie =
        new StatInt(StatKind.AVG, "Used strategies per generation try");
    private final StatCounter successfulInvGenTries =
        new StatCounter("Successful invariant generation tries");
    private final StatCounter terminatingInvGenTries =
        new StatCounter("Invariant generation tries finishing in time");

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);

      int numberOfInvGenTries = totalInvGenTries.getUpdateCount();
      if (numberOfInvGenTries > 0) {
        w0.put(totalInvGenTries)
            .beginLevel()
            .put(terminatingInvGenTries)
            .put(successfulInvGenTries)
            .put(
                "Used strategies per generation try",
                String.format(
                    "%11.2f (max: %d, min: %d)",
                    usedStrategiesPerTrie.getAverage(),
                    usedStrategiesPerTrie.getMax(),
                    usedStrategiesPerTrie.getMin()))
            .endLevel()
            .spacer()
            .put(invgenTime)
            .beginLevel()
            .put(rfKindTime)
            .put(rfInvGenTime)
            .put(pfKindTime)
            .put(pfWeakeningTime)
            .endLevel()
            .spacer()
            .put("Size of invariants cache", String.format("%8d", locationInvariantsCache.size()));
      }
    }

    @Override
    public String getName() {
      return "Invariant Generation";
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    semiCNFConverter.collectStatistics(pStatsCollection);
  }

}
