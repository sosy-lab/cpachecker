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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ConditionAdjustmentEventSubscriber;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariantCombination;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.AbstractInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.DoNothingInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationBounding;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaSlicer;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.targetreachability.ReachabilityState;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetCPA;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TestTargetLocationProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "bmc")
abstract class AbstractBMCAlgorithm
    implements StatisticsProvider, ConditionAdjustmentEventSubscriber {

  static final Predicate<AbstractState> IS_STOP_STATE =
    Predicates.compose(new Predicate<AssumptionStorageState>() {
                             @Override
                             public boolean apply(AssumptionStorageState pArg0) {
                               return (pArg0 != null) && pArg0.isStop();
                             }
                           },
                       AbstractStates.toState(AssumptionStorageState.class));

  static final Predicate<AbstractState> IS_SLICED_STATE = (state) ->
    AbstractStates.extractStateByType(state, ReachabilityState.class) == ReachabilityState.IRRELEVANT_TO_TARGET;

  @Option(secure=true, description = "If BMC did not find a bug, check whether "
      + "the bounding did actually remove parts of the state space "
      + "(this is similar to CBMC's unwinding assertions).")
  private boolean boundingAssertions = true;

  // NZ: option for interpolation-based model checking
  @Option(secure = true, description = "try using interpolation to verify programs with loops")
  private boolean interpolation = false;

  @Option(secure=true, description="try using induction to verify programs with loops")
  private boolean induction = false;

  @Option(secure=true, description="Strategy for generating auxiliary invariants")
  private InvariantGeneratorFactory invariantGenerationStrategy = InvariantGeneratorFactory.REACHED_SET;

  @Option(
    secure = true,
    description =
        "Controls how long the invariant generator is allowed to run before the k-induction procedure starts."
  )
  private InvariantGeneratorHeadStartFactories invariantGeneratorHeadStartStrategy =
      InvariantGeneratorHeadStartFactories.NONE;

  @Option(
    secure = true,
    description =
        "k-induction configuration to be used as an invariant generator for k-induction (ki-ki(-ai))."
  )
  @FileOption(value = Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path invariantGeneratorConfig = null;

  @Option(secure=true, description="Propagates the interrupts of the invariant generator.")
  private boolean propagateInvGenInterrupts = false;

  @Option(
    secure = true,
    description = "Use generalized counterexamples to induction as candidate invariants."
  )
  private boolean usePropertyDirection = false;

  protected final BMCStatistics stats;
  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;

  private final @Nullable ConfigurableProgramAnalysis stepCaseCPA;
  private final @Nullable Algorithm stepCaseAlgorithm;

  protected final InvariantGenerator invariantGenerator;
  private final InvariantGeneratorHeadStart invariantGeneratorHeadStart;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  protected final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;
  private final Specification specification;

  protected final ShutdownNotifier shutdownNotifier;

  private final TargetLocationProvider targetLocationProvider;

  private final @Nullable ShutdownRequestListener propagateSafetyInterrupt;

  private final AbstractionStrategy abstractionStrategy;

  /** The candidate invariants that have been proven to hold at the loop heads. */
  private final Set<CandidateInvariant> confirmedCandidates = new CopyOnWriteArraySet<>();

  private final List<ConditionAdjustmentEventSubscriber> conditionAdjustmentEventSubscribers =
      new CopyOnWriteArrayList<>();

  protected AbstractBMCAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      final ShutdownManager pShutdownManager,
      CFA pCFA,
      final Specification pSpecification,
      BMCStatistics pBMCStatistics,
      boolean pIsInvariantGenerator,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    pConfig.inject(this, AbstractBMCAlgorithm.class);

    stats = pBMCStatistics;
    algorithm = pAlgorithm;
    cpa = pCPA;
    logger = pLogger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCFA;
    specification = checkNotNull(pSpecification);

    shutdownNotifier = pShutdownManager.getNotifier();
    TestTargetCPA testCPA = CPAs.retrieveCPA(pCPA, TestTargetCPA.class);
    if (testCPA != null) {
      targetLocationProvider =
          new TestTargetLocationProvider(
              ((TestTargetTransferRelation) testCPA.getTransferRelation()).getTestTargets());
    } else {
      targetLocationProvider = new CachingTargetLocationProvider(shutdownNotifier, logger, cfa);
    }

    if (induction) {
      induction = checkIfInductionIsPossible(pCFA, pLogger);
      // if there is no loop we do not need induction, although loop information is available
      induction =
          induction
              && cfa.getLoopStructure().orElseThrow().getCount() > 0
              && !getLoopHeads().isEmpty();
    }

    if (induction) {
      LogManager stepCaseLogger = logger.withComponentName("InductionStepCase");
      CPABuilder builder =
          new CPABuilder(
              pConfig, stepCaseLogger, pShutdownManager.getNotifier(), pReachedSetFactory);
      stepCaseCPA = builder.buildCPAs(cfa, pSpecification, new AggregatedReachedSets());
      stepCaseAlgorithm =
          CPAAlgorithm.create(stepCaseCPA, stepCaseLogger, pConfig, pShutdownManager.getNotifier());
    } else {
      stepCaseCPA = null;
      stepCaseAlgorithm = null;
      invariantGenerationStrategy = InvariantGeneratorFactory.DO_NOTHING;
      invariantGeneratorHeadStartStrategy = InvariantGeneratorHeadStartFactories.NONE;
    }

    ShutdownManager invariantGeneratorShutdownManager = pShutdownManager;
    boolean addInvariantsByInduction = invariantGenerationStrategy == InvariantGeneratorFactory.INDUCTION;
    if (addInvariantsByInduction) {
      if (propagateInvGenInterrupts) {
        invariantGeneratorShutdownManager = pShutdownManager;
      } else {
        invariantGeneratorShutdownManager = ShutdownManager.createWithParent(pShutdownManager.getNotifier());
      }
      propagateSafetyInterrupt = new ShutdownRequestListener() {

        @Override
        public void shutdownRequested(String pReason) {
          if (invariantGenerator != null && invariantGenerator.isProgramSafe()) {
            pShutdownManager.requestShutdown(pReason);
          }
        }
      };
      invariantGeneratorShutdownManager.getNotifier().register(propagateSafetyInterrupt);
    } else {
      propagateSafetyInterrupt = null;
    }

    if (pIsInvariantGenerator && addInvariantsByInduction) {
      invariantGenerationStrategy = InvariantGeneratorFactory.REACHED_SET;
    }
    Configuration invGenConfig = pConfig;
    if (invariantGeneratorConfig != null) {
      try {
        invGenConfig =
            Configuration.builder()
                .copyFrom(invGenConfig)
                .loadFromFile(invariantGeneratorConfig)
                .build();
      } catch (IOException e) {
        throw new InvalidConfigurationException(
            String.format(
                "Cannot load configuration from file %s", invariantGeneratorConfig),
            e);
      }
    }
    invariantGenerator =
        invariantGenerationStrategy.createInvariantGenerator(
            invGenConfig,
            pLogger,
            pReachedSetFactory,
            invariantGeneratorShutdownManager,
            pCFA,
            pSpecification,
            pAggregatedReachedSets,
            targetLocationProvider);
    if (invariantGenerator instanceof ConditionAdjustmentEventSubscriber) {
      conditionAdjustmentEventSubscribers.add(
          (ConditionAdjustmentEventSubscriber) invariantGenerator);
    }
    invariantGeneratorHeadStart = invariantGeneratorHeadStartStrategy.createFor(this);

    @SuppressWarnings("resource")
    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, BMCAlgorithm.class);
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
    abstractionStrategy = new PredicateAbstractionStrategy(cfa.getVarClassification());
  }

  static boolean checkIfInductionIsPossible(CFA cfa, LogManager logger) {
    if (!cfa.getLoopStructure().isPresent()) {
      logger.log(Level.WARNING, "Could not use induction for proving program safety, loop structure of program could not be determined.");
      return false;
    }

    return true;
  }

  /**
   * The run method for interpolation-based model checking
   *
   * @param reachedSet Abstract Reachability Graph (ARG)
   *
   * @return {@code AlgorithmStatus.UNSOUND_AND_PRECISE} if an error location is reached, i.e.,
   *         unsafe; {@code AlgorithmStatus.SOUND_AND_PRECISE} if a fixed point is reached, i.e.,
   *         safe.
   */
  public AlgorithmStatus runInterpolation(final ReachedSet reachedSet)
      throws CPAException, SolverException, InterruptedException {
    Preconditions.checkState(
        cfa.getAllLoopHeads().isPresent() && cfa.getAllLoopHeads().get().size() <= 1,
        "NZ: programs with multiple loops are not supported yet");

    try (ProverEnvironmentWithFallback prover =
        new ProverEnvironmentWithFallback(solver, ProverOptions.GENERATE_MODELS)) {
      PathFormula prefixFormula = pmgr.makeEmptyPathFormula();
      BooleanFormula loopFormula = bfmgr.makeTrue();
      BooleanFormula tailFormula = bfmgr.makeTrue();
      do {
        int maxLoopIterations = CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();

        // step1: unroll with large-block encoding
        shutdownNotifier.shutdownIfNecessary();
        logger.log(
            Level.INFO,
            "NZ: unrolling the program with large-block encoding, maxLoopIterations = "
                + maxLoopIterations);
        stats.bmcPreparation.start();
        BMCHelper.unroll(logger, reachedSet, algorithm, cpa);
        stats.bmcPreparation.stop();
        shutdownNotifier.shutdownIfNecessary();

        // step2: collect prefix, loop, and suffix formulas
        logger.log(Level.INFO, "NZ: collecting prefix, loop, and suffix formulas");
        if (maxLoopIterations == 1) {
          if (existErrorBeforeLoop(reachedSet, prover)) {
            logger.log(Level.INFO, "NZ: there exist reachable errors before the loop");
            return AlgorithmStatus.UNSOUND_AND_PRECISE;
          }
          prefixFormula = getLoopHeadFormula(reachedSet, maxLoopIterations - 1);
        }
        else if (maxLoopIterations == 2) {
          loopFormula = getLoopHeadFormula(reachedSet, maxLoopIterations - 1).getFormula();
        }
        else {
          tailFormula =
              bfmgr
                  .and(
                      tailFormula,
                      getLoopHeadFormula(reachedSet, maxLoopIterations - 1).getFormula());
        }
        BooleanFormula suffixFormula =
            bfmgr.and(tailFormula, getErrorFormula(reachedSet, maxLoopIterations - 1));
//        logger.log(Level.INFO, "NZ: the prefix is " + prefixFormula.getFormula().toString());
//        logger.log(Level.INFO, "NZ: the loop is " + loopFormula.toString());
//        logger.log(Level.INFO, "NZ: the suffix is " + suffixFormula.toString());

        // step3: perform bounded model checking
        logger.log(Level.INFO, "NZ: perform bounded model checking");
        BooleanFormula reachErrorFormula =
            bfmgr.and(prefixFormula.getFormula(), loopFormula, suffixFormula);
        boolean reachable = boundedModelCheckWithLargeBlockEncoding(prover, reachErrorFormula);
        if (reachable) {
          logger.log(Level.INFO, "NZ: an error is reached by BMC");
          return AlgorithmStatus.UNSOUND_AND_PRECISE;
        }
        else {
          logger.log(
              Level.INFO,
              "NZ: the program is safe up to maxLoopIterations = " + maxLoopIterations);
          if (reachedSet.hasViolatedProperties()) {
            TargetLocationCandidateInvariant.INSTANCE.assumeTruth(reachedSet);
          }
          // step3.5: perform forward condition checking
          BooleanFormula forwardConditionFormula =
              bfmgr.and(
                  prefixFormula.getFormula(),
                  loopFormula,
                  tailFormula,
                  getLoopHeadFormula(reachedSet, maxLoopIterations).getFormula());
          boolean forward = forwardConditionCheck(prover, forwardConditionFormula);
          if (!forward) {
            logger.log(
                Level.INFO,
                "NZ: the program is safe as it cannot be unrolled forward at maxLoopIterations = "
                    + maxLoopIterations);
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }

        // step4: perform fixed point computation by interpolation
        if (interpolation && maxLoopIterations > 1) {
          logger.log(
              Level.INFO,
              "NZ: compute fixed points by interpolation at maxLoopIterations = "
                  + maxLoopIterations);
          boolean safe =
              computeFixedPointByInterpolation(
                  prover,
                  prefixFormula.getFormula(),
                  loopFormula,
                  suffixFormula,
                  prefixFormula.getSsa());
          if (safe) {
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }
      } while (adjustConditions());
    }
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private PathFormula getLoopHeadFormula(ReachedSet pReachedSet, int numEncounterLoopHead) {
    List<AbstractState> loopHead =
        from(pReachedSet)
            .filter(
                e -> AbstractStates.extractStateByType(e, LocationState.class)
                    .getLocationNode()
                    .isLoopStart())
            .filter(
                e -> AbstractStates.extractStateByType(e, LoopBoundState.class)
                    .getDeepestIteration()
                    - 1 == numEncounterLoopHead)
            .toList();
    if (loopHead.size() != 1) {
      logger
          .log(
              Level.SEVERE,
              "NZ: no unique loop head at encounter time = " + numEncounterLoopHead);
      assert false;
    }
    return PredicateAbstractState.getPredicateState(loopHead.get(0))
        .getAbstractionFormula()
        .getBlockFormula();
  }

  private BooleanFormula getErrorFormula(ReachedSet pReachedSet, int numEncounterLoopHead) {
    List<AbstractState> errorLocations =
        from(pReachedSet).filter(AbstractStates.IS_TARGET_STATE)
            .filter(
                e -> AbstractStates.extractStateByType(e, LoopBoundState.class)
                    .getDeepestIteration()
                    - 1 == numEncounterLoopHead)
            .toList();
    BooleanFormula formulaToErrorLocations = bfmgr.makeFalse();
    for (AbstractState pErrorState : errorLocations) {
      formulaToErrorLocations =
          bfmgr.or(
              formulaToErrorLocations,
              PredicateAbstractState.getPredicateState(pErrorState)
                  .getAbstractionFormula()
                  .getBlockFormula()
                  .getFormula());
    }
    return formulaToErrorLocations;
  }

  private boolean
      existErrorBeforeLoop(ReachedSet pReachedSet, ProverEnvironmentWithFallback pProver)
          throws InterruptedException, SolverException {
    BooleanFormula formulaToErrorLocations = getErrorFormula(pReachedSet, -1);
    try {
      pProver.push(formulaToErrorLocations);
      return !pProver.isUnsat();
    }
    catch (InterruptedException | SolverException e) {
      logger.log(Level.WARNING, "NZ: an exception happened during checking errors before the loop");
      throw e;
    }
  }

  private boolean formulaCheckSat(ProverEnvironmentWithFallback pProver, BooleanFormula pFormula)
      throws InterruptedException, SolverException {
    while (!pProver.isEmpty()) {
      pProver.pop();
    }
    pProver.push(pFormula);
    return !pProver.isUnsat();
  }

  private boolean boundedModelCheckWithLargeBlockEncoding(
      ProverEnvironmentWithFallback pProver,
      BooleanFormula pReachErrorFormula)
      throws InterruptedException, SolverException {
    try {
      return formulaCheckSat(pProver, pReachErrorFormula);
    } catch (InterruptedException | SolverException e) {
      logger.log(Level.WARNING, "NZ: an exception happened during BMC phase");
      throw e;
    }
  }

  private boolean forwardConditionCheck(
      ProverEnvironmentWithFallback pProver,
      BooleanFormula pForwardConditionFormula)
      throws InterruptedException, SolverException {
    try {
      return formulaCheckSat(pProver, pForwardConditionFormula);
    } catch (InterruptedException | SolverException e) {
      logger.log(Level.WARNING, "NZ: an exception happened during forward checking phase");
      throw e;
    }
  }

  private boolean reachFixedPointCheck(
      ProverEnvironmentWithFallback pProver,
      BooleanFormula pInterpolantFormula,
      BooleanFormula pCurrentImageFormula)
      throws InterruptedException, SolverException {
    try {
      BooleanFormula pNotImplicationFormula =
          bfmgr.not(bfmgr.implication(pInterpolantFormula, pCurrentImageFormula));
      return !formulaCheckSat(pProver, pNotImplicationFormula);
    } catch (InterruptedException | SolverException e) {
      logger.log(Level.WARNING, "NZ: an exception happened during fixed point checking phase");
      throw e;
    }
  }

  private boolean computeFixedPointByInterpolation(
      ProverEnvironmentWithFallback pProver,
      BooleanFormula pPrefixFormula,
      BooleanFormula pLoopFormula,
      BooleanFormula pSuffixFormula,
      SSAMap prefixSsaMap) {
    try (ProverEnvironmentWithFallback proverStack =
        new ProverEnvironmentWithFallback(solver, ProverOptions.GENERATE_UNSAT_CORE)) {

      Stack<Object> formulaA = new Stack<>();
      Stack<Object> formulaB = new Stack<>();
      formulaB.push(proverStack.push(pSuffixFormula));
      formulaA.push(proverStack.push(pLoopFormula));
      formulaA.push(proverStack.push(pPrefixFormula));
      // TODO: how to clone a BooleanFormula? pPrefixFormula will be changed via currentImage!
      BooleanFormula currentImage = bfmgr.and(pPrefixFormula, bfmgr.makeTrue());
      BooleanFormula interpolant = null;

      while (proverStack.isUnsat()) {
        interpolant = proverStack.getInterpolant(formulaA);
        logger.log(Level.INFO, "NZ: the prefix is " + pPrefixFormula.toString());
        logger.log(Level.INFO, "NZ: the SSA map is " + prefixSsaMap.toString());
        logger.log(
            Level.INFO,
            "NZ: the interpolant before changing index is " + interpolant.toString());
        // interpolant = bfmgr.not(proverStack.getInterpolant(formulaB));
        interpolant = fmgr.instantiate(fmgr.uninstantiate(interpolant), prefixSsaMap);
        logger.log(
            Level.INFO,
            "NZ: the interpolant after changing index is " + interpolant.toString());
        boolean reachFixedPoint = reachFixedPointCheck(pProver, interpolant, currentImage);
        if (reachFixedPoint) {
          logger.log(Level.INFO, "NZ: the current image is a fixed point, property proved");
          return true;
        }
        currentImage = bfmgr.or(currentImage, interpolant);
        logger.log(Level.INFO, "NZ: current image is " + currentImage.toString());
        proverStack.pop();
        formulaA.pop();
        formulaA.push(proverStack.push(interpolant));
      }
      logger.log(Level.INFO, "NZ: the overapproximation is unsafe, go back to BMC phase");
      return false;
    } catch (InterruptedException | SolverException e) {
      logger.log(Level.WARNING, "NZ: an exception happened during interpolation phase");
      return false;
    }
  }

  /**
   * Compute fixed points by interpolation
   *
   * @return {@code true} if a fixed point is reached, {@code false} if the current
   *         over-approximation is unsafe.
   */
  private boolean computeFixedPointByInterpolation(final ReachedSet reachedSet) {
    logger.log(Level.INFO, "NZ: Computing fixed points by interpolation, under construction");
    /*
     * Algorithmic steps (a block ends when a loop head is encountered)
     *
     * step1: get error path
     *
     * Q1: multiple error locations?
     *
     * Q2: error location inside the loop versus after the loop?
     *
     * step2: get block formulas; prefix=1st block, loop=2nd block, suffix=rest blocks
     *
     * step3: allocate a new stack with interpolation
     *
     * step4: mark prefix and loop as A, suffix as B; push suffix, loop, prefix
     *
     * step5: if not UNSAT (SAT or timeout), return false; else, go to step6
     *
     * step6: get the next interpolant and change its SSA indices to the biggest indices in prefix
     *
     * step7: if the next interpolant equals the prev interpolant, return true; else, go to step8
     *
     * step8: pop the prev interpolant and push the next interpolant; go to step5
     *
     */

    // step1: get an error path (Q1: multiple error locations; Q2: errors inside the loop)
    Optional<AbstractState> optionalTargetState =
        from(reachedSet).firstMatch(AbstractStates.IS_TARGET_STATE);
    if (!optionalTargetState.isPresent()) {
      logger.log(
          Level.WARNING,
          "NZ: no target state is found for fixed point computation");
      return false;
    }
    AbstractState targetState = optionalTargetState.get();
    logger.log(Level.INFO, "NZ: the target state is " + targetState.toString());
    ARGPath errorPath = ARGUtils.getShortestPathTo((ARGState) targetState);

    // step2: slice the formula into prefix, loop, and suffix
    PathIterator pathIterator = errorPath.fullPathIterator();
    List<ARGState> slicingPoints = new ArrayList<>();
    while (pathIterator.hasNext()) {
      if (slicingPoints.size() == 2) {
        // only the first and the second loop heads are needed
        break;
      }
      if (pathIterator.getLocation().isLoopStart()) {
        slicingPoints.add(pathIterator.getAbstractState());
      }
      pathIterator.advance();
    }
    Preconditions
        .checkState(slicingPoints.size() == 2, "NZ: the error path does not have two loop heads");
    // add the target state to the slicing points
    slicingPoints.add((ARGState) targetState);
    BlockFormulaSlicer slicer = new BlockFormulaSlicer(pmgr);
    BlockFormulas blockFormulas = null;
    try {
      blockFormulas = slicer.getFormulasForPath(errorPath.getFirstState(), slicingPoints);
    } catch (CPATransferException | InterruptedException e) {
      logger.log(Level.WARNING, "NZ: an exception happened during formula slicing");
      return false;
    }
    Preconditions
        .checkState(blockFormulas.getSize() == 3, "NZ: there are not three block formulas");
    BooleanFormula prefixFormula = blockFormulas.getFormulas().get(0);
    logger.log(Level.INFO, "NZ: the prefix formula is " + prefixFormula.toString());
    BooleanFormula loopFormula = blockFormulas.getFormulas().get(1);
    logger.log(Level.INFO, "NZ: the loop formula is " + loopFormula.toString());
    BooleanFormula suffixFormula = blockFormulas.getFormulas().get(2);
    logger.log(Level.INFO, "NZ: the suffix formula is " + suffixFormula.toString());

    // step3: allocate a proof stack with interpolation
    try (ProverEnvironmentWithFallback proverStack =
        new ProverEnvironmentWithFallback(
            solver,
            ProverOptions.GENERATE_UNSAT_CORE)) {

      // step4: label formulas
      Stack<Object> formulaA = new Stack<>();
      Stack<Object> formulaB = new Stack<>();
      formulaB.push(proverStack.push(suffixFormula));
      formulaA.push(proverStack.push(loopFormula));
      formulaA.push(proverStack.push(prefixFormula));

      // step5-8: the main interpolation loop for fixed point computation
      BooleanFormula currentImage = prefixFormula; // TODO: how to clone? prefixFormula will be
                                                   // changed via currentImage!
      BooleanFormula interpolant = null;
      while (proverStack.isUnsat()) {
        interpolant = proverStack.getInterpolant(formulaA);
        //interpolant = bfmgr.not(proverStack.getInterpolant(formulaB));
        logger.log(Level.INFO, "NZ: the interpolant is " + interpolant.toString());
        interpolant = changeSSAIndices(interpolant, prefixFormula);
        // TODO: it seems that bfmgr does not do solving but only compares objects?
        // The following line checks if the interpolant discovers new states; if not, a fixed
        // point is reached
        if (bfmgr.isTrue(bfmgr.implication(interpolant, currentImage))) {
          logger
              .log(Level.INFO, "NZ: the current image is a fixed point, property proved");
          return true;
        }
        currentImage = bfmgr.or(currentImage, interpolant);
        logger.log(Level.INFO, "NZ: current image is " + currentImage.toString());
        proverStack.pop();
        formulaA.pop();
        formulaA.push(proverStack.push(interpolant));
      }
      logger.log(Level.INFO, "NZ: the overapproximation is unsafe, go back to BMC phase");
      return false;
    } catch (InterruptedException | SolverException e) {
      logger.log(Level.WARNING, "NZ: an exception happened during interpolation phase");
      return false;
    }
  }

  private BooleanFormula changeSSAIndices(BooleanFormula f, BooleanFormula g) {
    // TODO: change the SSA indices of f to that of g
    return f;
  }

  public AlgorithmStatus run(final ReachedSet reachedSet) throws CPAException,
      SolverException,
      InterruptedException {
    CFANode initialLocation = extractLocation(reachedSet.getFirstState());
    invariantGenerator.start(initialLocation);

    // The set of candidate invariants that still need to be checked.
    // Successfully proven invariants are removed from the set.
    final CandidateGenerator candidateGenerator = getCandidateInvariants();
    Set<Obligation> ctiBlockingClauses = new TreeSet<>();
    Map<SymbolicCandiateInvariant, BmcResult> checkedClauses = new HashMap<>();

    if (!candidateGenerator.produceMoreCandidates()) {
      for (AbstractState state : ImmutableList.copyOf(reachedSet.getWaitlist())) {
        reachedSet.removeOnlyFromWaitlist(state);
      }
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    AlgorithmStatus status;

    try (ProverEnvironmentWithFallback prover =
        new ProverEnvironmentWithFallback(solver, ProverOptions.GENERATE_MODELS)) {
      invariantGeneratorHeadStart.waitForInvariantGenerator();

      do {
        shutdownNotifier.shutdownIfNecessary();

        logger.log(Level.INFO, "Creating formula for program");
        stats.bmcPreparation.start();
        status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);
        stats.bmcPreparation.stop();
        if (from(reachedSet)
            .skip(1) // first state of reached is always an abstraction state, so skip it
            .filter(not(IS_TARGET_STATE)) // target states may be abstraction states
            .anyMatch(PredicateAbstractState.CONTAINS_ABSTRACTION_STATE)) {

          logger.log(Level.WARNING, "BMC algorithm does not work with abstractions. Could not check for satisfiability!");
          return status;
        }
        shutdownNotifier.shutdownIfNecessary();

        if (invariantGenerator.isProgramSafe()) {
          TargetLocationCandidateInvariant.INSTANCE.assumeTruth(reachedSet);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }

        // Perform a bounded model check on each candidate invariant
        Iterator<CandidateInvariant> candidateInvariantIterator = candidateGenerator.iterator();
        while (candidateInvariantIterator.hasNext()) {
          shutdownNotifier.shutdownIfNecessary();
          CandidateInvariant candidateInvariant = candidateInvariantIterator.next();
          // first check safety in k iterations

          boolean safe = boundedModelCheck(reachedSet, prover, candidateInvariant);
          if (!safe) {
            if (candidateInvariant == TargetLocationCandidateInvariant.INSTANCE) {
              return AlgorithmStatus.UNSOUND_AND_PRECISE;
            }
            candidateInvariantIterator.remove();
          }

          if (invariantGenerator.isProgramSafe()) {
            TargetLocationCandidateInvariant.INSTANCE.assumeTruth(reachedSet);
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }

        // second check soundness
        boolean sound;

        // verify soundness, but don't bother if we are unsound anyway or we have found a bug
        if (status.isSound()) {

          // check bounding assertions
          sound = candidateGenerator.hasCandidatesAvailable() ? checkBoundingAssertions(reachedSet, prover) : true;

          if (invariantGenerator.isProgramSafe()) {
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }

          // try to prove program safety via induction
          if (induction && !sound) {
            if (usePropertyDirection) {
              usePropertyDirection =
                  refineCtiBlockingClauses(reachedSet, prover, ctiBlockingClauses, checkedClauses);
              if (!usePropertyDirection) {
                ctiBlockingClauses.clear();
              }
            }
            try (@SuppressWarnings("resource")
                KInductionProver kInductionProver = createInductionProver()) {
            sound =
                checkStepCase(reachedSet, candidateGenerator, kInductionProver, ctiBlockingClauses);
            }
          }
          if (invariantGenerator.isProgramSafe()
              || (sound && !candidateGenerator.produceMoreCandidates())) {
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }

        if (!candidateGenerator.hasCandidatesAvailable()) {
          // no remaining invariants to be proven
          return status;
        }
      }
      while (status.isSound() && adjustConditions());
    }

    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private boolean checkStepCase(
      final ReachedSet reachedSet,
      final CandidateGenerator candidateGenerator,
      KInductionProver kInductionProver,
      Set<Obligation> pCtiBlockingClauses)
      throws InterruptedException, CPAException, SolverException {

    final int k = CPAs.retrieveCPA(cpa, LoopIterationBounding.class).getMaxLoopIterations();

    Set<Object> checkedKeys = getCheckedKeys(reachedSet);
    Predicate<CandidateInvariant> isApplicable =
        getCandidateApplicabilityPredicate(reachedSet, checkedKeys);

    Set<CandidateInvariant> candidates =
        FluentIterable.concat(pCtiBlockingClauses, candidateGenerator).filter(isApplicable).toSet();
    Set<SymbolicCandiateInvariant> checked = new HashSet<>();

    shutdownNotifier.shutdownIfNecessary();

    boolean sound = true;
    Iterable<CandidateInvariant> candidatesToCheck = candidates;
    for (CandidateInvariant candidate : candidatesToCheck) {
      // No need to check the same clause twice
      if (candidate instanceof Obligation) {
        if (!checked.add(((Obligation) candidate).getBlockingClause())) {
          continue;
        }
        pCtiBlockingClauses.remove(candidate);
      }

      boolean extractCtiBlockingClauses = usePropertyDirection;

      Lifting lifting =
          extractCtiBlockingClauses
              ? new AbstractionBasedLifting(
                  abstractionStrategy, AbstractionBasedLifting.RefinementLAFStrategies.EAGER)
              : StandardLiftings.NO_LIFTING;

      InductionResult<CandidateInvariant> inductionResult =
          kInductionProver.check(
              Iterables.concat(confirmedCandidates, Collections.singleton(candidate)),
              k,
              candidate,
              checkedKeys,
              InvariantStrengthenings.noStrengthening(),
              lifting);
      if (inductionResult.isSuccessful()) {
        Iterables.addAll(
            confirmedCandidates,
            CandidateInvariantCombination.getConjunctiveParts(candidate));
        candidateGenerator.confirmCandidates(
            CandidateInvariantCombination.getConjunctiveParts(candidate));
        if (candidate == TargetLocationCandidateInvariant.INSTANCE) {
          sound = true;
          break;
        }
      } else {
        sound = false;

        if (candidate instanceof Obligation) {
          Obligation obligation = (Obligation) candidate;
          List<SymbolicCandiateInvariant> weakenings = obligation.getWeakenings();
          for (SymbolicCandiateInvariant weakening : weakenings) {
            inductionResult =
                kInductionProver.check(
                    Iterables.concat(confirmedCandidates, Collections.singleton(weakening)),
                    k,
                    weakening,
                    checkedKeys,
                    InvariantStrengthenings.noStrengthening(),
                    lifting);
            if (inductionResult.isSuccessful()) {
              Iterables.addAll(
                  confirmedCandidates,
                  CandidateInvariantCombination.getConjunctiveParts(weakening));
              candidateGenerator.confirmCandidates(
                  CandidateInvariantCombination.getConjunctiveParts(weakening));
              break;
            }
          }
        }

        if (!inductionResult.isSuccessful() && extractCtiBlockingClauses) {
          FluentIterable<? extends CandidateInvariant> causes =
              from(CandidateInvariantCombination.getConjunctiveParts(candidate));
          if (causes.anyMatch(Obligation.class::isInstance)) {
            causes = causes.filter(Obligation.class);
          }
          if (!causes.isEmpty()) {
            for (SymbolicCandiateInvariant badStateBlockingClause :
                inductionResult.getBadStateBlockingClauses()) {
              pCtiBlockingClauses.add(
                  new Obligation(causes.iterator().next(), badStateBlockingClause));
            }
          }
        }
      }
    }
    return sound;
  }

  /**
   * Gets all keys of loop-iteration reporting states that were reached by unrolling.
   *
   * @param pReachedSet the reached set.
   * @return all keys of loop-iteration reporting states that were reached by unrolling.
   */
  private Set<Object> getCheckedKeys(ReachedSet pReachedSet) {
    return AbstractStates.filterLocations(pReachedSet, getLoopHeads())
        .transform(s -> AbstractStates.extractStateByType(s, LoopIterationReportingState.class))
        .transform(LoopIterationReportingState::getPartitionKey)
        .toSet();
  }

  /**
   * Gets the candidate invariants to be checked.
   *
   * @return the candidate invariants to be checked.
   */
  protected abstract CandidateGenerator getCandidateInvariants();

  /**
   * Adjusts the conditions of those CPAs that support the adjustment of conditions.
   *
   * @return {@code true} if the conditions were adjusted, {@code false} if no further adjustment is
   *     possible.
   */
  private boolean adjustConditions() {
    FluentIterable<AdjustableConditionCPA> conditionCPAs =
        CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class);
    boolean adjusted = false;
    for (AdjustableConditionCPA condCpa : conditionCPAs) {
      if (condCpa.adjustPrecision()) {
        adjusted = true;
      }
    }
    if (!adjusted) {
      // these cpas said "do not continue"
      logger.log(
          Level.INFO,
          "Terminating because none of the following CPAs' precision can be adjusted any further ",
          Joiner.on(", ")
              .join(
                  conditionCPAs.transform(
                      conditionCpa -> conditionCpa.getClass().getSimpleName())));
      return false;
    }
    return !Iterables.isEmpty(conditionCPAs);
  }

  // NZ: begin of the modified bounded model check, which delays the removal of target states
  protected boolean boundedModelCheckNoRemoveTargetStates(
      final ReachedSet pReachedSet,
      final ProverEnvironmentWithFallback pProver,
      CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException, SolverException {
    return boundedModelCheckNoRemoveTargetStates(
        (Iterable<AbstractState>) pReachedSet,
        pProver,
        pCandidateInvariant);
  }

  private boolean boundedModelCheckNoRemoveTargetStates(
      Iterable<AbstractState> pReachedSet,
      ProverEnvironmentWithFallback pProver,
      CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException, SolverException {
    BooleanFormula program = bfmgr.not(pCandidateInvariant.getAssertion(pReachedSet, fmgr, pmgr));
    logger.log(Level.INFO, "Starting satisfiability check...");
    stats.satCheck.start();
    pProver.push(program);
    boolean safe = pProver.isUnsat();
    stats.satCheck.stop();
    // Leave program formula on solver stack until error path is created

    if (pReachedSet instanceof ReachedSet) {
      ReachedSet reachedSet = (ReachedSet) pReachedSet;
      if (safe) {
        // NZ: do not remove target states now because they are used in the interpolation phase
        // pCandidateInvariant.assumeTruth(reachedSet);
      } else if (pCandidateInvariant == TargetLocationCandidateInvariant.INSTANCE) {
        analyzeCounterexample(program, reachedSet, pProver);
      }
    }

    pProver.pop();

    return safe;
  }
  // NZ: end of the modified bounded model check, which delays the removal of target states

  protected boolean boundedModelCheck(
      final ReachedSet pReachedSet,
      final ProverEnvironmentWithFallback pProver,
      CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException, SolverException {
    return boundedModelCheck((Iterable<AbstractState>) pReachedSet, pProver, pCandidateInvariant);
  }

  private boolean boundedModelCheck(
      Iterable<AbstractState> pReachedSet,
      ProverEnvironmentWithFallback pProver,
      CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException, SolverException {
    BooleanFormula program = bfmgr.not(pCandidateInvariant.getAssertion(pReachedSet, fmgr, pmgr));
    logger.log(Level.INFO, "Starting satisfiability check...");
    stats.satCheck.start();
    pProver.push(program);
    boolean safe = pProver.isUnsat();
    stats.satCheck.stop();
    // Leave program formula on solver stack until error path is created

    if (pReachedSet instanceof ReachedSet) {
      ReachedSet reachedSet = (ReachedSet) pReachedSet;
      if (safe) {
        pCandidateInvariant.assumeTruth(reachedSet);
      } else if (pCandidateInvariant == TargetLocationCandidateInvariant.INSTANCE) {
        analyzeCounterexample(program, reachedSet, pProver);
      }
    }

    pProver.pop();

    return safe;
  }

  private boolean refineCtiBlockingClauses(
      ReachedSet pReachedSet,
      ProverEnvironmentWithFallback pProver,
      Set<Obligation> pCtiBlockingClauses,
      Map<SymbolicCandiateInvariant, BmcResult> pCheckedClauses)
      throws CPATransferException, InterruptedException, SolverException {

    Map<SymbolicCandiateInvariant, SymbolicCandiateInvariant> refinedBlockingClauses =
        new HashMap<>();

    for (SymbolicCandiateInvariant blockingClause :
        Iterables.transform(pCtiBlockingClauses, Obligation::getBlockingClause)) {
      if (refinedBlockingClauses.containsKey(blockingClause)) {
        continue;
      }

      BooleanFormula liftedCti = bfmgr.not(blockingClause.getPlainFormula(fmgr));

      // Add literals until unsat
      ToIntFunction<BooleanFormula> variableNameCount = f -> fmgr.extractVariableNames(f).size();
      Queue<BooleanFormula> literals =
          new PriorityQueue<>(Comparator.comparingInt(variableNameCount).reversed());
      Iterables.addAll(
          literals, SymbolicCandiateInvariant.getConjunctionOperands(fmgr, liftedCti, true));

      Iterator<BooleanFormula> literalIterator = literals.iterator();
      List<BooleanFormula> requiredLiterals = new ArrayList<>();
      boolean isUnsat = false;
      SymbolicCandiateInvariant newBlockingClause = blockingClause;
      while (!isUnsat && literalIterator.hasNext()) {
        BooleanFormula literal = literalIterator.next();
        requiredLiterals.add(literal);
        if (literalIterator.hasNext() && fmgr.extractVariableNames(literal).size() > 1) {
          continue;
        }

        BooleanFormula furtherReducedCti = bfmgr.and(requiredLiterals);
        BooleanFormula newBlockingClauseFormula = bfmgr.not(furtherReducedCti);
        newBlockingClause =
            SymbolicCandiateInvariant.makeSymbolicInvariant(
                blockingClause.getApplicableLocations(),
                blockingClause.getStateFilter(),
                newBlockingClauseFormula,
                fmgr);

        BmcResult clauseResult = pCheckedClauses.get(newBlockingClause);
        if (clauseResult == null) {
          clauseResult = new BmcResult();
          pCheckedClauses.put(newBlockingClause, clauseResult);
        }
        if (clauseResult.isSafe()) {
          Iterable<AbstractState> applicableStates =
              newBlockingClause.filterApplicable(pReachedSet);
          applicableStates = clauseResult.filterUnchecked(applicableStates);
          isUnsat = boundedModelCheck(applicableStates, pProver, newBlockingClause);
          if (isUnsat) {
            clauseResult.addSafeStates(applicableStates);
          } else {
            clauseResult.declareUnsafe();
          }
        }
      }
      if (isUnsat) {
        if (requiredLiterals.size() == literals.size()) {
          refinedBlockingClauses.put(blockingClause, blockingClause);
        } else {
          refinedBlockingClauses.put(blockingClause, newBlockingClause);
        }
      }
    }

    Iterator<Obligation> obligationIterator = pCtiBlockingClauses.iterator();
    List<Obligation> newObligations = new ArrayList<>();
    while (obligationIterator.hasNext()) {
      Obligation obligation = obligationIterator.next();
      SymbolicCandiateInvariant refinedClause =
          refinedBlockingClauses.get(obligation.getBlockingClause());
      if (refinedClause != obligation.getBlockingClause()) {
        obligationIterator.remove();
        if (refinedClause != null) {
          newObligations.add(obligation.refineWith(fmgr, refinedClause));
        } else {
          if (obligation.getDepth() == 0
              && obligation.getRootCause() instanceof TargetLocationCandidateInvariant) {
            return false;
          }
        }
      }
    }
    pCtiBlockingClauses.addAll(newObligations);
    return true;
  }

  /**
   * This method is called after a violation has been found (i.e., the bounded-model-checking
   * formula was satisfied). The formula is still on the solver stack. Subclasses can use this
   * method to further analyze the counterexample if necessary.
   *
   * @param pCounterexample the satisfiable formula that contains the specification violation
   * @param pReachedSet the reached used for analyzing
   * @param pProver the prover that was used (has pCounterexample formula pushed onto it)
   * @throws CPATransferException may be thrown in subclasses
   * @throws InterruptedException may be thrown in subclasses
   */
  protected void analyzeCounterexample(
      final BooleanFormula pCounterexample,
      final ReachedSet pReachedSet,
      final ProverEnvironmentWithFallback pProver)
      throws CPATransferException, InterruptedException {
    // by default, do nothing (just a hook for subclasses)
  }

  /**
   * Checks if the bounded unrolling completely unrolled all reachable loop iterations by performing
   * a satisfiablity check on the formulas encoding the reachability of the states where the bounded
   * model check stopped due to reaching the bound.
   *
   * <p>If this is is the case, then the bounded model check is guaranteed to be sound.
   *
   * @param pReachedSet the reached set containing the frontier of the bounded model check, i.e.
   *     where the bounded model check stopped.
   * @param prover the prover to be used to prove that the stop states are unreachable.
   * @return {@code true} if the bounded model check covered all reachable states and was thus
   *     sound, {@code false} otherwise.
   * @throws InterruptedException if the satisfiability check is interrupted.
   */
  private boolean checkBoundingAssertions(
      final ReachedSet pReachedSet, final ProverEnvironmentWithFallback prover)
      throws SolverException, InterruptedException {
    FluentIterable<AbstractState> stopStates = from(pReachedSet)
        .filter(IS_STOP_STATE)
        .filter(Predicates.not(IS_SLICED_STATE));

    if (boundingAssertions) {
      // create formula for unwinding assertions
      BooleanFormula assertions = BMCHelper.createFormulaFor(stopStates, bfmgr);

      logger.log(Level.INFO, "Starting assertions check...");

      stats.assertionsCheck.start();
      prover.push(assertions);
      boolean sound = prover.isUnsat();
      prover.pop();
      stats.assertionsCheck.stop();

      logger.log(Level.FINER, "Soundness after assertion checks:", sound);
      return sound;

    } else {
      // fast check for trivial cases
      return stopStates.isEmpty();
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
    if (invariantGenerator instanceof StatisticsProvider) {
      ((StatisticsProvider)invariantGenerator).collectStatistics(pStatsCollection);
    }
  }

  protected KInductionProver createInductionProver() {
    assert induction;
    return new KInductionProver(
        cfa,
        logger,
        stepCaseAlgorithm,
        stepCaseCPA,
        invariantGenerator,
        stats,
        reachedSetFactory,
        shutdownNotifier,
        getLoopHeads(),
        usePropertyDirection);
  }

  /**
   * Gets the potential target locations.
   *
   * @return the potential target locations.
   */
  protected Collection<CFANode> getTargetLocations() {
    return targetLocationProvider.tryGetAutomatonTargetLocations(
        cfa.getMainFunction(), specification);
  }

  /**
   * Gets the loop heads.
   *
   * @return the loop heads.
   */
  protected Set<CFANode> getLoopHeads() {
    return BMCHelper.getLoopHeads(cfa, targetLocationProvider);
  }

  public enum InvariantGeneratorFactory {
    INDUCTION {

      @Override
      InvariantGenerator createInvariantGenerator(
          Configuration pConfig,
          LogManager pLogger,
          ReachedSetFactory pReachedSetFactory,
          ShutdownManager pShutdownManager,
          CFA pCFA,
          Specification pSpecification,
          AggregatedReachedSets pAggregatedReachedSets,
          TargetLocationProvider pTargetLocationProvider)
          throws InvalidConfigurationException, CPAException, InterruptedException {
        return
            KInductionInvariantGenerator.create(
                pConfig,
                pLogger,
                pShutdownManager,
                pCFA,
                pSpecification,
                pReachedSetFactory,
                pTargetLocationProvider,
                pAggregatedReachedSets);
      }
    },

    REACHED_SET {
      @Override
      InvariantGenerator createInvariantGenerator(
          Configuration pConfig,
          LogManager pLogger,
          ReachedSetFactory pReachedSetFactory,
          ShutdownManager pShutdownManager,
          CFA pCFA,
          Specification pSpecification,
          AggregatedReachedSets pAggregatedReachedSets,
          TargetLocationProvider pTargetLocationProvider) {
        return new AbstractInvariantGenerator() {

          @Override
          protected void startImpl(CFANode pInitialLocation) {
            // do nothing
          }

          @Override
          public boolean isProgramSafe() {
            // just return false, program will be ended by parallel algorithm if the invariant
            // generator can prove safety before the current analysis
            return false;
          }

          @Override
          public void cancel() {
            // do nothing
          }

          @Override
          public AggregatedReachedSets get() throws CPAException, InterruptedException {
            return pAggregatedReachedSets;
          }
        };
      }
    },

    DO_NOTHING {

      @Override
      InvariantGenerator createInvariantGenerator(
          Configuration pConfig,
          LogManager pLogger,
          ReachedSetFactory pReachedSetFactory,
          ShutdownManager pShutdownManager,
          CFA pCFA,
          Specification pSpecification,
          AggregatedReachedSets pAggregatedReachedSets,
          TargetLocationProvider pTargetLocationProvider) {
        return new DoNothingInvariantGenerator();
      }

    };

    abstract InvariantGenerator createInvariantGenerator(
        Configuration pConfig,
        LogManager pLogger,
        ReachedSetFactory pReachedSetFactory,
        ShutdownManager pShutdownManager,
        CFA pCFA,
        Specification pSpecification,
        AggregatedReachedSets pAggregatedReachedSets,
        TargetLocationProvider pTargetLocationProvider)
        throws InvalidConfigurationException, CPAException, InterruptedException;
  }

  protected FluentIterable<CandidateInvariant> getConfirmedCandidates(final CFANode pLocation) {
    return from(confirmedCandidates)
        .filter(pConfirmedCandidate -> pConfirmedCandidate.appliesTo(pLocation));
  }

  private Predicate<CandidateInvariant> getCandidateApplicabilityPredicate(
      ReachedSet pReached, Set<Object> pCheckedKeys) {
    Map<Loop, Integer> reachedK;
    int maxK = 0;
    if (cfa.getLoopStructure().isPresent()) {
      reachedK = new HashMap<>();
      for (AbstractState checkedState :
          BMCHelper.filterBmcChecked(
              AbstractStates.filterLocations(pReached, getLoopHeads()), pCheckedKeys)) {
        for (CFANode location : AbstractStates.extractLocations(checkedState)) {
          LoopIterationReportingState lirs =
              AbstractStates.extractStateByType(checkedState, LoopIterationReportingState.class);
          if (lirs != null) {
            for (Loop loop : cfa.getLoopStructure().orElseThrow().getLoopsForLoopHead(location)) {
              Integer previous = reachedK.get(loop);
              int iteration = lirs.getIteration(loop);
              if (previous == null || previous < iteration) {
                reachedK.put(loop, iteration);
                maxK = Math.max(maxK, iteration);
              }
            }
          }
        }
      }
    } else {
      reachedK = ImmutableMap.of();
    }
    int finalMaxK = maxK;
    return (candidate) -> {
      if (candidate == TargetLocationCandidateInvariant.INSTANCE) {
        return true;
      }
      if (!cfa.getLoopStructure().isPresent()) {
        return getLoopHeads().isEmpty();
      }
      Set<CFANode> locations =
          AbstractStates.extractLocations(candidate.filterApplicable(pReached)).toSet();
      if (locations.isEmpty()) {
        return false;
      }
      for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
        for (CFANode location : locations) {
          if (loop.getLoopNodes().contains(location) && reachedK.get(loop) < finalMaxK) {
            return false;
          }
        }
      }
      return true;
    };
  }

  @Override
  public void adjustmentSuccessful(ConfigurableProgramAnalysis pCpa) {
    for (ConditionAdjustmentEventSubscriber caes : conditionAdjustmentEventSubscribers) {
      caes.adjustmentSuccessful(pCpa);
    }
  }

  @Override
  public void adjustmentRefused(ConfigurableProgramAnalysis pCpa) {
    for (ConditionAdjustmentEventSubscriber caes : conditionAdjustmentEventSubscribers) {
      caes.adjustmentRefused(pCpa);
    }
  }

  private static class Obligation implements CandidateInvariant, Comparable<Obligation> {

    private final CandidateInvariant causingCandidateInvariant;

    private final @Nullable Obligation causingObligation;

    private final SymbolicCandiateInvariant blockingClause;

    private final List<SymbolicCandiateInvariant> weakenings;

    private int hashCode = 0;

    private Obligation(
        CandidateInvariant pCause,
        SymbolicCandiateInvariant pBlockingClause,
        List<SymbolicCandiateInvariant> pStrengthening) {
      if (pCause instanceof Obligation) {
        causingObligation = (Obligation) pCause;
        causingCandidateInvariant = causingObligation.causingCandidateInvariant;
      } else {
        causingCandidateInvariant = Objects.requireNonNull(pCause);
        causingObligation = null;
      }
      blockingClause = Objects.requireNonNull(pBlockingClause);
      weakenings = ImmutableList.copyOf(pStrengthening);
    }

    public Obligation(CandidateInvariant pCause, SymbolicCandiateInvariant pBlockingClause) {
      this(pCause, pBlockingClause, ImmutableList.of());
    }

    public int getDepth() {
      int depth = 0;
      Obligation current = this;
      while (current.causingObligation != null) {
        current = current.causingObligation;
        ++depth;
      }
      assert (depth == 0 && causingObligation == null) || (depth > 0 && causingObligation != null);
      return depth;
    }

    @Override
    public String toString() {
      return blockingClause.toString();
    }

    public CandidateInvariant getRootCause() {
      return causingCandidateInvariant;
    }

    public SymbolicCandiateInvariant getBlockingClause() {
      return blockingClause;
    }

    public List<SymbolicCandiateInvariant> getWeakenings() {
      return weakenings;
    }

    public Obligation refineWith(
        FormulaManagerView pFmgr, SymbolicCandiateInvariant pRefinedBlockingClause)
        throws InterruptedException {
      if (pRefinedBlockingClause == blockingClause) {
        return this;
      }

      BooleanFormulaManager bfmgr = pFmgr.getBooleanFormulaManager();
      Set<BooleanFormula> reducedLiftedCti =
          ImmutableSet.copyOf(
              SymbolicCandiateInvariant.getConjunctionOperands(
                  pFmgr, bfmgr.not(pRefinedBlockingClause.getPlainFormula(pFmgr)), true));
      List<BooleanFormula> remainingLiterals =
          from(SymbolicCandiateInvariant.getConjunctionOperands(
                  pFmgr, bfmgr.not(blockingClause.getPlainFormula(pFmgr)), true))
              .filter(not(Predicates.in(reducedLiftedCti)))
              .toList();
      BooleanFormula strengthened = bfmgr.and(reducedLiftedCti);
      List<SymbolicCandiateInvariant> weakenedInvariants =
          new ArrayList<>(remainingLiterals.size());
      for (BooleanFormula remainingLiteral : remainingLiterals) {
        strengthened = bfmgr.and(strengthened, remainingLiteral);
        weakenedInvariants.add(
            SymbolicCandiateInvariant.makeSymbolicInvariant(
                blockingClause.getApplicableLocations(),
                blockingClause.getStateFilter(),
                bfmgr.not(strengthened),
                pFmgr));
      }

      if (causingObligation == null) {
        return new Obligation(
            causingCandidateInvariant, pRefinedBlockingClause, weakenedInvariants);
      }
      return new Obligation(causingObligation, pRefinedBlockingClause, weakenedInvariants);
    }

    @Override
    public int hashCode() {
      if (hashCode != 0) {
        return hashCode;
      }
      if (causingObligation != null) {
        return hashCode = Objects.hash(causingObligation, blockingClause, weakenings);
      }
      return hashCode = Objects.hash(causingCandidateInvariant, blockingClause, weakenings);
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof Obligation) {
        Obligation other = (Obligation) pOther;
        if (causingObligation == null) {
          return other.causingObligation == null
              && causingCandidateInvariant.equals(other.causingCandidateInvariant)
              && blockingClause.equals(other.blockingClause)
              && weakenings.equals(other.weakenings);
        }
        return causingObligation.equals(other.causingObligation)
            && blockingClause.equals(other.blockingClause)
            && weakenings.equals(other.weakenings);
      }
      return false;
    }

    @Override
    public BooleanFormula getFormula(
        FormulaManagerView pFmgr, PathFormulaManager pPfmgr, @Nullable PathFormula pContext)
        throws CPATransferException, InterruptedException {
      return blockingClause.getFormula(pFmgr, pPfmgr, pContext);
    }

    @Override
    public BooleanFormula getAssertion(
        Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
        throws CPATransferException, InterruptedException {
      return blockingClause.getAssertion(pReachedSet, pFMGR, pPFMGR);
    }

    @Override
    public void assumeTruth(ReachedSet pReachedSet) {
      blockingClause.assumeTruth(pReachedSet);
    }

    @Override
    public boolean appliesTo(CFANode pLocation) {
      return blockingClause.appliesTo(pLocation);
    }

    @Override
    public int compareTo(Obligation pObligation) {
      if (this == pObligation) {
        return 0;
      }
      return Integer.compare(getDepth(), pObligation.getDepth());
    }
  }

  private static class BmcResult {

    private final Set<AbstractState> checkedStates = new HashSet<>();

    private boolean safe = true;

    public void addSafeStates(Iterable<AbstractState> pSafeStates) {
      Iterables.addAll(checkedStates, pSafeStates);
    }

    public void declareUnsafe() {
      safe = false;
      checkedStates.clear();
    }

    public boolean isSafe() {
      return safe;
    }

    public Iterable<AbstractState> filterUnchecked(Iterable<AbstractState> pStates) {
      checkState(isSafe(), "A counterexample was found already.");
      return Iterables.filter(pStates, Predicates.not(Predicates.in(checkedStates)));
    }
  }

  private interface InvariantGeneratorHeadStart {

    void waitForInvariantGenerator() throws InterruptedException;
  }

  private enum InvariantGeneratorHeadStartFactories {
    NONE {

      @Override
      public InvariantGeneratorHeadStart createFor(AbstractBMCAlgorithm pBmcAlgorithm) {
        return new InvariantGeneratorHeadStart() {

          @Override
          public void waitForInvariantGenerator() throws InterruptedException {
            // Return immediately
          }
        };
      }
    },

    AWAIT_TERMINATION {

      @Override
      public InvariantGeneratorHeadStart createFor(AbstractBMCAlgorithm pBmcAlgorithm) {
        CountDownLatch latch = new CountDownLatch(1);
        pBmcAlgorithm.conditionAdjustmentEventSubscribers.add(
            new ConditionAdjustmentEventSubscriber() {

              @Override
              public void adjustmentSuccessful(ConfigurableProgramAnalysis pCpa) {
                // Ignore
              }

              @Override
              public void adjustmentRefused(ConfigurableProgramAnalysis pCpa) {
                latch.countDown();
              }
            });
        return new HeadStartWithLatch(pBmcAlgorithm, latch);
      }
    },

    WAIT_UNTIL_EXPENSIVE_ADJUSTMENT {

      @Override
      InvariantGeneratorHeadStart createFor(AbstractBMCAlgorithm pBmcAlgorithm) {
        CountDownLatch latch = new CountDownLatch(1);
        pBmcAlgorithm.conditionAdjustmentEventSubscribers.add(
            new ConditionAdjustmentEventSubscriber() {

              @Override
              public void adjustmentSuccessful(ConfigurableProgramAnalysis pCpa) {
                FluentIterable<InvariantsCPA> cpas =
                    CPAs.asIterable(pCpa).filter(InvariantsCPA.class);
                if (cpas.isEmpty()) {
                  latch.countDown();
                } else {
                  for (InvariantsCPA invariantCpa : cpas) {
                    if (invariantCpa.isLikelyLongRunning()) {
                      latch.countDown();
                      break;
                    }
                  }
                }
              }

              @Override
              public void adjustmentRefused(ConfigurableProgramAnalysis pCpa) {
                latch.countDown();
              }
            });
        return new HeadStartWithLatch(pBmcAlgorithm, latch);
      }
    };

    private static final class HeadStartWithLatch implements InvariantGeneratorHeadStart {

      private final CountDownLatch latch;

      @SuppressWarnings("UnnecessaryAnonymousClass") // ShutdownNotifier needs a strong reference
      private final ShutdownRequestListener shutdownListener =
          new ShutdownRequestListener() {

            @Override
            public void shutdownRequested(String pReason) {
              latch.countDown();
            }
          };

      public HeadStartWithLatch(AbstractBMCAlgorithm pBmcAlgorithm, CountDownLatch pLatch) {
        latch = Objects.requireNonNull(pLatch);
        pBmcAlgorithm.shutdownNotifier.registerAndCheckImmediately(shutdownListener);
      }

      @Override
      public void waitForInvariantGenerator() throws InterruptedException {
        latch.await();
      }
    }

    abstract InvariantGeneratorHeadStart createFor(AbstractBMCAlgorithm pBmcAlgorithm);
  }
}