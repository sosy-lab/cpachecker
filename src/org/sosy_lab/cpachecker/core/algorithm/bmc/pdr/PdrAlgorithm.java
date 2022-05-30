// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.pdr;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.filterAncestors;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.isTrivialSelfLoop;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.AbstractionBasedLifting;
import org.sosy_lab.cpachecker.core.algorithm.bmc.AbstractionBasedLifting.LiftingAbstractionFailureStrategy;
import org.sosy_lab.cpachecker.core.algorithm.bmc.AbstractionStrategy;
import org.sosy_lab.cpachecker.core.algorithm.bmc.AssertCandidate;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.algorithm.bmc.InductionResult;
import org.sosy_lab.cpachecker.core.algorithm.bmc.InvariantStrengthening;
import org.sosy_lab.cpachecker.core.algorithm.bmc.InvariantStrengthening.NextCti;
import org.sosy_lab.cpachecker.core.algorithm.bmc.InvariantStrengthenings;
import org.sosy_lab.cpachecker.core.algorithm.bmc.Lifting;
import org.sosy_lab.cpachecker.core.algorithm.bmc.PredicateAbstractionStrategy;
import org.sosy_lab.cpachecker.core.algorithm.bmc.ProverEnvironmentWithFallback;
import org.sosy_lab.cpachecker.core.algorithm.bmc.StandardLiftings;
import org.sosy_lab.cpachecker.core.algorithm.bmc.StaticCandidateProvider;
import org.sosy_lab.cpachecker.core.algorithm.bmc.UnrolledReachedSet;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant.BlockedCounterexampleToInductivity;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.pdr.PartialTransitionRelation.CtiWithInputs;
import org.sosy_lab.cpachecker.core.algorithm.invariants.AbstractInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.predicates.AssignmentToPathAllocator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.invariants.FormulaInvariantsSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class PdrAlgorithm implements Algorithm {

  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final PredicateAbstractionManager pam;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  private final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;
  private final Specification specification;
  private final Configuration config;
  private final PdrStatistics stats;
  private final BasicPdrOptions basicPdrOptions;
  private final AbstractionStrategy abstractionStrategy;

  private final InvariantGenerator invariantGenerator;

  private final TargetLocationProvider targetLocationProvider;

  private final ShutdownNotifier shutdownNotifier;

  private final AssignmentToPathAllocator assignmentToPathAllocator;

  private final Set<CandidateInvariant> confirmedCandidates = new CopyOnWriteArraySet<>();

  private boolean invariantGenerationRunning = true;

  private static class PdrStatistics implements Statistics {

    private final Timer satCheck = new Timer();
    private final Timer errorPathCreation = new Timer();

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      if (satCheck.getNumberOfIntervals() > 0) {
        pOut.println("Time for final sat check:            " + satCheck);
      }
      if (errorPathCreation.getNumberOfIntervals() > 0) {
        pOut.println("Time for error path creation:        " + errorPathCreation);
      }
    }

    @Override
    public @Nullable String getName() {
      return "PDR algorithm";
    }
  }

  public PdrAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA,
      Specification pSpecification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException {

    algorithm = pAlgorithm;
    cpa = pCPA;
    logger = pLogger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCFA;
    specification = checkNotNull(pSpecification);
    config = pConfig;
    stats = new PdrStatistics();
    basicPdrOptions = new BasicPdrOptions(config);
    abstractionStrategy =
        basicPdrOptions.abstractionStrategyFactory.createAbstractionStrategy(
            cfa.getVarClassification());

    shutdownNotifier = pShutdownNotifier;
    targetLocationProvider = new CachingTargetLocationProvider(shutdownNotifier, logger, cfa);

    @SuppressWarnings("resource")
    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, BMCAlgorithm.class);
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
    pam = predCpa.getPredicateManager();

    assignmentToPathAllocator =
        new AssignmentToPathAllocator(config, shutdownNotifier, pLogger, cfa.getMachineModel());
    invariantGenerator =
        new AbstractInvariantGenerator() {

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

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    CFANode initialLocation = AbstractStates.extractLocation(pReachedSet.getFirstState());

    TotalTransitionRelation transitionRelation =
        new TotalTransitionRelation(
            fmgr,
            initialLocation,
            getNonTrivialLoopHeads().iterator(),
            location ->
                location == initialLocation
                    ? createPartialTransitionRelation(location, pReachedSet)
                    : createPartialTransitionRelation(location));
    for (BooleanFormula locationFormula : transitionRelation.getPredecessorLocationFormulas()) {
      abstractionStrategy.refinePrecision(pam, Collections.singleton(locationFormula));
    }

    try {
      return runPdr(transitionRelation);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure: " + e.getMessage(), e);
    }
  }

  private AlgorithmStatus runPdr(TotalTransitionRelation pTransitionRelation)
      throws InterruptedException, CPAException, SolverException {
    Objects.requireNonNull(pTransitionRelation);

    UnrolledReachedSet bmcReachedSet = pTransitionRelation.getInitiationRelation().getReachedSet();
    ReachedSet rawBmcReachedSet = bmcReachedSet.getReachedSet();

    // The set of candidate invariants that still need to be checked.
    // Successfully proven invariants are removed from the set.
    final CandidateGenerator candidateGenerator = getCandidateInvariants();
    if (!candidateGenerator.produceMoreCandidates()) {
      rawBmcReachedSet.clearWaitlist();
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    // We need to perform an initial bounded model check every time a new root candidate invariant
    // is introduced (for that candidate).
    AlgorithmStatus status = pTransitionRelation.ensureK();
    if (!checkAbstractionFree(rawBmcReachedSet)) {
      return status;
    }

    boolean producedNewRootInvariants = true;

    try (FrameSet frameSet =
        new FrameSet(
            solver, EnumSet.of(ProverOptions.GENERATE_MODELS, ProverOptions.GENERATE_UNSAT_CORE))) {
      learnClause(frameSet, 0, pTransitionRelation.getInitiationAssertion());

      while (producedNewRootInvariants) {
        Optional<AlgorithmStatus> initialPushResult =
            initialPush(candidateGenerator, bmcReachedSet, frameSet);
        if (initialPushResult.isPresent()) {
          return initialPushResult.orElseThrow();
        }

        while (candidateGenerator.hasCandidatesAvailable()) {

          boolean propagated = false;

          for (int i = 0; i < frameSet.getFrontierIndex(); ++i) {
            @SuppressWarnings("resource")
            ProverEnvironmentWithFallback frameProver = frameSet.getFrameProver(i);
            Set<CandidateInvariant> frameInvariants = frameSet.getInvariants(i);
            frameInvariants =
                Sets.union(
                    frameInvariants,
                    Collections.singleton(getCurrentInvariant(pTransitionRelation)));
            List<CandidateInvariant> toPush = new ArrayList<>();
            for (CandidateInvariant frameClause : frameSet.getPushableFrameClauses(i)) {
              InductionResult<CandidateInvariant> pushAttempt =
                  checkInduction(
                      frameProver,
                      frameInvariants,
                      pTransitionRelation,
                      frameClause,
                      InvariantStrengthenings.noStrengthening(),
                      StandardLiftings.NO_LIFTING);
              if (pushAttempt.isSuccessful()) {
                toPush.add(frameClause);
                propagated = true;
              }
            }

            for (CandidateInvariant pushableClause : toPush) {
              frameSet.pushFrameClause(i, pushableClause);
            }
          }

          if (propagated) {
            // We have just propagated some non-root candidate invariants,
            // which in turn may have confirmed some root candidate invariants
            Optional<AlgorithmStatus> candidateConfirmationResult =
                handleConfirmedCandidates(candidateGenerator, frameSet, rawBmcReachedSet);
            if (candidateConfirmationResult.isPresent()) {
              return candidateConfirmationResult.orElseThrow();
            }
          }

          // Increase frontier for all root invariants
          Optional<AlgorithmStatus> strengthenResult =
              strengthen(candidateGenerator, frameSet, pTransitionRelation);
          if (strengthenResult.isPresent()) {
            return strengthenResult.orElseThrow();
          }
        }

        producedNewRootInvariants = candidateGenerator.produceMoreCandidates();
      }
    }

    return status;
  }

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
          conditionCPAs.stream()
              .map(Object::getClass)
              .map(Class::getSimpleName)
              .collect(Collectors.joining(", ")));
      return false;
    }
    return !Iterables.isEmpty(conditionCPAs);
  }

  private boolean continueAfterFailedConditionAdjustment() {
    return true;
  }

  private Optional<AlgorithmStatus> strengthen(
      CandidateGenerator pCandidateGenerator,
      FrameSet pFrameSet,
      TotalTransitionRelation pTransitionRelation)
      throws InterruptedException, SolverException, CPAException {

    Optional<AlgorithmStatus> strengthenResult =
        blockAllKStepCounterexamples(pCandidateGenerator, pFrameSet, pTransitionRelation);
    if (strengthenResult.isPresent()) {
      return strengthenResult;
    }

    // We have just pushed the frontier, so maybe some root invariants were confirmed
    Optional<AlgorithmStatus> candidateConfirmationResult =
        handleConfirmedCandidates(
            pCandidateGenerator,
            pFrameSet,
            pTransitionRelation.getInitiationRelation().getReachedSet().getReachedSet());
    if (candidateConfirmationResult.isPresent()) {
      return candidateConfirmationResult;
    }

    if (basicPdrOptions.getConditionAdjustmentCriterion().shouldAdjustConditions()) {
      // Increase k
      if (!adjustConditions() && !continueAfterFailedConditionAdjustment()) {
        return Optional.of(AlgorithmStatus.UNSOUND_AND_PRECISE);
      }
    }

    return Optional.empty();
  }

  private Optional<AlgorithmStatus> blockAllKStepCounterexamples(
      CandidateGenerator pCandidateGenerator,
      FrameSet pFrameSet,
      TotalTransitionRelation pTransitionRelation)
      throws InterruptedException, SolverException, CPAException {

    for (CandidateInvariant rootCandidateInvariant : pCandidateGenerator) {

      FrontierExtensionResult frontierExtensionResult = null;
      while (frontierExtensionResult == null || !frontierExtensionResult.isSuccessful()) {

        frontierExtensionResult =
            extendFrontier(rootCandidateInvariant, pFrameSet, pTransitionRelation);

        if (frontierExtensionResult.getEarlyReturn().isPresent()) {
          return frontierExtensionResult.getEarlyReturn();
        }

        if (!frontierExtensionResult.isSuccessful()) {
          Optional<AlgorithmStatus> blockResult =
              blockProofObligation(
                  pFrameSet,
                  pTransitionRelation,
                  frontierExtensionResult.getProofObligation(),
                  pCandidateGenerator);
          if (blockResult.isPresent()) {
            return blockResult;
          }
        }
      }
    }

    return Optional.empty();
  }

  @SuppressWarnings("resource")
  private Optional<AlgorithmStatus> blockProofObligation(
      FrameSet pFrameSet,
      TotalTransitionRelation pTransitionRelation,
      ProofObligation pObligation,
      CandidateGenerator pCandidateGenerator)
      throws SolverException, InterruptedException, CPAException {

    int frontierIndex = pFrameSet.getFrontierIndex();
    Set<ProofObligation> forceEagerLiftingRefinement = new HashSet<>();
    Queue<ProofObligation> proofObligations = new PriorityQueue<>();
    proofObligations.add(pObligation);

    boolean checkCounterexample = false;

    while (!proofObligations.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();

      ProofObligation obligation = proofObligations.poll();

      SymbolicCandiateInvariant abstractBlockingClause = obligation.getBlockedAbstractCti();
      int frameIndex = obligation.getFrameIndex();
      Set<CandidateInvariant> invariants = pFrameSet.getInvariants(frameIndex);
      invariants =
          Sets.union(invariants, Collections.singleton(getCurrentInvariant(pTransitionRelation)));
      ProverEnvironmentWithFallback prover = pFrameSet.getFrameProver(frameIndex);

      boolean eagerLiftingRefinement =
          obligation.getLiftingAbstractionFailureCount()
                  >= basicPdrOptions.getLiftingAbstractionFailureThreshold()
              || frameIndex == 0
              || forceEagerLiftingRefinement.contains(obligation);

      // Check blocked abstract CTI first
      DetectingLiftingAbstractionFailureStrategy lafsForAbstractCheck =
          new DetectingLiftingAbstractionFailureStrategy(eagerLiftingRefinement);
      Lifting liftingForAbstractCheck =
          basicPdrOptions
              .getLiftingStrategy()
              .createLifting(abstractionStrategy, lafsForAbstractCheck);
      InductionResult<SymbolicCandiateInvariant> abstractResult =
          checkInduction(
              prover,
              invariants,
              pTransitionRelation,
              abstractBlockingClause,
              basicPdrOptions
                  .getInvariantRefinementStrategy()
                  .createRefinementStrategy(abstractionStrategy),
              liftingForAbstractCheck);

      if (abstractResult.isSuccessful()) {
        // If the abstract CTI was successfully blocked, we learn its negation as a clause
        learnClause(pFrameSet, frameIndex + 1, abstractResult.getInvariantRefinement());
        logger.log(
            Level.FINEST,
            "Learned clause "
                + abstractBlockingClause
                + " at frame index "
                + (frameIndex + 1)
                + " to block "
                + obligation);
        if (frameIndex + 1 < frontierIndex) {
          proofObligations.add(obligation.incrementFrameIndex());
        }
      } else if (obligation.getBlockedConcreteCti().isPresent()) {
        // If the abstract version failed and a concrete CTI exists,
        // we know that we are in a situation caused by a lazily handled
        // lifting-abstraction failure (LAF).

        // Check concrete CTI
        InvariantStrengthening<SymbolicCandiateInvariant, SymbolicCandiateInvariant>
            invariantAbstraction =
                mustRefineOnConsecutionAbstractionFailure(obligation)
                    ? basicPdrOptions
                        .getInvariantRefinementStrategy()
                        .createRefinementStrategy(abstractionStrategy)
                    : InvariantStrengthenings.noStrengthening();
        DetectingLiftingAbstractionFailureStrategy lafsForConcreteCheck =
            new DetectingLiftingAbstractionFailureStrategy(eagerLiftingRefinement);
        Lifting liftingForConcreteCheck =
            basicPdrOptions
                .getLiftingStrategy()
                .createLifting(abstractionStrategy, lafsForConcreteCheck);
        InductionResult<SymbolicCandiateInvariant> concreteResult =
            checkInduction(
                prover,
                invariants,
                pTransitionRelation,
                obligation.getBlockedConcreteCti().orElseThrow(),
                invariantAbstraction,
                liftingForConcreteCheck);
        if (concreteResult.isSuccessful()) {
          // If the concrete check is successful, the abstraction was spurious
          // and we have a consecution abstraction failure. (CAF)
          assert implies(prover, invariants, obligation.getBlockedConcreteCti().orElseThrow());
          if (mustRefineOnConsecutionAbstractionFailure(obligation)) {
            // CAF-case 1: If we exceeded the threshold for spurious transitions
            // or are in frame zero, we perform consecution refinement:

            // TODO better consecution refinement using interpolation
            SymbolicCandiateInvariant refined = concreteResult.getInvariantRefinement();
            assert implies(prover, invariants, refined);
            logger.log(
                Level.FINEST,
                "Learned clause "
                    + refined
                    + " at frame index "
                    + (frameIndex + 1)
                    + " to block "
                    + pObligation);
            learnClause(pFrameSet, frameIndex + 1, refined);
          } else if (frameIndex > 0) {
            // CAF-case 2: If we are still below the threshold,
            // we continue with the (attempted) liftings of the CTIs to the abstract check:
            extractNewProofObligations(obligation, lafsForAbstractCheck, abstractResult)
                .map(ProofObligation::addSpuriousTransition)
                .forEach(proofObligations::offer);
          } else {
            throw new AssertionError(
                "Negative frame index? Frame zero should be handled in the first CAF case, "
                    + "frames with positive index in the second CAF case. Actual index: "
                    + frameIndex);
          }
        } else if (frameIndex > 0) {
          // If the concrete check failed we extract new proof obligations
          // from the CTIs to the concrete check.
          extractNewProofObligations(obligation, lafsForConcreteCheck, concreteResult)
              .map(ProofObligation::addSpuriousTransition)
              .forEach(proofObligations::offer);
        } else {
          // The concrete check failed, we are in the initial frame,
          // and there was at least one lifting abstraction failure somewhere on the trace
          // (the current one). There may, however, also be another one.
          Optional<ProofObligation> nextLAF =
              obligation.find(o -> o != obligation && o.getBlockedConcreteCti().isPresent());
          if (nextLAF.isPresent()) {
            // If there is another spurious transition on the trace, we need to refine it.
            ProofObligation next = nextLAF.orElseThrow();
            next = next.addSpuriousTransition();
            forceEagerLiftingRefinement.add(next);
            proofObligations.offer(next);
          } else {
            // If the current obligation is the only spurious one,
            // the concrete CTI represents a real counterexample.
            checkCounterexample = true;
          }
        }
      } else if (frameIndex > 0) {
        // If the abstract version failed and the obligation had no concrete CTI,
        // we know the abstract formula is a true (i.e., non-spurious) underapproximation
        // of bad states,
        // so we extract a proof obligation from the CTIs to the abstract check:
        extractNewProofObligations(obligation, lafsForAbstractCheck, abstractResult)
            .forEach(proofObligations::offer);
      } else {
        // The abstract check failed, the proof obligation is non-spurious,
        // and we are in the initial frame.
        // This means we found a real counterexample to the candidate invariant.
        checkCounterexample = true;
      }

      if (checkCounterexample) {
        boolean safe =
            boundedModelCheck(
                obligation,
                pTransitionRelation.getInitiationRelation().getReachedSet(),
                pFrameSet.getFrameProver(0));
        if (!safe) {
          CandidateInvariant violatedCandidate = obligation.getViolatedInvariant();
          Iterables.removeIf(pCandidateGenerator, violatedCandidate::equals);
          if (violatedCandidate == TargetLocationCandidateInvariant.INSTANCE) {
            return Optional.of(AlgorithmStatus.UNSOUND_AND_PRECISE);
          }
        }
      }
    }
    return Optional.empty();
  }

  private boolean implies(
      ProverEnvironmentWithFallback pProver,
      Set<CandidateInvariant> pInvariants,
      SymbolicCandiateInvariant pBlockedAbstractCti)
      throws SolverException, InterruptedException {
    try {
      BooleanFormula frameAssertion = bfmgr.makeTrue();
      for (SymbolicCandiateInvariant frameClause :
          Iterables.filter(pInvariants, SymbolicCandiateInvariant.class)) {
        frameAssertion = bfmgr.and(frameAssertion, frameClause.getPlainFormula(fmgr));
      }
      BooleanFormula violation = bfmgr.not(pBlockedAbstractCti.getPlainFormula(fmgr));
      pProver.push(frameAssertion);
      pProver.push(violation);
      boolean sound = pProver.isUnsat();
      return sound;
    } finally {
      pProver.pop();
      pProver.pop();
    }
  }

  private static Stream<ProofObligation> extractNewProofObligations(
      ProofObligation pOldObligation,
      DetectingLiftingAbstractionFailureStrategy pLafsForCheck,
      InductionResult<? extends CandidateInvariant> pCheckResult) {
    return pCheckResult.getBadStateBlockingClauses().stream()
        .map(
            badStateBlockingClause ->
                pOldObligation.causeProofObligation(
                    badStateBlockingClause,
                    pLafsForCheck.getBlockedConcreteCtiForSpuriousAbstraction(
                        badStateBlockingClause),
                    pOldObligation.getNSpuriousTransitions(),
                    pCheckResult.getK()));
  }

  private boolean mustRefineOnConsecutionAbstractionFailure(ProofObligation obligation) {
    return obligation.getNSpuriousTransitions()
            >= basicPdrOptions.getSpuriousTransitionCountThreshold()
        || obligation.getFrameIndex() == 0;
  }

  private void learnClause(FrameSet pFrameSet, int pFrameIndex, SymbolicCandiateInvariant pClause) {
    pFrameSet.addFrameClause(pFrameIndex, pClause);
  }

  private InvariantSupplier getCurrentInvariantSupplier() throws InterruptedException {
    if (invariantGenerationRunning) {
      try {
        return invariantGenerator.getSupplier();
      } catch (CPAException e) {
        logger.logUserException(Level.FINE, e, "Invariant generation failed.");
        invariantGenerationRunning = false;
      } catch (InterruptedException e) {
        shutdownNotifier.shutdownIfNecessary();
        logger.log(Level.FINE, "Invariant generation was cancelled.");
        logger.logDebugException(e);
        invariantGenerationRunning = false;
      }
    }
    return InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  private CandidateInvariant getCurrentInvariant(TotalTransitionRelation pTotalTransitionRelation)
      throws InterruptedException {
    BooleanFormula invariant = bfmgr.makeTrue();
    for (CFANode location : pTotalTransitionRelation.getPredecessorLocations()) {
      BooleanFormula locationInvariant =
          getCurrentInvariantSupplier()
              .getInvariantFor(location, Optional.empty(), fmgr, pmgr, null);
      if (!bfmgr.isTrue(locationInvariant)) {
        invariant =
            bfmgr.and(
                invariant,
                fmgr.uninstantiate(
                    bfmgr.implication(
                        TotalTransitionRelation.getUnprimedLocationFormula(fmgr, location),
                        locationInvariant)));
      }
    }
    return SymbolicCandiateInvariant.makeSymbolicInvariant(
        pTotalTransitionRelation.getPredecessorLocations(),
        pTotalTransitionRelation.getCandidateInvariantStatePredicate(),
        invariant,
        fmgr);
  }

  @SuppressWarnings("resource")
  private FrontierExtensionResult extendFrontier(
      CandidateInvariant pRootCandidateInvariant,
      FrameSet pFrameSet,
      TotalTransitionRelation pTransitionRelation)
      throws InterruptedException, CPATransferException, SolverException {

    int oldFrontierIndex = pFrameSet.getFrontierIndex(pRootCandidateInvariant);
    Set<CandidateInvariant> predecessorAssertions = pFrameSet.getInvariants(oldFrontierIndex);
    predecessorAssertions =
        Sets.union(
            predecessorAssertions, Collections.singleton(getCurrentInvariant(pTransitionRelation)));
    ProverEnvironmentWithFallback prover = pFrameSet.getFrameProver(oldFrontierIndex);

    boolean eagerLiftingRefinement =
        oldFrontierIndex == 0 || basicPdrOptions.liftingAbstractionFailureThreshold <= 0;
    DetectingLiftingAbstractionFailureStrategy lafs =
        new DetectingLiftingAbstractionFailureStrategy(eagerLiftingRefinement);

    InductionResult<CandidateInvariant> inductionResult =
        checkInduction(
            prover,
            predecessorAssertions,
            pTransitionRelation,
            pRootCandidateInvariant,
            InvariantStrengthenings.noStrengthening(),
            basicPdrOptions.getLiftingStrategy().createLifting(abstractionStrategy, lafs));

    if (inductionResult.isSuccessful()) {
      pFrameSet.pushFrontier(oldFrontierIndex + 1, pRootCandidateInvariant);
      return FrontierExtensionResult.getSuccess();
    }

    Set<SymbolicCandiateInvariant> model = inductionResult.getBadStateBlockingClauses();
    assert model.size() == 1;
    SymbolicCandiateInvariant blockedAbstractCti = model.iterator().next();

    Optional<SymbolicCandiateInvariant> blockedConcreteCti =
        lafs.getBlockedConcreteCtiForSpuriousAbstraction(blockedAbstractCti);
    assert !(eagerLiftingRefinement && blockedConcreteCti.isPresent());

    return FrontierExtensionResult.getFailure(
        ProofObligation.createObligation(
            blockedAbstractCti,
            blockedConcreteCti,
            oldFrontierIndex - 1,
            0,
            pTransitionRelation.getInitiationRelation().getDesiredK(),
            pRootCandidateInvariant));
  }

  private <S extends CandidateInvariant, T extends CandidateInvariant>
      InductionResult<T> checkInduction(
          ProverEnvironmentWithFallback pProver,
          Set<CandidateInvariant> pPredecessorAssertions,
          TotalTransitionRelation pTransitionRelation,
          S pCandidateInvariant,
          InvariantStrengthening<S, T> pInvariantRefinement,
          Lifting pLifting)
          throws SolverException, InterruptedException, CPATransferException {

    assert pProver.isEmpty();

    AssertCandidate assertSinglePredecessor =
        p ->
            BMCHelper.assertAt(
                Collections.singleton(
                    pTransitionRelation
                        .getInitiationRelation()
                        .getReachedSet()
                        .getReachedSet()
                        .getFirstState()),
                p,
                fmgr,
                pmgr,
                true);

    // TODO this can probably done more efficiently
    // by keeping the assertions and transitions on the stack;
    // we just need to make sure then to recreate that stack when the transition changes
    Object transitionAssertionId = pProver.push(pTransitionRelation.getTransitionFormula());
    Object predecessorAssertionId =
        pProver.push(pTransitionRelation.getPredecessorAssertions(pPredecessorAssertions));
    Object candidateAssertionId =
        pProver.push(pTransitionRelation.getPredecessorAssertion(pCandidateInvariant));

    Multimap<BooleanFormula, BooleanFormula> successorViolationAssertions =
        pTransitionRelation.getSuccessorViolationAssertions(pCandidateInvariant);
    BooleanFormula successorViolation =
        BMCHelper.disjoinStateViolationAssertions(bfmgr, successorViolationAssertions);
    pProver.push(successorViolation);

    boolean success = pProver.isUnsat();
    if (success) {
      AssertCandidate assertKPredecessors = p -> pTransitionRelation.getPredecessorAssertion(p);
      AssertCandidate assertSuccessorViolation =
          candidate -> {
            Multimap<BooleanFormula, BooleanFormula> succViolationAssertions =
                pTransitionRelation.getSuccessorViolationAssertions(pCandidateInvariant);
            BooleanFormula succViolation =
                BMCHelper.disjoinStateViolationAssertions(bfmgr, succViolationAssertions);
            return succViolation;
          };
      NextCti nextCti =
          () -> {
            List<ValueAssignment> modelAssignments = pProver.getModelAssignments();
            PartialTransitionRelation violatedPartialTransition =
                pTransitionRelation.getViolatedPartialTransition(modelAssignments);
            CtiWithInputs ctiWithInputs =
                violatedPartialTransition.getCtiWithInputs(modelAssignments);
            return Optional.of(ctiWithInputs.getCti());
          };

      T refinedInvariant =
          pInvariantRefinement.strengthenInvariant(
              pProver,
              fmgr,
              pam,
              pCandidateInvariant,
              assertKPredecessors,
              assertSuccessorViolation,
              assertSinglePredecessor,
              successorViolationAssertions,
              Optional.empty(),
              nextCti);

      pProver.pop(); // Pop the successor violation
      pProver.pop(); // Pop the predecessor assertions
      pProver.pop(); // Pop the candidate predecessor assertion
      pProver.pop(); // Pop the transition

      assert pProver.isEmpty();

      return InductionResult.getSuccessful(refinedInvariant);
    }

    List<ValueAssignment> modelAssignments = pProver.getModelAssignments();
    PartialTransitionRelation violatedPartialTransition =
        pTransitionRelation.getViolatedPartialTransition(modelAssignments);
    CtiWithInputs ctiWithInputs = violatedPartialTransition.getCtiWithInputs(modelAssignments);
    BlockedCounterexampleToInductivity blockedConcreteCti =
        SymbolicCandiateInvariant.blockCti(
            pTransitionRelation.getPredecessorLocations(),
            pTransitionRelation.getCandidateInvariantStatePredicate(),
            ctiWithInputs.getCti(),
            fmgr);
    SymbolicCandiateInvariant blockedAbstractCti = blockedConcreteCti;
    BooleanFormula inputs = ctiWithInputs.getInputs();

    pProver.pop(); // Pop the successor violation

    // Lifting
    if (pLifting.canLift()) {
      assert !pProver.isUnsat();

      // Push the candidate successor assertion
      Object successorAssertionId =
          pProver.push(pTransitionRelation.getSuccessorAssertion(pCandidateInvariant));

      Object inputAssertionId = pProver.push(inputs); // Push the input assignments

      // Lift
      blockedAbstractCti =
          pLifting.lift(
              fmgr,
              pam,
              pProver,
              blockedConcreteCti,
              assertSinglePredecessor,
              Arrays.asList(
                  transitionAssertionId,
                  predecessorAssertionId,
                  candidateAssertionId,
                  successorAssertionId,
                  inputAssertionId));
      pProver.pop(); // Pop input assignments
      pProver.pop(); // Pop the candidate successor assertion
    }

    pProver.pop(); // Pop the candidate predecessor assertion
    pProver.pop(); // Pop the predecessor assertions
    pProver.pop(); // Pop the transition

    assert pProver.isEmpty();

    return InductionResult.getFailed(
        Collections.singleton(blockedAbstractCti), violatedPartialTransition.getDesiredK());
  }

  private Optional<AlgorithmStatus> handleConfirmedCandidates(
      CandidateGenerator pCandidateGenerator, FrameSet pFrameSet, ReachedSet pReachedSet) {
    Iterator<CandidateInvariant> rootInvariantIterator = pCandidateGenerator.iterator();
    while (rootInvariantIterator.hasNext()) {
      CandidateInvariant rootInvariant = rootInvariantIterator.next();
      if (pFrameSet.isConfirmed(rootInvariant)) {
        rootInvariantIterator.remove();
        confirmedCandidates.add(rootInvariant);
        if (rootInvariant == TargetLocationCandidateInvariant.INSTANCE) {
          rootInvariant.assumeTruth(pReachedSet);
          return Optional.of(AlgorithmStatus.SOUND_AND_PRECISE);
        }
      }
    }
    return Optional.empty();
  }

  private Optional<AlgorithmStatus> initialPush(
      CandidateGenerator pCandidateGenerator, UnrolledReachedSet pBmcReachedSet, FrameSet pFrameSet)
      throws CPATransferException, InterruptedException, SolverException {
    Iterator<CandidateInvariant> rootInvariantIterator = pCandidateGenerator.iterator();
    while (rootInvariantIterator.hasNext()) {
      CandidateInvariant rootInvariant = rootInvariantIterator.next();
      boolean bmcSafe =
          boundedModelCheck(pBmcReachedSet, pFrameSet.getFrameProver(0), rootInvariant);
      if (bmcSafe) {
        pFrameSet.pushFrontier(1, rootInvariant);
      } else {
        rootInvariantIterator.remove();
        if (rootInvariant == TargetLocationCandidateInvariant.INSTANCE) {
          return Optional.of(AlgorithmStatus.SOUND_AND_PRECISE);
        }
      }
    }
    return Optional.empty();
  }

  private PartialTransitionRelation createPartialTransitionRelation(CFANode predecessorLocation) {
    return createPartialTransitionRelation(predecessorLocation, reachedSetFactory.create(cpa));
  }

  private PartialTransitionRelation createPartialTransitionRelation(
      CFANode predecessorLocation, ReachedSet pReachedSet) {
    PartialTransitionRelation partialTransitionRelation =
        new PartialTransitionRelation(
            predecessorLocation, algorithm, cpa, fmgr, pmgr, logger, pReachedSet, getLoopHeads());
    return partialTransitionRelation;
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
  private Set<CFANode> getLoopHeads() {
    return BMCHelper.getLoopHeads(cfa, targetLocationProvider);
  }

  private Stream<CFANode> getNonTrivialLoopHeads() {
    Set<CFANode> loopHeads = getLoopHeads();
    if (!cfa.getLoopStructure().isPresent()) {
      return loopHeads.stream();
    }
    return cfa.getLoopStructure().orElseThrow().getAllLoops().stream()
        .filter(loop -> !isTrivialSelfLoop(loop))
        .map(Loop::getLoopHeads)
        .flatMap(Collection::stream)
        .filter(getLoopHeads()::contains)
        .distinct();
  }

  private boolean checkAbstractionFree(ReachedSet pBmcReachedSet) {
    Optional<AbstractState> abstractionState =
        from(pBmcReachedSet).stream()
            .skip(1) // first state of reached is always an abstraction state, so skip it
            .filter(
                Predicates.not(
                    AbstractStates::isTargetState)) // target states may be abstraction states
            .filter(PredicateAbstractState::containsAbstractionState)
            .findAny();
    if (abstractionState.isPresent()) {
      logger.log(
          Level.WARNING,
          "PDR algorithm and its derivatives do not work with PredicateCPA abstractions. Could not"
              + " check for satisfiability.");
      return false;
    }
    return true;
  }

  private boolean boundedModelCheck(
      ProofObligation pObligation,
      UnrolledReachedSet pBmcReachedSet,
      ProverEnvironmentWithFallback pCexProver)
      throws InterruptedException, CPAException, SolverException {
    checkArgument(
        pObligation.getFrameIndex() == 0,
        "Bounded model check should only be called for obligations at frame index zero.");

    CandidateInvariant violatedCandidate = pObligation.getViolatedInvariant();
    ReachedSet reached = pBmcReachedSet.getReachedSet();
    int previousK = pBmcReachedSet.getDesiredK();
    int k = 0;
    BooleanFormula program = bfmgr.makeTrue();
    for (ProofObligation currentObligation : pObligation) {
      k += currentObligation.getLength();
      pBmcReachedSet.setDesiredK(k + 1);
      pBmcReachedSet.ensureK();
      /* FIXME
      Iterable<AbstractState> assertionStates =
          pObligation.getBlockedAbstractCti().filterApplicable(reached);
      assertionStates = BMCHelper.filterIteration(assertionStates, k, getLoopHeads());
      SymbolicCandiateInvariant ctiBlock = currentObligation.getBlockedAbstractCti();
      if (currentObligation.getBlockedConcreteCti().isPresent()) {
        ctiBlock = currentObligation.getBlockedConcreteCti().get();
      }
      BooleanFormula ctiBlockFormula = ctiBlock.getPlainFormula(fmgr);
      BooleanFormula ctiFormula = bfmgr.not(ctiBlockFormula);
      ctiFormula =
          fmgr.filterLiterals(
              ctiFormula,
              f ->
                  !fmgr.extractVariableNames(f)
                      .contains(TotalTransitionRelation.getLocationVariableName()));
      SymbolicCandiateInvariant cti =
          SymbolicCandiateInvariant.makeSymbolicInvariant(
              ctiBlock.getApplicableLocations(), ctiBlock.getStateFilter(), ctiFormula, fmgr);
      BooleanFormula ctiAssertion = BMCHelper.assertAt(reached, cti, fmgr, pmgr, true);
      program = bfmgr.and(program, ctiAssertion);
      */
    }
    pBmcReachedSet.setDesiredK(previousK);

    Iterable<AbstractState> violationStates = violatedCandidate.filterApplicable(reached);
    violationStates = BMCHelper.filterIterationsUpTo(violationStates, k, getLoopHeads());
    BooleanFormula candidateAssertion = violatedCandidate.getAssertion(violationStates, fmgr, pmgr);
    BooleanFormula violationAssertion = bfmgr.not(candidateAssertion);
    program = bfmgr.and(program, violationAssertion);
    pCexProver.push(program);

    logger.log(Level.INFO, "Starting satisfiability check...");
    stats.satCheck.start();
    boolean safe = pCexProver.isUnsat();
    stats.satCheck.stop();

    if (!safe && violatedCandidate == TargetLocationCandidateInvariant.INSTANCE) {
      analyzeCounterexample(program, reached, pCexProver);
    } else if (safe && pBmcReachedSet.getCurrentMaxK() <= k) {
      violatedCandidate.assumeTruth(reached);
    }

    // Pop off everything
    pCexProver.pop();

    return safe;
  }

  private boolean boundedModelCheck(
      UnrolledReachedSet pReachedSet,
      ProverEnvironmentWithFallback pProver,
      CandidateInvariant pInductionProblem)
      throws CPATransferException, InterruptedException, SolverException {
    ReachedSet reached = pReachedSet.getReachedSet();
    BooleanFormula program = bfmgr.not(pInductionProblem.getAssertion(reached, fmgr, pmgr));
    logger.log(Level.INFO, "Starting satisfiability check...");
    stats.satCheck.start();
    pProver.push(program);
    boolean safe = pProver.isUnsat();
    // Leave program formula on solver stack until error path is created
    stats.satCheck.stop();

    if (safe) {
      pInductionProblem.assumeTruth(reached);
      // TODO check forward condition
    } else if (pInductionProblem == TargetLocationCandidateInvariant.INSTANCE) {
      analyzeCounterexample(program, reached, pProver);
    }

    // Now pop the program formula off of the stack
    pProver.pop();

    return safe;
  }

  /**
   * This method tries to find a feasible path to (one of) the target state(s). It does so by asking
   * the solver for a satisfying assignment.
   */
  @SuppressWarnings("resource")
  private void analyzeCounterexample(
      final BooleanFormula pCounterexampleFormula,
      final ReachedSet pReachedSet,
      final ProverEnvironmentWithFallback pProver)
      throws CPATransferException, InterruptedException {
    if (!(cpa instanceof ARGCPA)) {
      logger.log(Level.INFO, "Error found, but error path cannot be created without ARGCPA");
      return;
    }

    stats.errorPathCreation.start();
    Solver cexAnalysisSolver = solver;
    PathFormulaManager cexAnalysisPmgr = pmgr;
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

      // get (precise) error path
      ARGPath targetPath;
      try (Model model = pProver.getModel()) {
        ARGState root = (ARGState) pReachedSet.getFirstState();
        Set<AbstractState> arg = pReachedSet.asCollection();

        try {
          targetPath = pmgr.getARGPathFromModel(model, root, arg);
        } catch (IllegalArgumentException e) {
          logger.logUserException(Level.WARNING, e, "Could not create error path");
          return;
        }

      } catch (SolverException e) {
        logger.log(Level.WARNING, "Solver could not produce model, cannot create error path.");
        logger.logDebugException(e);
        return;
      }

      BooleanFormula cexFormula = pCounterexampleFormula;

      // replay error path for a more precise satisfying assignment
      PathChecker pathChecker;
      try {

        if (cexAnalysisSolver.getVersion().toLowerCase().contains("smtinterpol")) {
          // SMTInterpol does not support reusing the same solver
          cexAnalysisSolver = Solver.create(config, logger, shutdownNotifier);
          FormulaManagerView formulaManager = cexAnalysisSolver.getFormulaManager();
          cexAnalysisPmgr =
              new PathFormulaManagerImpl(
                  formulaManager, config, logger, shutdownNotifier, cfa, AnalysisDirection.FORWARD);
          // cannot dump pCounterexampleFormula, PathChecker would use wrong FormulaManager for it
          cexFormula = cexAnalysisSolver.getFormulaManager().getBooleanFormulaManager().makeTrue();
        }

        pathChecker =
            new PathChecker(
                config, logger, cexAnalysisPmgr, cexAnalysisSolver, assignmentToPathAllocator);

      } catch (InvalidConfigurationException e) {
        // Configuration has somehow changed and can no longer be used to create the solver and path
        // formula manager
        logger.logUserException(
            Level.WARNING, e, "Could not replay error path to get a more precise model");
        return;
      }

      CounterexampleTraceInfo cexInfo =
          CounterexampleTraceInfo.feasible(
              ImmutableList.of(cexFormula), ImmutableList.of(), targetPath);
      CounterexampleInfo counterexample = pathChecker.createCounterexample(targetPath, cexInfo);
      counterexample.getTargetState().addCounterexampleInformation(counterexample);

    } finally {
      stats.errorPathCreation.stop();
      if (cexAnalysisSolver != solver) {
        cexAnalysisSolver.close();
      }
    }
  }

  private CandidateGenerator getCandidateInvariants() {
    if (getTargetLocations().isEmpty() || !cfa.getAllLoopHeads().isPresent()) {
      return CandidateGenerator.EMPTY_GENERATOR;
    } else {
      return new StaticCandidateProvider(
          Collections.singleton(TargetLocationCandidateInvariant.INSTANCE));
    }
  }

  @Options(prefix = "pdr")
  protected static class BasicPdrOptions {

    @Option(
        secure = true,
        description =
            "Maximum number of accepted spurious transitions within a proof-obligation trace before"
                + " a consecution abstraction failure triggers a refinement.")
    private int spuriousTransitionCountThreshold = 0;

    @Option(
        secure = true,
        description =
            "Maximum number of ignored lifting abstraction failures within a proof-obligation"
                + " trace.")
    private int liftingAbstractionFailureThreshold = 0;

    @Option(
        secure = true,
        description = "Which strategy to use to abstract counterexamples to inductivity.")
    private LiftingStrategyFactories liftingStrategy = LiftingStrategyFactories.NO_LIFTING;

    @Option(
        secure = true,
        name = "abstractionStrategy",
        description =
            "Which strategy to use to perform abstraction of successful proof results"
                + " or when lifting with the lifting strategy ABSTRACTION_BASED_LIFTING.")
    private AbstractionStrategyFactories abstractionStrategyFactory =
        AbstractionStrategyFactories.NO_ABSTRACTION;

    @Option(
        secure = true,
        name = "invariantRefinementStrategy",
        description =
            "Which strategy to use to perform invariant refinement on successful proof results.")
    private InvariantStrengtheningStrategies invariantRefinementStrategy =
        InvariantStrengtheningStrategies.NO_STRENGTHENING;

    @Option(
        secure = true,
        description = "Whether to adjust conditions (i.e. increment k) after frontier extension.")
    private ConditionAdjustmentCriterion conditionAdjustmentCriterion =
        ConditionAdjustmentCriterion.NEVER;

    private BasicPdrOptions(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public int getSpuriousTransitionCountThreshold() {
      return spuriousTransitionCountThreshold;
    }

    public int getLiftingAbstractionFailureThreshold() {
      return liftingAbstractionFailureThreshold;
    }

    public LiftingStrategyFactories getLiftingStrategy() {
      return liftingStrategy;
    }

    public AbstractionStrategyFactories getAbstractionStrategyFactory() {
      return abstractionStrategyFactory;
    }

    public InvariantStrengtheningStrategies getInvariantRefinementStrategy() {
      return invariantRefinementStrategy;
    }

    public ConditionAdjustmentCriterion getConditionAdjustmentCriterion() {
      return conditionAdjustmentCriterion;
    }
  }

  private enum LiftingStrategyFactories {
    NO_LIFTING {
      @Override
      Lifting createLifting(
          AbstractionStrategy pAbstractionStrategy,
          LiftingAbstractionFailureStrategy pLAFStrategy) {
        return StandardLiftings.NO_LIFTING;
      }

      @Override
      Set<ProverOptions> getRequiredProverOptions() {
        return EnumSet.noneOf(ProverOptions.class);
      }
    },

    UNSAT_CORE_BASED_LIFTING {
      @Override
      Lifting createLifting(
          AbstractionStrategy pAbstractionStrategy,
          LiftingAbstractionFailureStrategy pLAFStrategy) {
        return StandardLiftings.UNSAT_BASED_LIFTING;
      }

      @Override
      Set<ProverOptions> getRequiredProverOptions() {
        return EnumSet.of(ProverOptions.GENERATE_UNSAT_CORE);
      }
    },

    ABSTRACTION_BASED_LIFTING {
      @Override
      Lifting createLifting(
          AbstractionStrategy pAbstractionStrategy,
          LiftingAbstractionFailureStrategy pLAFStrategy) {
        return new AbstractionBasedLifting(pAbstractionStrategy, pLAFStrategy);
      }

      @Override
      Set<ProverOptions> getRequiredProverOptions() {
        return EnumSet.of(ProverOptions.GENERATE_UNSAT_CORE);
      }
    };

    abstract Lifting createLifting(
        AbstractionStrategy pAbstractionStrategy, LiftingAbstractionFailureStrategy pLAFStrategy);

    abstract Set<ProverOptions> getRequiredProverOptions();
  }

  private enum AbstractionStrategyFactories {
    NO_ABSTRACTION {
      @Override
      AbstractionStrategy createAbstractionStrategy(
          Optional<VariableClassification> pVariableClassification) {
        return AbstractionStrategy.NoAbstraction.INSTANCE;
      }

      @Override
      Set<ProverOptions> getRequiredProverOptions() {
        return EnumSet.noneOf(ProverOptions.class);
      }
    },

    ALLSAT_BASED_PREDICATE_ABSTRACTION {
      @Override
      AbstractionStrategy createAbstractionStrategy(
          Optional<VariableClassification> pVariableClassification) {
        return new PredicateAbstractionStrategy(pVariableClassification);
      }

      @Override
      Set<ProverOptions> getRequiredProverOptions() {
        return EnumSet.of(ProverOptions.GENERATE_MODELS, ProverOptions.GENERATE_UNSAT_CORE);
      }
    };

    abstract AbstractionStrategy createAbstractionStrategy(
        Optional<VariableClassification> pVariableClassification);

    abstract Set<ProverOptions> getRequiredProverOptions();
  }

  enum InvariantStrengtheningStrategies {
    NO_STRENGTHENING {
      @Override
      InvariantStrengthening<SymbolicCandiateInvariant, SymbolicCandiateInvariant>
          createRefinementStrategy(AbstractionStrategy pAbstractionStrategy) {
        return InvariantStrengthenings.noStrengthening();
      }
    },

    UNSAT_CORE_BASED_STRENGTHENING {
      @Override
      InvariantStrengthening<SymbolicCandiateInvariant, SymbolicCandiateInvariant>
          createRefinementStrategy(AbstractionStrategy pAbstractionStrategy) {
        return InvariantStrengthenings.unsatCoreBasedStrengthening();
      }
    };

    abstract InvariantStrengthening<SymbolicCandiateInvariant, SymbolicCandiateInvariant>
        createRefinementStrategy(AbstractionStrategy pAbstractionStrategy);
  }

  private enum ConditionAdjustmentCriterion {
    NEVER {
      @Override
      boolean shouldAdjustConditions() {
        return false;
      }
    },

    ALWAYS {
      @Override
      boolean shouldAdjustConditions() {
        return true;
      }
    };

    abstract boolean shouldAdjustConditions();
  }

  private static class DetectingLiftingAbstractionFailureStrategy
      implements LiftingAbstractionFailureStrategy {

    private final boolean eager;

    private final Map<SymbolicCandiateInvariant, BlockedCounterexampleToInductivity>
        spuriousAbstractions = new HashMap<>();

    public DetectingLiftingAbstractionFailureStrategy(boolean pEager) {
      eager = pEager;
    }

    @Override
    public SymbolicCandiateInvariant handleLAF(
        FormulaManagerView pFMGR,
        PredicateAbstractionManager pPam,
        ProverEnvironmentWithFallback pProver,
        BlockedCounterexampleToInductivity pBlockedConcreteCti,
        SymbolicCandiateInvariant pBlockedAbstractCti,
        AssertCandidate pAssertPredecessor,
        Iterable<Object> pAssertionIds,
        AbstractionStrategy pAbstractionStrategy)
        throws CPATransferException, InterruptedException, SolverException {
      if (eager) {
        // Lifting refinement:
        return AbstractionBasedLifting.RefinementLAFStrategies.EAGER.handleLAF(
            pFMGR,
            pPam,
            pProver,
            pBlockedConcreteCti,
            pBlockedAbstractCti,
            pAssertPredecessor,
            pAssertionIds,
            pAbstractionStrategy);
      }
      spuriousAbstractions.put(pBlockedAbstractCti, pBlockedConcreteCti);
      return pBlockedAbstractCti;
    }

    Optional<SymbolicCandiateInvariant> getBlockedConcreteCtiForSpuriousAbstraction(
        SymbolicCandiateInvariant pBlockedAbstractCti) {
      return Optional.ofNullable(spuriousAbstractions.get(pBlockedAbstractCti));
    }
  }
}
