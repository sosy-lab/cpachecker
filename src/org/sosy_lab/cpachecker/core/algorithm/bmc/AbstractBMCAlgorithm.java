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
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
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
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import javax.annotation.Nullable;
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
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.targetreachability.ReachabilityState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
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
      throws InvalidConfigurationException, CPAException {

    pConfig.inject(this, AbstractBMCAlgorithm.class);

    stats = pBMCStatistics;
    algorithm = pAlgorithm;
    cpa = pCPA;
    logger = pLogger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCFA;
    specification = checkNotNull(pSpecification);

    shutdownNotifier = pShutdownManager.getNotifier();
    targetLocationProvider = new CachingTargetLocationProvider(shutdownNotifier, logger, cfa);

    if (induction) {
      induction = checkIfInductionIsPossible(pCFA, pLogger);
      // if there is no loop we do not need induction, although loop information is available
      induction = induction && cfa.getLoopStructure().get().getCount() > 0 && !getLoopHeads().isEmpty();
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
          InvariantGenerator invariantGenerator = AbstractBMCAlgorithm.this.invariantGenerator;
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
      for (AbstractState state : from(reachedSet.getWaitlist()).toList()) {
        reachedSet.removeOnlyFromWaitlist(state);
      }
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    AlgorithmStatus status;

    try (ProverEnvironmentWithFallback prover =
            new ProverEnvironmentWithFallback(solver, ProverOptions.GENERATE_MODELS);
        @SuppressWarnings("resource")
            KInductionProver kInductionProver = createInductionProver()) {
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
            sound =
                checkStepCase(reachedSet, candidateGenerator, kInductionProver, ctiBlockingClauses);
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
      Queue<BooleanFormula> literals = new PriorityQueue<>(new BooleanFormulaComparator(fmgr));
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

  protected @Nullable KInductionProver createInductionProver() {
    return induction
        ? new KInductionProver(
            cfa,
            logger,
            stepCaseAlgorithm,
            stepCaseCPA,
            invariantGenerator,
            stats,
            reachedSetFactory,
            shutdownNotifier,
            getLoopHeads(),
            usePropertyDirection)
        : null;
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

  private static final class BooleanFormulaComparator implements Comparator<BooleanFormula> {

    private final FormulaManagerView fmgr;

    public BooleanFormulaComparator(FormulaManagerView pFmgr) {
      fmgr = Objects.requireNonNull(pFmgr);
    }

    @Override
    public int compare(BooleanFormula pO1, BooleanFormula pO2) {
      Set<String> leftVariableNames = fmgr.extractVariableNames(pO1);
      Set<String> rightVariableNames = fmgr.extractVariableNames(pO2);
      return ComparisonChain.start()
          .compare(rightVariableNames.size(), leftVariableNames.size())
          .result();
    }
  }

  public static enum InvariantGeneratorFactory {

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
          TargetLocationProvider pTargetLocationProvider) throws InvalidConfigurationException, CPAException {
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
        TargetLocationProvider pTargetLocationProvider) throws InvalidConfigurationException, CPAException;

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
            for (Loop loop : cfa.getLoopStructure().get().getLoopsForLoopHead(location)) {
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
      reachedK = Collections.emptyMap();
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
      for (Loop loop : cfa.getLoopStructure().get().getAllLoops()) {
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
      this(pCause, pBlockingClause, Collections.emptyList());
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
          from(SymbolicCandiateInvariant.getConjunctionOperands(
                  pFmgr, bfmgr.not(pRefinedBlockingClause.getPlainFormula(pFmgr)), true))
              .toSet();
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
      if (!isSafe()) {
        throw new IllegalStateException("A counterexample was found already.");
      }
      return Iterables.filter(pStates, Predicates.not(Predicates.in(checkedStates)));
    }
  }

  private static interface InvariantGeneratorHeadStart {

    void waitForInvariantGenerator() throws InterruptedException;
  }

  private static enum InvariantGeneratorHeadStartFactories {

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
                  for (InvariantsCPA cpa : cpas) {
                    if (cpa.isLikelyLongRunning()) {
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