// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.filterAncestors;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.math.BigInteger;
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
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
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
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ConditionAdjustmentEventSubscriber;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariantCombination;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SingleLocationFormulaInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.AbstractInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.DoNothingInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
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
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
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
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitnessGenerator;
import org.sosy_lab.cpachecker.util.predicates.AssignmentToPathAllocator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.invariants.FormulaInvariantsSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "bmc")
abstract class AbstractBMCAlgorithm
    implements StatisticsProvider, ConditionAdjustmentEventSubscriber {

  protected static boolean isStopState(AbstractState state) {
    AssumptionStorageState assumptionState =
        AbstractStates.extractStateByType(state, AssumptionStorageState.class);
    return assumptionState != null && assumptionState.isStop();
  }

  /** Filters out states that were detected as irrelevant for reachability */
  protected static boolean isRelevantForReachability(AbstractState state) {
    return AbstractStates.extractStateByType(state, ReachabilityState.class)
        != ReachabilityState.IRRELEVANT_TO_TARGET;
  }

  @Option(
      secure = true,
      description =
          "If BMC did not find a bug, check whether "
              + "the bounding did actually remove parts of the state space "
              + "(this is similar to CBMC's unwinding assertions).")
  private boolean boundingAssertions = true;

  @Option(
      secure = true,
      description =
          "If BMC did not find a bug, check which parts of the boundary actually reachable"
              + "and prevent them from being unrolled any further.")
  private boolean boundingAssertionsSlicing = false;

  @Option(secure = true, description = "try using induction to verify programs with loops")
  private boolean induction = false;

  @Option(secure = true, description = "Strategy for generating auxiliary invariants")
  private InvariantGeneratorFactory invariantGenerationStrategy =
      InvariantGeneratorFactory.REACHED_SET;

  @Option(
      secure = true,
      description =
          "Controls how long the invariant generator is allowed to run before the k-induction"
              + " procedure starts.")
  private InvariantGeneratorHeadStartFactories invariantGeneratorHeadStartStrategy =
      InvariantGeneratorHeadStartFactories.NONE;

  @Option(
      secure = true,
      description =
          "k-induction configuration to be used as an invariant generator for k-induction"
              + " (ki-ki(-ai)).")
  @FileOption(value = Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path invariantGeneratorConfig = null;

  @Option(secure = true, description = "Propagates the interrupts of the invariant generator.")
  private boolean propagateInvGenInterrupts = false;

  @Option(
      secure = true,
      description = "Use generalized counterexamples to induction as candidate invariants.")
  private boolean usePropertyDirection = false;

  @Option(
      secure = true,
      description =
          "Try to simplify the structure of formulas for the sat check of BMC. "
              + "The improvement depends on the underlying SMT solver.")
  private boolean simplifyBooleanFormula = false;

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
  private final Configuration config;
  private final CFA cfa;
  private final AssignmentToPathAllocator assignmentToPathAllocator;
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
    config = pConfig;
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
      stepCaseCPA = builder.buildCPAs(cfa, pSpecification, AggregatedReachedSets.empty());
      stepCaseAlgorithm =
          CPAAlgorithm.create(stepCaseCPA, stepCaseLogger, pConfig, pShutdownManager.getNotifier());
    } else {
      stepCaseCPA = null;
      stepCaseAlgorithm = null;
      invariantGenerationStrategy = InvariantGeneratorFactory.DO_NOTHING;
      invariantGeneratorHeadStartStrategy = InvariantGeneratorHeadStartFactories.NONE;
    }

    ShutdownManager invariantGeneratorShutdownManager = pShutdownManager;
    boolean addInvariantsByInduction =
        invariantGenerationStrategy == InvariantGeneratorFactory.INDUCTION;
    if (addInvariantsByInduction) {
      if (propagateInvGenInterrupts) {
        invariantGeneratorShutdownManager = pShutdownManager;
      } else {
        invariantGeneratorShutdownManager =
            ShutdownManager.createWithParent(pShutdownManager.getNotifier());
      }
      propagateSafetyInterrupt =
          new ShutdownRequestListener() {

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
            String.format("Cannot load configuration from file %s", invariantGeneratorConfig), e);
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
    assignmentToPathAllocator =
        new AssignmentToPathAllocator(config, shutdownNotifier, pLogger, pCFA.getMachineModel());
  }

  static boolean checkIfInductionIsPossible(CFA cfa, LogManager logger) {
    if (!cfa.getLoopStructure().isPresent()) {
      logger.log(
          Level.WARNING,
          "Could not use induction for proving program safety, loop structure of program could not"
              + " be determined.");
      return false;
    }

    return true;
  }

  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, SolverException, InterruptedException {
    CFANode initialLocation = extractLocation(reachedSet.getFirstState());
    invariantGenerator.start(initialLocation);

    // The set of candidate invariants that still need to be checked.
    // Successfully proven invariants are removed from the set.
    final CandidateGenerator candidateGenerator = getCandidateInvariants();
    Set<Obligation> ctiBlockingClauses = new TreeSet<>();
    Map<SymbolicCandiateInvariant, BmcResult> checkedClauses = new HashMap<>();

    if (!candidateGenerator.produceMoreCandidates()) {
      reachedSet.clearWaitlist();
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    AlgorithmStatus status;

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      invariantGeneratorHeadStart.waitForInvariantGenerator();

      do {
        shutdownNotifier.shutdownIfNecessary();

        logger.log(Level.INFO, "Creating formula for program");
        stats.bmcPreparation.start();
        status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);
        stats.bmcPreparation.stop();
        if (from(reachedSet)
            .skip(1) // first state of reached is always an abstraction state, so skip it
            .filter(not(AbstractStates::isTargetState)) // target states may be abstraction states
            .anyMatch(PredicateAbstractState::containsAbstractionState)) {

          logger.log(
              Level.WARNING,
              "BMC algorithm does not work with abstractions. Could not check for satisfiability!");
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
          sound =
              candidateGenerator.hasCandidatesAvailable()
                  ? checkBoundingAssertions(reachedSet, prover)
                  : true;

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
                  checkStepCase(
                      reachedSet, candidateGenerator, kInductionProver, ctiBlockingClauses);
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
      } while (status.isSound() && adjustConditions());
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

      // If we are not running KI-PDR and the candidate invariant is specified at a certain
      // location, checked keys (i.e., loop-bound states) should come from this location.
      if (!extractCtiBlockingClauses && candidate instanceof SingleLocationFormulaInvariant) {
        checkedKeys =
            getCheckedKeysAtLocation(
                reachedSet, ((SingleLocationFormulaInvariant) candidate).getLocation());
      }

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
            confirmedCandidates, CandidateInvariantCombination.getConjunctiveParts(candidate));
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
   * Gets all keys of loop-iteration reporting states at the specified location that were reached by
   * unrolling.
   *
   * @param pReachedSet the reached set.
   * @param pLoc the specified location.
   * @return all keys of loop-iteration reporting states at the specified location that were reached
   *     by unrolling.
   */
  private Set<Object> getCheckedKeysAtLocation(ReachedSet pReachedSet, CFANode pLoc) {
    return from(AbstractStates.filterLocation(pReachedSet, pLoc))
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
  protected boolean adjustConditions() {
    FluentIterable<AdjustableConditionCPA> conditionCPAs =
        CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class);
    boolean adjusted = conditionCPAs.anyMatch(AdjustableConditionCPA::adjustPrecision);
    if (!adjusted) {
      // these cpas said "do not continue"
      logger.log(
          Level.INFO,
          "Terminating because none of the following CPAs' precision can be adjusted any further ",
          Joiner.on(", ")
              .join(
                  conditionCPAs.transform(
                      conditionCpa -> conditionCpa.getClass().getSimpleName())));
    }
    return adjusted;
  }

  protected boolean boundedModelCheck(
      final ReachedSet pReachedSet,
      final BasicProverEnvironment<?> pProver,
      CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException, SolverException {
    return boundedModelCheck((Iterable<AbstractState>) pReachedSet, pProver, pCandidateInvariant);
  }

  private boolean boundedModelCheck(
      Iterable<AbstractState> pReachedSet,
      BasicProverEnvironment<?> pProver,
      CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException, SolverException {
    BooleanFormula program = bfmgr.not(pCandidateInvariant.getAssertion(pReachedSet, fmgr, pmgr));
    if (simplifyBooleanFormula) {
      BigInteger sizeBeforeSimplification = fmgr.countBooleanOperations(program);
      program = fmgr.simplifyBooleanFormula(program);
      BigInteger sizeAfterSimplification = fmgr.countBooleanOperations(program);
      logger.logf(
          Level.FINER,
          "Formula was simplified from %s to %s boolean operations.",
          sizeBeforeSimplification,
          sizeAfterSimplification);
    }
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
      BasicProverEnvironment<?> pProver,
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
      final BasicProverEnvironment<?> pProver)
      throws CPATransferException, InterruptedException {
    // Subclasses can perform their own violation handling (e.g., allow imprecise counterexamples)
    // by overriding this method, and call analyzeCounterexample0() to find satisfying assignments
    analyzeCounterexample0(pCounterexample, pReachedSet, pProver)
        .ifPresent(cex -> cex.getTargetState().addCounterexampleInformation(cex));
  }

  /**
   * This method tries to find a feasible path to (one of) the target state(s). It does so by asking
   * the solver for a satisfying assignment.
   */
  @SuppressWarnings("resource")
  protected Optional<CounterexampleInfo> analyzeCounterexample0(
      final BooleanFormula pCounterexampleFormula,
      final ReachedSet pReachedSet,
      final BasicProverEnvironment<?> pProver)
      throws CPATransferException, InterruptedException {
    if (!(cpa instanceof ARGCPA)) {
      logger.log(Level.INFO, "Error found, but error path cannot be created without ARGCPA");
      return Optional.empty();
    }

    stats.errorPathCreation.start();
    try {
      logger.log(Level.INFO, "Error found, creating error path");

      Set<ARGState> targetStates =
          from(pReachedSet).filter(AbstractStates::isTargetState).filter(ARGState.class).toSet();
      Set<ARGState> redundantStates = filterAncestors(targetStates, AbstractStates::isTargetState);
      redundantStates.forEach(
          state -> {
            state.removeFromARG();
          });
      pReachedSet.removeAll(redundantStates);
      targetStates = Sets.difference(targetStates, redundantStates);

      final boolean shouldCheckBranching;
      if (targetStates.size() == 1) {
        ARGState state = Iterables.getOnlyElement(targetStates);
        while (state.getParents().size() == 1 && state.getChildren().size() <= 1) {
          state = Iterables.getOnlyElement(state.getParents());
        }
        shouldCheckBranching = (state.getParents().size() > 1) || (state.getChildren().size() > 1);
      } else {
        shouldCheckBranching = true;
      }

      if (shouldCheckBranching) {
        Set<ARGState> arg = from(pReachedSet).filter(ARGState.class).toSet();

        // get the branchingFormula
        // this formula contains predicates for all branches we took
        // this way we can figure out which branches make a feasible path
        BooleanFormula branchingFormula = pmgr.buildBranchingFormula(arg);

        if (bfmgr.isTrue(branchingFormula)) {
          logger.log(
              Level.WARNING,
              "Could not create error path because of missing branching information!");
          return Optional.empty();
        }

        // add formula to solver environment
        pProver.push(branchingFormula);
      }

      List<ValueAssignment> model;
      try {
        // need to ask solver for satisfiability again,
        // otherwise model doesn't contain new predicates
        boolean stillSatisfiable = !pProver.isUnsat();

        if (!stillSatisfiable) {
          // should not occur
          logger.log(
              Level.WARNING,
              "Could not create error path information because of inconsistent branching"
                  + " information!");
          return Optional.empty();
        }

        model = pProver.getModelAssignments();

      } catch (SolverException e) {
        logger.log(Level.WARNING, "Solver could not produce model, cannot create error path.");
        logger.logDebugException(e);
        return Optional.empty();

      } finally {
        if (shouldCheckBranching) {
          pProver.pop(); // remove branchingFormula
        }
      }

      // get precise error path
      Map<Integer, Boolean> branchingInformation = pmgr.getBranchingPredicateValuesFromModel(model);
      ARGState root = (ARGState) pReachedSet.getFirstState();

      ARGPath targetPath;
      try {
        Set<AbstractState> arg = pReachedSet.asCollection();
        targetPath = ARGUtils.getPathFromBranchingInformation(root, arg, branchingInformation);
      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "Could not create error path");
        return Optional.empty();
      }

      BooleanFormula cexFormula = pCounterexampleFormula;

      // replay error path for a more precise satisfying assignment
      PathChecker pathChecker;
      try {
        Solver solverForPathChecker = solver;
        PathFormulaManager pmgrForPathChecker = pmgr;

        if (solverForPathChecker.getVersion().toLowerCase().contains("smtinterpol")) {
          // SMTInterpol does not support reusing the same solver
          solverForPathChecker = Solver.create(config, logger, shutdownNotifier);
          FormulaManagerView formulaManager = solverForPathChecker.getFormulaManager();
          pmgrForPathChecker =
              new PathFormulaManagerImpl(
                  formulaManager, config, logger, shutdownNotifier, cfa, AnalysisDirection.FORWARD);
          // cannot dump pCounterexampleFormula, PathChecker would use wrong FormulaManager for it
          cexFormula =
              solverForPathChecker.getFormulaManager().getBooleanFormulaManager().makeTrue();
        }

        pathChecker =
            new PathChecker(
                config,
                logger,
                pmgrForPathChecker,
                solverForPathChecker,
                assignmentToPathAllocator);

      } catch (InvalidConfigurationException e) {
        // Configuration has somehow changed and can no longer be used to create the solver and path
        // formula manager
        logger.logUserException(
            Level.WARNING, e, "Could not replay error path to get a more precise model");
        return Optional.empty();
      }

      CounterexampleTraceInfo cexInfo =
          CounterexampleTraceInfo.feasible(
              ImmutableList.of(cexFormula), model, branchingInformation);
      return Optional.of(pathChecker.createCounterexample(targetPath, cexInfo));

    } finally {
      stats.errorPathCreation.stop();
    }
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
      final ReachedSet pReachedSet, final BasicProverEnvironment<?> prover)
      throws SolverException, InterruptedException {
    FluentIterable<AbstractState> stopStates =
        from(pReachedSet)
            .filter(AbstractBMCAlgorithm::isStopState)
            .filter(AbstractBMCAlgorithm::isRelevantForReachability);

    if (boundingAssertions) {
      logger.log(Level.INFO, "Starting assertions check...");
      boolean sound = true;

      if (!boundingAssertionsSlicing) {
        // create one formula for unwinding assertions
        BooleanFormula assertions = BMCHelper.createFormulaFor(stopStates, bfmgr);
        stats.assertionsCheck.start();
        prover.push(assertions);
        sound = prover.isUnsat();
        prover.pop();
        stats.assertionsCheck.stop();
      } else {
        List<AbstractState> toRemove = new ArrayList<>();
        for (AbstractState s : stopStates) {
          // create individual formula for unwinding assertions
          BooleanFormula assertions = BMCHelper.createFormulaFor(ImmutableList.of(s), bfmgr);
          stats.assertionsCheck.start();
          prover.push(assertions);
          boolean result = prover.isUnsat();
          prover.pop();
          stats.assertionsCheck.stop();
          sound &= result;
          if (result) {
            toRemove.add(s);
          }
        }
        for (AbstractState s : toRemove) {
          pReachedSet.remove(s);
          if (s instanceof ARGState) {
            ((ARGState) s).removeFromARG();
          }
        }
      }
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
      ((StatisticsProvider) algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
    if (invariantGenerator instanceof StatisticsProvider) {
      ((StatisticsProvider) invariantGenerator).collectStatistics(pStatsCollection);
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
        return KInductionInvariantGenerator.create(
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
          public InvariantSupplier getSupplier() throws CPAException, InterruptedException {
            return new FormulaInvariantsSupplier(pAggregatedReachedSets);
          }

          @Override
          public ExpressionTreeSupplier getExpressionTreeSupplier()
              throws CPAException, InterruptedException {
            return new ExpressionTreeInvariantSupplier(pAggregatedReachedSets, pCFA);
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
    },

    INVARIANT_STORE {
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
        try {
          return InvariantWitnessGenerator.getNewFromDiskInvariantGenerator(
              pConfig, pCFA, pLogger, pShutdownManager.getNotifier());
        } catch (IOException e) {
          throw new CPAException("Could not create from disk generator", e);
        }
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
