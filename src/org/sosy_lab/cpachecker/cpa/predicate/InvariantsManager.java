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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.AbstractLocationFormulaInvariant.makeLocationInvariant;
import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getAllStatesOnPathsTo;
import static org.sosy_lab.cpachecker.util.AbstractStates.EXTRACT_LOCATION;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.io.Path;
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
import org.sosy_lab.cpachecker.core.algorithm.bmc.AbstractLocationFormulaInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.StaticCandidateProvider;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantChecker;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.cpa.formulaslicing.InductiveWeakeningManager;
import org.sosy_lab.cpachecker.cpa.formulaslicing.LoopTransitionFinder;
import org.sosy_lab.cpachecker.cpa.formulaslicing.RCNFManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.visitors.DefaultBooleanFormulaVisitor;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Options(prefix = "cpa.predicate.invariants")
class InvariantsManager implements StatisticsProvider {

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

    /**
     * Runs an additional analysis restricted to the given error path and takes
     * the invariants generated by it.
     *
     * This option interferes with {@link InvariantGenerationStrategy#ASYNC}
     * as both use the configuration set with <code>invariantGeneration.config</code>.
     */
    RF_INVARIANT_GENERATION,

    /**
     * Runs an additional analysis concurrent to the main analysis and asks
     * for invariants when needed. The generated invariants will be added everywhere
     * they are need (e.g. refinement + abstraction formula) additionally they
     * can be retrieved by calling {@link AsyncInvariantsSupplier#getInvariantFor(CFANode,
     * FormulaManagerView, PathFormulaManager, PathFormula)}
     *
     * This option interferes with {@link InvariantGenerationStrategy#RF_INVARIANT_GENERATION}
     * as both use the configuration set with <code>invariantGeneration.config</code>.
     */
    ASYNC;
  }

  private static enum InvariantUsageStrategy {

    /**
     * Generated invariants should only be used for refining the precision
     * of the analysis.
     */
    REFINEMENT,

    /**
     * Generated invariants should only be used for directly adding them
     * to the abstraction Formula during precision adjustment.
     */
    ABSTRACTION_FORMULA,

    /**
     * Generated invariants should be used for both, refinement and precision
     * adjustment.
     */
    COMBINATION,

    /**
     * No invariants should be used. This means that every approach besides
     * the concurrent invariant generation will not be executed at all. The invariants
     * computed by the concurrent approach (if selected) can still be retrieved
     * by calling {@link AsyncInvariantsSupplier#getInvariantFor(CFANode,
     * FormulaManagerView, PathFormulaManager, PathFormula)}
     */
    NONE;
  }

  @Option(
    secure = true,
    description =
        "Which strategy should be used for generating invariants, a comma separated"
            + " list can be specified. Usually later specified strategies serve as"
            + " fallback for earlier ones."
  )
  private List<InvariantGenerationStrategy> generationStrategy =
      Lists.newArrayList(InvariantGenerationStrategy.RF_INVARIANT_GENERATION);

  @Option(
    secure = true,
    description = "Where should the generated invariants (if there are some) be used?"
  )
  private InvariantUsageStrategy usageStrategy = InvariantUsageStrategy.NONE;

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

  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final PathFormulaManager pfmgr;
  private final InterpolationManager imgr;

  private final InductiveWeakeningManager inductiveWeakeningMgr;
  private final RCNFManager semiCNFConverter;
  private final CFA cfa;
  private final PrefixProvider prefixProvider;

  // TODO Configuration should not be used at runtime, only during constructor
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Stats stats = new Stats();

  private final Map<CFANode, BooleanFormula> loopFormulaCache = new HashMap<>();
  private final Cache<CFANode, Set<BooleanFormula>> locationInvariantsCache;
  private final List<BooleanFormula> refinementCache = new ArrayList<>();
  private LocationInvariantSupplier invariantSupplierSingleton = null;

  private final AsyncInvariantsSupplier asyncInvariantSupplierSingleton;

  public InvariantsManager(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Solver pSolver,
      PathFormulaManager pPfmgr,
      PrefixProvider pPrefixProvider)
      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this);

    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    locationInvariantsCache = CacheBuilder.newBuilder().recordStats().build();
    cfa = pCfa;
    solver = pSolver;
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr = pPfmgr;
    semiCNFConverter = new RCNFManager(fmgr, pConfig);

    imgr =
        new InterpolationManager(
            pPfmgr,
            pSolver,
            pCfa.getLoopStructure(),
            pCfa.getVarClassification(),
            pConfig,
            pShutdownNotifier,
            pLogger);

    prefixProvider = pPrefixProvider;
    inductiveWeakeningMgr = new InductiveWeakeningManager(pConfig, pSolver, pLogger,
        pShutdownNotifier);

    if (generationStrategy.contains(InvariantGenerationStrategy.ASYNC)) {
      asyncInvariantSupplierSingleton = new AsyncInvariantsSupplier();
    } else {
      asyncInvariantSupplierSingleton = null;
    }

    if (usageStrategy != InvariantUsageStrategy.NONE) {
      logger.log(
          Level.INFO,
          "Using invariant generation strategy",
          generationStrategy,
          "for",
          usageStrategy);
    } else {
      if (!generationStrategy.contains(InvariantGenerationStrategy.ASYNC)) {
        logger.log(Level.INFO, "No invariants are computed");
      }
    }
  }

  public void setInitialLocation(CFANode node) {
    if (asyncInvariantSupplierSingleton != null) {
      asyncInvariantSupplierSingleton.setInitialLocation(node);
    }
  }

  LocationInvariantSupplier asRegionInvariantsSupplier() {
    if (invariantSupplierSingleton == null) {
      invariantSupplierSingleton = new LocationInvariantSupplier();
    }
    return invariantSupplierSingleton;
  }

  AsyncInvariantsSupplier asAsyncInvariantsSupplier() {
    Preconditions.checkState(
        asyncInvariantSupplierSingleton != null,
        "To use asynchronous invariants this has to be configured in the generation strategy.");
    return asyncInvariantSupplierSingleton;
  }

  /**
   * This method returns the invariants computed for the given locations in
   * {@link #findInvariants(ARGPath, List, Set)}
   * in the given order.
   */
  public List<BooleanFormula> getInvariantsForRefinement() {
    // we have to check that at least one invariant is not trivially true
    boolean containsOnlyTrivialInvariants = true;
    for (BooleanFormula formula : refinementCache) {
      if (!containsOnlyTrivialInvariants) {
        break;
      }

      containsOnlyTrivialInvariants = bfmgr.isTrue(formula);
    }

    if (containsOnlyTrivialInvariants) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(refinementCache);
    }
  }

  /**
   * @return Indicates if invariants should be computed at all.
   */
  public boolean shouldInvariantsBeComputed() {
    return usageStrategy != InvariantUsageStrategy.NONE;
  }

  /**
   * @return Indicates if invariants should be used for refinement.
   */
  public boolean shouldInvariantsBeUsedForRefinement() {
    return usageStrategy == InvariantUsageStrategy.REFINEMENT
        || usageStrategy == InvariantUsageStrategy.COMBINATION;
  }

  /**
   * @return Indicates if invariants should be used during abstraction
   */
  public boolean shouldInvariantsBeUsedForAbstraction() {
    return usageStrategy == InvariantUsageStrategy.ABSTRACTION_FORMULA
        || usageStrategy == InvariantUsageStrategy.COMBINATION;
  }

  /**
   * This method finds invariants for usage during refinement or precision
   * adjustment of the PredicateAnalysisCPA. The exact use case can be configured.
   *
   * For better performance this method should only be called during refinement.
   * The computed invariants (if there are some) are cached for later usage in
   * precision adjustment.
   */
  public void findInvariants(final ARGPath allStatesTrace,
      final List<ARGState> abstractionStatesTrace, final Set<Loop> loopsInPath) {
    // shortcut if we do not need to compute anything
    if (!shouldInvariantsBeComputed()) {
      return;
    }

    List<Pair<PathFormula, CFANode>> argForPathFormulaBasedGeneration = new ArrayList<>();
    List<CFANode> listOfNodes = new ArrayList<>();

    for (ARGState state : abstractionStatesTrace) {
      CFANode node = extractLocation(state);
      // TODO what if loop structure does not exist?
      if (cfa.getLoopStructure().get().getAllLoopHeads().contains(node)) {
        PredicateAbstractState predState =
            extractStateByType(state, PredicateAbstractState.class);
        PathFormula pathFormula = predState.getPathFormula();
        argForPathFormulaBasedGeneration.add(Pair.of(pathFormula, node));
        listOfNodes.add(node);
      } else if (!node.equals(
          extractLocation(abstractionStatesTrace.get(abstractionStatesTrace.size() - 1)))) {
        argForPathFormulaBasedGeneration.add(Pair.<PathFormula, CFANode>of(null, node));
        listOfNodes.add(node);
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
                  addResultToCache(bfmgr.makeBoolean(true), pair.getSecond());
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
                  addResultToCache(bfmgr.makeBoolean(true), pair.getSecond());
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

          case RF_INVARIANT_GENERATION:
            wasSuccessful =
                findInvariantsWithGenerator(
                    allStatesTrace,
                    abstractionStatesTrace,
                    loopsInPath,
                    invariantShutdown);
            break;

          case ASYNC:
            for (Pair<PathFormula, CFANode> pair : argForPathFormulaBasedGeneration) {
              if (pair.getFirst() != null) {
                BooleanFormula invariant =
                    asyncInvariantSupplierSingleton.getInvariantFor(
                        pair.getSecond(), fmgr, pfmgr, pair.getFirst());
                wasSuccessful = wasSuccessful || !bfmgr.isTrue(invariant);
                addResultToCache(invariant, pair.getSecond());
              } else {
                addResultToCache(bfmgr.makeBoolean(true), pair.getSecond());
              }
            }
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

    } catch (
        CPAException | SolverException | InterruptedException | InvalidConfigurationException
                | IOException
            e) {
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
      updateRefinementCache(listOfNodes);

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
      Set<BooleanFormula> foundInvariants = locationInvariantsCache.getIfPresent(pLocation);

      if (foundInvariants != null) {
        foundInvariants.add(pInvariant);
      } else {
        Set<BooleanFormula> invariantSet = new HashSet<>();
        invariantSet.add(pInvariant);
        locationInvariantsCache.put(pLocation, invariantSet);
      }
    }
  }

  private void updateRefinementCache(List<CFANode> nodes) {
    // lazily compute the conjunction of all invariants per location in the path to be refined
    if (shouldInvariantsBeUsedForRefinement()) {
      refinementCache.clear();
      for (CFANode node : nodes) {

        Set<BooleanFormula> oldInvariants = locationInvariantsCache.getIfPresent(node);
        if (oldInvariants == null) {
          oldInvariants = Collections.singleton(bfmgr.makeBoolean(true));
        }

        // fill nodes without invariants with TRUE
        Set<BooleanFormula> invariantParts =
            from(oldInvariants)
                .transform(
                    new Function<BooleanFormula, BooleanFormula>() {
                      @Override
                      public BooleanFormula apply(BooleanFormula pInput) {
                        if (pInput == null) {
                          return bfmgr.makeBoolean(true);
                        } else {
                          return pInput;
                        }
                      }
                    })
                .toSet();

        // add a conjunction of the invariant parts to the refinement cache
        // (or the single invariant if there is only one part)
        if (invariantParts.size() > 1) {
          refinementCache.add(bfmgr.and(invariantParts));
        } else {
          refinementCache.add(Iterables.getOnlyElement(invariantParts));
        }
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
      PathFormula loopFormula;

      // we already found this loop and just need to update the SSA indices
      if (loopFormulaCache.containsKey(pLocation)) {
        loopFormula =
            new PathFormula(fmgr.instantiate(loopFormulaCache.get(pLocation), ssa), ssa, pts, 0);
      } else {
        loopFormula =
            new LoopTransitionFinder(
                    config, cfa.getLoopStructure().get(), pfmgr, fmgr, logger, pInvariantShutdown)
                .generateLoopTransition(ssa, pts, pLocation);
        loopFormulaCache.put(pLocation, fmgr.uninstantiate(loopFormula.getFormula()));
      }

      Set<BooleanFormula> lemmas =
          semiCNFConverter.toLemmas(
              fmgr.uninstantiate(pBlockFormula.getFormula()));

      Set<BooleanFormula> inductiveLemmas =
          inductiveWeakeningMgr.findInductiveWeakeningForRCNF(
              ssa, loopFormula, lemmas
          );

      if (lemmas.isEmpty()) {
        logger.log(Level.FINER, "Invariant for location", pLocation, "is true, ignoring it");
        return false;
      } else {
        addResultToCache(bfmgr.and(inductiveLemmas), pLocation);
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

      BooleanFormula cnfFormula = bfmgr.and(semiCNFConverter.toLemmas
          (pPathFormula
          .getFormula
          ()));
      Collection<BooleanFormula> conjuncts =
          bfmgr.visit(
              new DefaultBooleanFormulaVisitor<List<BooleanFormula>>() {
                @Override
                protected List<BooleanFormula> visitDefault() {
                  return Collections.emptyList();
                }

                @Override
                public List<BooleanFormula> visitAnd(List<BooleanFormula> operands) {
                  return operands;
                }
              },
              cnfFormula);
      final Map<String, BooleanFormula> formulaToRegion = new HashMap<>();
      StaticCandidateProvider candidateGenerator =
          new StaticCandidateProvider(
              from(conjuncts)
                  .transform(
                      new Function<BooleanFormula, CandidateInvariant>() {
                        @Override
                        public CandidateInvariant apply(BooleanFormula pInput) {
                          String dumpedFormula = fmgr.dumpFormula(pInput).toString();
                          formulaToRegion.put(dumpedFormula, pInput);
                          return makeLocationInvariant(pLocation, dumpedFormula);
                        }
                      }));

      new KInductionInvariantChecker(config, pInvariantShutdown, new OnlyWarningsLogmanager(logger), cfa, candidateGenerator)
          .checkCandidates();

      Set<CandidateInvariant> invariants = candidateGenerator.getConfirmedCandidates();

      BooleanFormula invariant = bfmgr.makeBoolean(true);
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
      ShutdownManager pInvariantShutdown)
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

      if (dumpInvariantGenerationAutomata) {
        Path logPath = dumpInvariantGenerationAutomataFile.getFreshPath();
        CharSink file = logPath.asCharSink(Charset.defaultCharset(), FileWriteMode.APPEND);
        file.openStream().append(spc).close();
      }

      Scope scope =
          cfa.getLanguage() == Language.C
              ? new CProgramScope(cfa, logger)
              : DummyScope.getInstance();

      List<Automaton> automata =
          AutomatonParser.parseAutomaton(
              new StringReader(spc.toString()),
              Optional.<Path>absent(),
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
              Optional.<ShutdownManager>absent(),
              cfa,
              automata);

      return generateInvariants0(abstractionStatesTrace, invGen);
    } finally {
      stats.rfInvGenTime.stop();
    }
  }

  private boolean generateInvariants0(
      final List<ARGState> abstractionStatesTrace, InvariantGenerator invGen)
      throws CPAException, InterruptedException {

    invGen.start(cfa.getMainFunction());
    InvariantSupplier invSup = invGen.get();

    // we do only want to use invariants that can be used to make the program safe
    if (!useStrongInvariantsOnly || invGen.isProgramSafe()) {
      List<Pair<BooleanFormula, CFANode>> invariants = new ArrayList<>();
      for (ARGState s : abstractionStatesTrace) {
        // the last one will always be false, we don't need it here
        if (s != abstractionStatesTrace.get(abstractionStatesTrace.size() - 1)) {
          CFANode location = extractLocation(s);
          PredicateAbstractState pas =
              AbstractStates.extractStateByType(s, PredicateAbstractState.class);
          BooleanFormula invariant =
              invSup.getInvariantFor(location, fmgr, pfmgr, pas.getPathFormula());
          invariants.add(Pair.of(invariant, location));
          logger.log(Level.FINEST, "Invariant for location", location, "is", invariant);
        }
      }

      boolean wasSuccessful =
          !from(invariants)
              .transform(Pair.<BooleanFormula>getProjectionToFirst())
              .allMatch(equalTo(fmgr.getBooleanFormulaManager().makeBoolean(true)));

      if (wasSuccessful) {
        // iterate over the trace another time and add the result to the
        // appropriate caches (we do not do this above, as it could be that every
        // found invariant is true, this is not helpful and therefore omitted)
        for (Pair<BooleanFormula, CFANode> pair : invariants) {
          addResultToCache(pair.getFirst(), pair.getSecond());
        }
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
              candidateGenerator);
      invChecker.checkCandidates();

      if (candidateGenerator.hasFoundInvariants()) {
        List<Pair<BooleanFormula, CFANode>> invariants =
            candidateGenerator.retrieveConfirmedInvariants();
        // if we found invariants at least one of them may not be "TRUE"
        boolean wasSuccessful =
            !from(invariants)
                .transform(Pair.<BooleanFormula>getProjectionToFirst())
                .allMatch(equalTo(fmgr.getBooleanFormulaManager().makeBoolean(true)));
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

    private InvCandidateGenerator(ARGPath pPath, List<ARGState> pAbstractionStatesTrace)
        throws CPAException, InterruptedException {
      argPath = pPath;
      abstractionNodes = from(pAbstractionStatesTrace).transform(EXTRACT_LOCATION).toList();
      elementsOnPath = getAllStatesOnPathsTo(argPath.getLastState());
      abstractionStatesTrace = pAbstractionStatesTrace;

      infeasiblePrefixes = prefixProvider.extractInfeasiblePrefixes(argPath);
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
          newArrayList(concat(from(infeasiblePrefixes).transform(TO_LOCATION_CANDIDATE_INVARIANT)));
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
                    pathFormula.add(bfmgr.makeBoolean(true));
                  }
                  interpolants =
                      imgr.buildCounterexampleTrace(
                              pInput.getPathFormulae(),
                              Lists.<AbstractState>newArrayList(abstractionStatesTrace),
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
                interpolants.add(bfmgr.makeBoolean(false));

                for (Pair<CFANode, BooleanFormula> nodeAndFormula :
                    Pair.<CFANode, BooleanFormula>zipList(abstractionNodes, interpolants)) {
                  invCandidates.add(
                      makeLocationInvariant(
                          nodeAndFormula.getFirst(),
                          solver
                              .getFormulaManager()
                              .dumpFormula(nodeAndFormula.getSecond())
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
                              return fmgr.getBooleanFormulaManager().makeBoolean(true);
                            }
                          }
                        })
                    .or(fmgr.getBooleanFormulaManager().makeBoolean(true)),
                node));
      }

      return invariants;
    }
  }

  public class AsyncInvariantsSupplier implements InvariantSupplier {

    private InvariantGenerator invGen;
    private InvariantSupplier invSup;

    /**
     * private constructor so that this object can only be created from within
     * InvariantsGenerator
     */
    private AsyncInvariantsSupplier() throws InvalidConfigurationException, CPAException {
      invGen =
          CPAInvariantGenerator.create(
              config,
              logger,
              ShutdownManager.createWithParent(shutdownNotifier),
              Optional.<ShutdownManager>absent(),
              cfa);
    }

    private void setInitialLocation(CFANode pInitialLocation) {
      invGen.start(pInitialLocation);

      try {
        invSup = invGen.get();

      } catch (InterruptedException | CPAException e) {
        invGen.cancel();
        // we cannot throw any checked exception here as this is not compatible
        // with the (allowed) declared exceptions for the caller of this method
        // CPA#getInitialState so we just log a warning and swallow the exception
        // such that the analysis cane move on without invariants
        logger.logUserException(Level.WARNING, e, "Asynchronous invariant generation skipped.");
        invSup = TrivialInvariantSupplier.INSTANCE;
      }
    }

    /**
     * Returns the invariants for a given location.
     *
     * @param pLocation the location for which invariants are needed
     * @return the invariants for the given location, or null if not available
     */
    @Override
    public BooleanFormula getInvariantFor(
        CFANode pLocation,
        FormulaManagerView pFmgr,
        PathFormulaManager pPfmgr,
        PathFormula pContext) {
      checkState(invSup != null, "Before getting invariants an initial location has to be set.");
      return invSup.getInvariantFor(pLocation, pFmgr, pPfmgr, pContext);
    }
  }

  public class LocationInvariantSupplier {

    /**
     * private constructor so that this object can only be created from within
     * InvariantsGenerator
     */
    private LocationInvariantSupplier() {}

    /**
     * Returns the invariants for a given location.
     *
     * @param pLocation the location for which invariants are needed
     * @return the invariants for the given location, or null if not available
     */
    public @Nonnull Set<BooleanFormula> getInvariantFor(CFANode pLocation) {
      Set<BooleanFormula> invariants = locationInvariantsCache.getIfPresent(pLocation);
      if (invariants == null) {
        return Collections.emptySet();
      } else {
        return invariants;
      }
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
      return pPriority == Level.WARNING || pPriority == Level.SEVERE;
    }

    @Override
    public void log(Level pPriority, Object... pArgs) {
      if (pPriority == Level.WARNING || pPriority == Level.SEVERE) {
        logger.log(pPriority, pArgs);
      }
    }

    @Override
    public void logf(Level pPriority, String pFormat, Object... pArgs) {
      if (pPriority == Level.WARNING || pPriority == Level.SEVERE) {
        logger.logf(pPriority, pFormat, pArgs);
      }
    }

    @Override
    public void logUserException(
        Level pPriority, Throwable pE, @Nullable String pAdditionalMessage) {
      if (pPriority == Level.WARNING || pPriority == Level.SEVERE) {
        logger.logUserException(pPriority, pE, pAdditionalMessage);
      }
    }

    @Override
    public void logException(Level pPriority, Throwable pE, @Nullable String pAdditionalMessage) {
      if (pPriority == Level.WARNING || pPriority == Level.SEVERE) {
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
            .put(
                "Size of invariants cache",
                String.format(
                    "%8d (updated invariants: %d)",
                    locationInvariantsCache.size(),
                    locationInvariantsCache.stats().evictionCount()))
            .put(
                "Invariants cache hit rate",
                String.format(
                    "%11.2f (requests: %d, hits: %d, misses: %d)",
                    locationInvariantsCache.stats().hitRate(),
                    locationInvariantsCache.stats().requestCount(),
                    locationInvariantsCache.stats().hitCount(),
                    locationInvariantsCache.stats().missCount()));

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
    if (asyncInvariantSupplierSingleton != null
        && asyncInvariantSupplierSingleton.invGen instanceof StatisticsProvider) {
      ((StatisticsProvider) asyncInvariantSupplierSingleton.invGen)
          .collectStatistics(pStatsCollection);
    }
  }
}
