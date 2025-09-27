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

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.ForOverride;
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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.util.predicates.AssignmentToPathAllocator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model;
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
          "File with predicate precisions "
              + "that should be used as candidate invariants in k-induction",
      name = "kinduction.predicatePrecisionFile")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path initialPredicatePrecisionFile = null;

  @Option(
      secure = true,
      description =
          "Correctness witness in 2.x format (for previous program version) "
              + "that should be used in regression verification"
              + "to get candidate invariants for k-induction",
      name = "kinduction.regression.witnessFile")
  @FileOption(value = Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path witnessFileForRegressionVerification = null;

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

  private final AbstractionStrategy abstractionStrategy;

  /** The candidate invariants that have been proven to hold at the loop heads. */
  private final Set<CandidateInvariant> confirmedCandidates = new CopyOnWriteArraySet<>();

  protected final InvariantGeneratorForBMC invariantGeneratorForBMC;

  private final ImmutableSet<CandidateInvariant> predicatePrecisionCandidates;
  private @Nullable PredicateToKInductionInvariantConverter predToKIndInv;

  private ImmutableSet<CandidateInvariant> candidateInvariantsFromWitness = null;

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
    }

    @SuppressWarnings("resource")
    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, BMCAlgorithm.class);
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
    abstractionStrategy = new PredicateAbstractionStrategy(cfa.getVarClassification());
    assignmentToPathAllocator =
        new AssignmentToPathAllocator(config, shutdownNotifier, pLogger, pCFA.getMachineModel());

    invariantGeneratorForBMC =
        new InvariantGeneratorForBMC(
            pShutdownManager,
            pIsInvariantGenerator,
            pConfig,
            pLogger,
            pReachedSetFactory,
            pCFA,
            pSpecification,
            pAggregatedReachedSets,
            targetLocationProvider,
            bfmgr.makeTrue());

    if (initialPredicatePrecisionFile != null) {
      predToKIndInv =
          new PredicateToKInductionInvariantConverter(config, logger, shutdownNotifier, cfa);
      predicatePrecisionCandidates =
          predToKIndInv.convertPredPrecToKInductionInvariant(
              initialPredicatePrecisionFile, solver, predCpa.getAbstractionManager());
    } else {
      predicatePrecisionCandidates = ImmutableSet.of();
    }

    if (witnessFileForRegressionVerification != null) {
      candidateInvariantsFromWitness =
          new RegressionVerificationWitnessToCandidateInvariantsConverter(
                  config, logger, shutdownNotifier, fmgr, pmgr, cfa)
              .getCandidateInvariantsFromWitness(witnessFileForRegressionVerification);
    } else {
      candidateInvariantsFromWitness = ImmutableSet.of();
    }
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
    final CandidateGenerator candidateGenerator;
    AlgorithmStatus status;
    final Set<Obligation> ctiBlockingClauses;
    final Map<SymbolicCandiateInvariant, BmcResult> checkedClauses;

    stats.bmcPreparation.start();
    try {
      CFANode initialLocation = extractLocation(reachedSet.getFirstState());
      invariantGeneratorForBMC.start(initialLocation);

      // The set of candidate invariants that still need to be checked.
      // Successfully proven invariants are removed from the set.
      candidateGenerator = getCandidateInvariants();
      ctiBlockingClauses = new TreeSet<>();
      checkedClauses = new HashMap<>();

      if (!candidateGenerator.produceMoreCandidates()) {
        reachedSet.clearWaitlist();
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }

      // suggest candidates from predicate precision file
      if (!predicatePrecisionCandidates.isEmpty()) {
        candidateGenerator.suggestCandidates(predicatePrecisionCandidates);
      }

      // suggest candidates from Witness 2.0 file
      if (!candidateInvariantsFromWitness.isEmpty()) {
        candidateGenerator.suggestCandidates(candidateInvariantsFromWitness);
      }
    } finally {
      stats.bmcPreparation.stop();
    }

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      invariantGeneratorForBMC.waitForHeadStart();

      do {
        shutdownNotifier.shutdownIfNecessary();

        logger.log(Level.INFO, "Creating formula for program");
        stats.bmcUnrolling.start();
        try {
          status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);
        } finally {
          stats.bmcUnrolling.stop();
        }
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

        if (invariantGeneratorForBMC.isProgramSafe()) {
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

          if (invariantGeneratorForBMC.isProgramSafe()) {
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

          if (invariantGeneratorForBMC.isProgramSafe()) {
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
          if (invariantGeneratorForBMC.isProgramSafe()
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
      if (candidate instanceof Obligation obligation) {
        if (!checked.add(obligation.getBlockingClause())) {
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
      if (!extractCtiBlockingClauses
          && candidate instanceof SingleLocationFormulaInvariant singleLocationFormulaInvariant) {
        checkedKeys =
            getCheckedKeysAtLocation(reachedSet, singleLocationFormulaInvariant.getLocation());
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

        if (candidate instanceof Obligation obligation) {
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
  protected final boolean adjustConditions() {
    FluentIterable<AdjustableConditionCPA> conditionCPAs =
        CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class);
    boolean adjusted = conditionCPAs.anyMatch(AdjustableConditionCPA::adjustPrecision);
    if (!adjusted) {
      // these cpas said "do not continue"
      logger.log(
          Level.INFO,
          "Terminating because none of the following CPAs' precision can be adjusted any further ",
          conditionCPAs
              .transform(conditionCpa -> conditionCpa.getClass().getSimpleName())
              .join(Joiner.on(", ")));
    }
    return adjusted;
  }

  @ForOverride
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
    final boolean safe;
    try {
      pProver.push(program);
      safe = pProver.isUnsat();
    } finally {
      stats.satCheck.stop();
    }
    // Leave program formula on solver stack until error path is created

    if (pReachedSet instanceof ReachedSet reachedSet) {
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
  protected final Optional<CounterexampleInfo> analyzeCounterexample0(
      final BooleanFormula pCounterexampleFormula,
      final ReachedSet pReachedSet,
      final BasicProverEnvironment<?> pProver)
      throws CPATransferException, InterruptedException {
    if (!(cpa instanceof ARGCPA)) {
      logger.log(Level.INFO, "Error found, but error path cannot be created without ARGCPA");
      return Optional.empty();
    }

    // get (precise) error path
    logger.log(Level.INFO, "Error found, creating error path");
    ARGPath targetPath;

    stats.errorPathCreation.start();
    try {
      Set<ARGState> targetStates =
          from(pReachedSet).filter(AbstractStates::isTargetState).filter(ARGState.class).toSet();
      Set<ARGState> redundantStates = filterAncestors(targetStates);
      redundantStates.forEach(ARGState::removeFromARG);
      pReachedSet.removeAll(redundantStates);

      try (Model model = pProver.getModel()) {
        ARGState root = (ARGState) pReachedSet.getFirstState();

        try {
          targetPath =
              pmgr.getARGPathFromModel(
                  model, root, ARGUtils.getAllStatesOnPathsTo(targetStates)::contains);
        } catch (IllegalArgumentException e) {
          logger.logUserException(Level.WARNING, e, "Could not create error path");
          return Optional.empty();
        }

        if (!targetPath.getLastState().isTarget()) {
          logger.log(Level.WARNING, "Could not create error path: path ends without target state!");
          return Optional.empty();
        }

      } catch (SolverException e) {
        logger.log(Level.WARNING, "Solver could not produce model, cannot create error path.");
        logger.logDebugException(e);
        return Optional.empty();
      }
    } finally {
      stats.errorPathCreation.stop();
    }

    stats.errorPathProcessing.start();
    try {
      BooleanFormula cexFormula = pCounterexampleFormula;

      // replay error path for a more precise satisfying assignment
      PathChecker pathChecker;
      try {
        Solver solverForPathChecker = solver;
        PathFormulaManager pmgrForPathChecker = pmgr;

        if (Ascii.toLowerCase(solverForPathChecker.getVersion()).contains("smtinterpol")) {
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
          CounterexampleTraceInfo.feasible(ImmutableList.of(cexFormula), targetPath);
      return Optional.of(pathChecker.handleFeasibleCounterexample(cexInfo, targetPath));

    } finally {
      stats.errorPathProcessing.stop();
    }
  }

  /**
   * Checks if the bounded unrolling completely unrolled all reachable loop iterations by performing
   * a satisfiablity check on the formulas encoding the reachability of the states where the bounded
   * model check stopped due to reaching the bound.
   *
   * <p>If this is the case, then the bounded model check is guaranteed to be sound.
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
        try {
          prover.push(assertions);
          sound = prover.isUnsat();
          prover.pop();
        } finally {
          stats.assertionsCheck.stop();
        }
      } else {
        List<AbstractState> toRemove = new ArrayList<>();
        for (AbstractState s : stopStates) {
          // create individual formula for unwinding assertions
          BooleanFormula assertions = BMCHelper.createFormulaFor(ImmutableList.of(s), bfmgr);
          stats.assertionsCheck.start();
          final boolean result;
          try {
            prover.push(assertions);
            result = prover.isUnsat();
            prover.pop();
          } finally {
            stats.assertionsCheck.stop();
          }
          sound &= result;
          if (result) {
            toRemove.add(s);
          }
        }
        for (AbstractState s : toRemove) {
          pReachedSet.remove(s);
          if (s instanceof ARGState aRGState) {
            aRGState.removeFromARG();
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
    if (algorithm instanceof StatisticsProvider statisticsProvider) {
      statisticsProvider.collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
    invariantGeneratorForBMC.collectStatistics(pStatsCollection);
    if (predToKIndInv != null) {
      pStatsCollection.add(predToKIndInv);
    }
  }

  @ForOverride
  protected KInductionProver createInductionProver() {
    assert induction;
    return new KInductionProver(
        cfa,
        logger,
        stepCaseAlgorithm,
        stepCaseCPA,
        invariantGeneratorForBMC,
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
  protected final Collection<CFANode> getTargetLocations() {
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

  protected final FluentIterable<CandidateInvariant> getConfirmedCandidates(
      final CFANode pLocation) {
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
    return candidate -> {
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
    invariantGeneratorForBMC.adjustmentSuccessful(pCpa);
  }

  @Override
  public void adjustmentRefused(ConfigurableProgramAnalysis pCpa) {
    invariantGeneratorForBMC.adjustmentRefused(pCpa);
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
      if (pCause instanceof Obligation obligation) {
        causingObligation = obligation;
        causingCandidateInvariant = causingObligation.causingCandidateInvariant;
      } else {
        causingCandidateInvariant = Objects.requireNonNull(pCause);
        causingObligation = null;
      }
      blockingClause = Objects.requireNonNull(pBlockingClause);
      weakenings = ImmutableList.copyOf(pStrengthening);
    }

    Obligation(CandidateInvariant pCause, SymbolicCandiateInvariant pBlockingClause) {
      this(pCause, pBlockingClause, ImmutableList.of());
    }

    int getDepth() {
      int depth = 0;
      Obligation current = this;
      while (current.causingObligation != null) {
        current = current.causingObligation;
        ++depth;
      }
      assert causingObligation == null ? depth == 0 : depth > 0;
      return depth;
    }

    @Override
    public String toString() {
      return blockingClause.toString();
    }

    CandidateInvariant getRootCause() {
      return causingCandidateInvariant;
    }

    SymbolicCandiateInvariant getBlockingClause() {
      return blockingClause;
    }

    List<SymbolicCandiateInvariant> getWeakenings() {
      return weakenings;
    }

    Obligation refineWith(
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
        hashCode = Objects.hash(causingObligation, blockingClause, weakenings);
      } else {
        hashCode = Objects.hash(causingCandidateInvariant, blockingClause, weakenings);
      }
      return hashCode;
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }

      if (pOther instanceof Obligation other
          && blockingClause.equals(other.blockingClause)
          && weakenings.equals(other.weakenings)) {

        // If we have a causing obligation we can just delegate the rest to it.
        if (causingObligation == null) {
          return other.causingObligation == null
              && causingCandidateInvariant.equals(other.causingCandidateInvariant);
        } else {
          return causingObligation.equals(other.causingObligation);
        }
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

    void addSafeStates(Iterable<AbstractState> pSafeStates) {
      Iterables.addAll(checkedStates, pSafeStates);
    }

    void declareUnsafe() {
      safe = false;
      checkedStates.clear();
    }

    boolean isSafe() {
      return safe;
    }

    Iterable<AbstractState> filterUnchecked(Iterable<AbstractState> pStates) {
      checkState(isSafe(), "A counterexample was found already.");
      return Iterables.filter(pStates, Predicates.not(Predicates.in(checkedStates)));
    }
  }
}
