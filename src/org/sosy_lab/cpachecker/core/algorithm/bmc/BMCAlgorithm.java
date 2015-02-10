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

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.FILTER_ABSTRACTION_STATES;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.DoNothingInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.ReachedSetUtils;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

@Options(prefix="bmc")
public class BMCAlgorithm implements Algorithm, StatisticsProvider {

  private static final Predicate<AbstractState> IS_STOP_STATE =
    Predicates.compose(new Predicate<AssumptionStorageState>() {
                             @Override
                             public boolean apply(AssumptionStorageState pArg0) {
                               return (pArg0 != null) && pArg0.isStop();
                             }
                           },
                       AbstractStates.toState(AssumptionStorageState.class));

  /**
   * If these functions appear in the program, we must assume that the program
   * contains concurrency and we cannot rule out error locations that appear to
   * be syntactically unreachable.
   */
  private static final Set<String> CONCURRENT_FUNCTIONS = ImmutableSet.of("pthread_create");

  @Option(secure=true, description = "If BMC did not find a bug, check whether "
      + "the bounding did actually remove parts of the state space "
      + "(this is similar to CBMC's unwinding assertions).")
  private boolean boundingAssertions = true;

  @Option(secure=true, description="Check reachability of target states after analysis "
      + "(classical BMC). The alternative is to check the reachability "
      + "as soon as the target states are discovered, which is done if "
      + "cpa.predicate.targetStateSatCheck=true.")
  private boolean checkTargetStates = true;

  @Option(secure=true, description="try using induction to verify programs with loops")
  private boolean induction = false;

  @Option(secure=true, description="Generate invariants and add them to the induction hypothesis.")
  private boolean useInvariantsForInduction = false;

  @Option(secure=true, description="Generate additional invariants by induction and add them to the induction hypothesis.")
  private boolean addInvariantsByInduction = true;

  @Option(secure=true, description="Adds pre-loop information to the induction hypothesis. "
      + "This is unsound and should generally not be used; however "
      + "it is provided as an implementation of the technique introduced in "
      + "the SV-COMP 2013 competition contribution of ESBMC 1.20.")
  private boolean havocLoopTerminationConditionVariablesOnly = false;

  @Option(secure=true, description="dump counterexample formula to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpCounterexampleFormula = PathTemplate.ofFormatString("ErrorPath.%d.smt2");

  private final BMCStatistics stats = new BMCStatistics();
  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;

  private final ConfigurableProgramAnalysis stepCaseCPA;
  private final Algorithm stepCaseAlgorithm;

  private InvariantGenerator invariantGenerator;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;
  private final MachineModel machineModel;

  private final Configuration config;
  private final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;

  private final ShutdownNotifier shutdownNotifier;

  private final TargetLocationProvider targetLocationProvider;

  private final boolean isProgramConcurrent;

  private final Predicate<? super AbstractState> isTargetState;

  private KInductionProver kInductionProver = null;

  private boolean isInvariantGenerator = false;

  public BMCAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA,
                      Configuration pConfig, LogManager pLogger,
                      ReachedSetFactory pReachedSetFactory,
                      ShutdownNotifier pShutdownNotifier, CFA pCFA)
                      throws InvalidConfigurationException, CPAException {
    this(pAlgorithm, pCPA, pConfig, pLogger, pReachedSetFactory, pShutdownNotifier, pCFA, IS_TARGET_STATE);
    if (addInvariantsByInduction
        && pCFA.getLoopStructure().isPresent()
        && pCFA.getLoopStructure().get().getAllLoopHeads().size() == 1) {
      addInvariantsByInduction = false;
      ShutdownNotifier invGenBMCShutdownNotfier = ShutdownNotifier.createWithParent(pShutdownNotifier);
      CPABuilder builder = new CPABuilder(pConfig, pLogger, invGenBMCShutdownNotfier, pReachedSetFactory);
      ConfigurableProgramAnalysis invGenBMCCPA = builder.buildCPAWithSpecAutomatas(cfa);
      Algorithm invGenBMCCPAAlgorithm = CPAAlgorithm.create(invGenBMCCPA, pLogger, pConfig, invGenBMCShutdownNotfier);
      BMCAlgorithm invGenBMC = new BMCAlgorithm(invGenBMCCPAAlgorithm, invGenBMCCPA, pConfig, pLogger, pReachedSetFactory, invGenBMCShutdownNotfier, pCFA, IS_TARGET_STATE);
      invGenBMC.isInvariantGenerator = true;

      PredicateCPA stepCasePredicateCPA = CPAs.retrieveCPA(stepCaseCPA, PredicateCPA.class);

      KInductionInvariantGenerator kIndInvGen =
          new KInductionInvariantGenerator(invGenBMC, pReachedSetFactory, invGenBMCCPA, pLogger, invGenBMCShutdownNotfier, pCFA,
              pCFA.getLoopStructure().get().getAllLoopHeads().iterator().next(), stepCasePredicateCPA.getPathFormulaManager(), true);
      this.invariantGenerator = kIndInvGen;
    }
  }

  public BMCAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA,
                      Configuration pConfig, LogManager pLogger,
                      ReachedSetFactory pReachedSetFactory,
                      ShutdownNotifier pShutdownNotifier, CFA pCFA,
                      Predicate<? super AbstractState> pIsTargetStatePredicate)
                      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this);

    algorithm = pAlgorithm;
    cpa = pCPA;
    config = pConfig;
    logger = pLogger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCFA;
    isTargetState = pIsTargetStatePredicate;

    if (induction && useInvariantsForInduction) {
      invariantGenerator = new CPAInvariantGenerator(pConfig, pLogger, reachedSetFactory, pShutdownNotifier, cfa);
    } else {
      invariantGenerator = new DoNothingInvariantGenerator(reachedSetFactory);
    }

    CPABuilder builder = new CPABuilder(pConfig, pLogger, pShutdownNotifier, pReachedSetFactory);
    stepCaseCPA = builder.buildCPAWithSpecAutomatas(cfa);
    stepCaseAlgorithm = CPAAlgorithm.create(stepCaseCPA, pLogger, pConfig, pShutdownNotifier);

    stats.invariantGeneration = invariantGenerator.getTimeOfExecution();

    PredicateCPA predCpa = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    if (predCpa == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for BMCAlgorithm");
    }
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
    shutdownNotifier = pShutdownNotifier;
    machineModel = predCpa.getMachineModel();

    targetLocationProvider = new TargetLocationProvider(reachedSetFactory, shutdownNotifier, logger, pConfig, cfa);

    isProgramConcurrent = from(cfa.getAllFunctionNames()).anyMatch(in(CONCURRENT_FUNCTIONS));
  }

  public Collection<CandidateInvariant> getCurrentLoopHeadInvariants() {
    KInductionProver kInductionProver = this.kInductionProver;
    if (kInductionProver == null) {
      return Collections.emptySet();
    }
    return knownLoopHeadInvariants = kInductionProver.getKnownLoopHeadInvariants();
  }

  public BooleanFormula getCurrentLocationInvariants(CFANode pLocation, FormulaManagerView pFMGR) {
    KInductionProver kInductionProver = this.kInductionProver;
    if (kInductionProver == null) {
      return pFMGR.getBooleanFormulaManager().makeBoolean(true);
    }
    try {
      invGenReachedSet = kInductionProver.getCurrentInvariantsReachedSet();
      return kInductionProver.getCurrentLocationInvariants(pLocation, pFMGR);
    } catch (InterruptedException | CPAException e) {
      return pFMGR.getBooleanFormulaManager().makeBoolean(true);
    }
  }

  private Collection<CandidateInvariant> knownLoopHeadInvariants = null;

  private UnmodifiableReachedSet invGenReachedSet = null;

  public boolean areNewInvariantsAvailable() {
    KInductionProver kInductionProver = this.kInductionProver;
    if (kInductionProver == null) {
      return false;
    }
    if (!Objects.equals(knownLoopHeadInvariants, kInductionProver.getKnownLoopHeadInvariants())) {
      return true;
    }
    if (!Objects.equals(invGenReachedSet, kInductionProver.getCurrentInvariantsReachedSet())) {
      return true;
    }
    return false;
  }

  private boolean run1(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    final ReachedSet reachedSet = pReachedSet;

    CFANode initialLocation = extractLocation(reachedSet.getFirstState());

    invariantGenerator.start(initialLocation);

    try {
      logger.log(Level.INFO, "Creating formula for program");
      boolean soundInner;


      try (ProverEnvironment prover = solver.newProverEnvironmentWithModelGeneration();
          @SuppressWarnings("resource")
          KInductionProver kInductionProver = createInductionProver()) {

        this.kInductionProver = kInductionProver;

        ImmutableSet<CandidateInvariant> potentialInvariants = null;
        Set<CFAEdge> relevantAssumeEdges = null;
        ImmutableSet<CFANode> targetLocations = null;
        do {
          shutdownNotifier.shutdownIfNecessary();

          if (induction) {
            if (targetLocations == null && !isProgramConcurrent) {
              targetLocations = targetLocationProvider.tryGetAutomatonTargetLocations(cfa.getMainFunction());
            } else {
              targetLocations = kInductionProver.getCurrentPotentialTargetLocations();
            }
            if (targetLocations != null && targetLocations.isEmpty()) {
              logger.log(Level.INFO, "Invariant generation found no target states.");
              invariantGenerator.cancel();
              for (AbstractState waitlistState : new ArrayList<>(pReachedSet.getWaitlist())) {
                pReachedSet.removeOnlyFromWaitlist(waitlistState);
              }
              return true;
            }
          }

          soundInner = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);
          if (from(reachedSet)
              .skip(1) // first state of reached is always an abstraction state, so skip it
              .transform(toState(PredicateAbstractState.class))
              .anyMatch(FILTER_ABSTRACTION_STATES)) {

            logger.log(Level.WARNING, "BMC algorithm does not work with abstractions. Could not check for satisfiability!");
            return soundInner;
          }

          if (addInvariantsByInduction && induction && !kInductionProver.isTrivial()) {
            if (targetLocations != null) {
              if (relevantAssumeEdges == null || kInductionProver.haveCurrentPotentialTargetLocationsChanged()) {
                relevantAssumeEdges = getRelevantAssumeEdges(pReachedSet, targetLocations);
              }
              if (potentialInvariants != null) {
                potentialInvariants = from(potentialInvariants).filter(not(in(kInductionProver.getKnownLoopHeadInvariants()))).toSet();
              }
              potentialInvariants = guessLoopHeadInvariants(reachedSet, relevantAssumeEdges, prover, kInductionProver.getLoop(), potentialInvariants);
              potentialInvariants = kInductionProver.setPotentialLoopHeadInvariants(potentialInvariants);
            }
          }
          if (potentialInvariants == null || potentialInvariants.isEmpty()) {
            return true;
          }

          // try to prove program safety via induction
          if (induction && kInductionProver.check()) {
            return true;
          }
        }
        while (soundInner && adjustConditions());
      }

      return false;
    } finally {
      if (reachedSet != pReachedSet) {
        pReachedSet.clear();
        ReachedSetUtils.addReachedStatesToOtherReached(reachedSet, pReachedSet);
      }
    }
  }

  @Override
  public boolean run(final ReachedSet pReachedSet) throws CPAException, InterruptedException {

    if (isInvariantGenerator && !(invariantGenerator instanceof KInductionInvariantGenerator)) {
      return run1(pReachedSet);
    }

    final ReachedSet reachedSet = pReachedSet;

    CFANode initialLocation = extractLocation(reachedSet.getFirstState());

    invariantGenerator.start(initialLocation);

    try {
      logger.log(Level.INFO, "Creating formula for program");
      boolean soundInner;


      try (ProverEnvironment prover = solver.newProverEnvironmentWithModelGeneration();
          @SuppressWarnings("resource")
          KInductionProver kInductionProver = createInductionProver()) {

        this.kInductionProver = kInductionProver;

        ImmutableSet<CandidateInvariant> potentialInvariants = null;
        Set<CFAEdge> relevantAssumeEdges = null;
        ImmutableSet<CFANode> targetLocations = null;
        do {
          shutdownNotifier.shutdownIfNecessary();

          if (induction) {
            if (targetLocations == null && !isProgramConcurrent) {
              targetLocations = targetLocationProvider.tryGetAutomatonTargetLocations(cfa.getMainFunction(), false);
            } else {
              targetLocations = kInductionProver.getCurrentPotentialTargetLocations();
            }
            if (targetLocations != null && targetLocations.isEmpty()) {
              logger.log(Level.INFO, "Invariant generation found no target states.");
              invariantGenerator.cancel();
              for (AbstractState waitlistState : new ArrayList<>(pReachedSet.getWaitlist())) {
                pReachedSet.removeOnlyFromWaitlist(waitlistState);
              }
              return true;
            }
          }

          soundInner = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);
          if (from(reachedSet)
              .skip(1) // first state of reached is always an abstraction state, so skip it
              .transform(toState(PredicateAbstractState.class))
              .anyMatch(FILTER_ABSTRACTION_STATES)) {

            logger.log(Level.WARNING, "BMC algorithm does not work with abstractions. Could not check for satisfiability!");
            return soundInner;
          }

          // first check safety
          boolean safe = checkTargetStates(reachedSet, prover);
          logger.log(Level.FINER, "Program is safe?:", safe);

          if (!safe) {
            createErrorPath(reachedSet, prover);
          }

          if (checkTargetStates) {
            prover.pop(); // remove program formula from solver stack
          }

          if (!safe) {
            return soundInner;
          } else if (addInvariantsByInduction && induction && !kInductionProver.isTrivial()) {
            if (targetLocations != null) {
              if (relevantAssumeEdges == null || kInductionProver.haveCurrentPotentialTargetLocationsChanged()) {
                relevantAssumeEdges = getRelevantAssumeEdges(pReachedSet, targetLocations);
              }
              if (potentialInvariants != null) {
                potentialInvariants = from(potentialInvariants).filter(not(in(kInductionProver.getKnownLoopHeadInvariants()))).toSet();
              }
              potentialInvariants = guessLoopHeadInvariants(reachedSet, relevantAssumeEdges, prover, kInductionProver.getLoop(), potentialInvariants);
              potentialInvariants = kInductionProver.setPotentialLoopHeadInvariants(potentialInvariants);
            }
          }

          // second check soundness
          boolean sound = false;

          // verify soundness, but don't bother if we are unsound anyway or we have found a bug
          if (soundInner && safe) {

            // check bounding assertions
            sound = checkBoundingAssertions(reachedSet, prover);

            // try to prove program safety via induction
            if (induction) {
              sound = sound || kInductionProver.check();
            }
            if (sound) {
              return true;
            }
          }
        }
        while (soundInner && adjustConditions());
      }

      return false;
    } finally {
      invariantGenerator.cancel();
      if (reachedSet != pReachedSet) {
        pReachedSet.clear();
        ReachedSetUtils.addReachedStatesToOtherReached(reachedSet, pReachedSet);
      }
    }
  }

  private ImmutableSet<CandidateInvariant> guessLoopHeadInvariants(ReachedSet pReachedSet, final Set<CFAEdge> pAssumeEdges,
      ProverEnvironment pProver, Loop pLoop, ImmutableSet<CandidateInvariant> pPreviousLoopHeadInvariants) throws CPAException, InterruptedException {

    if (pAssumeEdges.isEmpty()) {
      return ImmutableSet.of();
    }

    Iterable<AbstractState> loopHeadStates = AbstractStates.filterLocations(pReachedSet, pLoop.getLoopHeads());

    ImmutableSet.Builder<CandidateInvariant> candidateInvariants = new ImmutableSet.Builder<>();

    for (CFAEdge assumeEdge : pAssumeEdges) {
      CandidateInvariant candidateInvariant = new CandidateInvariant(assumeEdge);
      BooleanFormula candidateInvariantBF = candidateInvariant.getCandidate(fmgr, pmgr);

      if (pPreviousLoopHeadInvariants == null || pPreviousLoopHeadInvariants.contains(candidateInvariant)) {

        // Is there any loop head state, where the assumption does not hold?
        BooleanFormula invariantInvalidity = bfmgr.not(bfmgr.and(from(BMCHelper.assertAt(loopHeadStates, candidateInvariantBF, fmgr)).toList()));

        pProver.push(invariantInvalidity);
        if (pProver.isUnsat()) {
          candidateInvariants.add(candidateInvariant);
        } else if (logger.wouldBeLogged(Level.ALL)) {
          logger.log(Level.ALL, candidateInvariantBF, "is not an invariant:", pProver.getModel());
        }
        pProver.pop();
      }

    }

    return candidateInvariants.build();
  }

  /**
   * @param pReachedSet
   * @param pTargetLocations
   * @return
   */
  private Set<CFAEdge> getRelevantAssumeEdges(ReachedSet pReachedSet, ImmutableSet<CFANode> pTargetLocations) {
    FluentIterable<AbstractState> targetStates = from(pReachedSet).filter(isTargetState);
    final Set<CFAEdge> assumeEdges = new HashSet<>();
    Set<CFANode> targetLocations = from(Iterables.concat(pTargetLocations, targetStates.transform(EXTRACT_LOCATION))).toSet();
    Set<CFANode> visited = new HashSet<>(targetLocations);
    Queue<CFANode> waitlist = new ArrayDeque<>(targetLocations);
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(current)) {
        CFANode predecessor = enteringEdge.getPredecessor();
        if (enteringEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
          assumeEdges.add(enteringEdge);
        } else if (visited.add(predecessor)) {
          waitlist.add(predecessor);
        }
      }
    }
    return assumeEdges;
  }

  /**
   * Adjusts the conditions of the CPAs which support the adjusting of
   * conditions.
   *
   * @return {@code true} if all CPAs supporting the feature agreed on
   * adjusting their conditions, {@code false} if one of the CPAs does not
   * support any further adjustment of conditions.
   */
  private boolean adjustConditions() {
    Iterable<AdjustableConditionCPA> conditionCPAs = CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class);
    for (AdjustableConditionCPA condCpa : conditionCPAs) {
      if (!condCpa.adjustPrecision()) {
        // this cpa said "do not continue"
        logger.log(Level.INFO, "Terminating because of", condCpa.getClass().getSimpleName());
        return false;
      }
    }
    return !Iterables.isEmpty(conditionCPAs);
  }

  /**
   * This method tries to find a feasible path to (one of) the target state(s).
   * It does so by asking the solver for a satisfying assignment.
   * @throws InterruptedException
   */
  private void createErrorPath(final ReachedSet pReachedSet, final ProverEnvironment pProver) throws CPATransferException, InterruptedException {
    addCounterexampleTo(pReachedSet, pProver, new CounterexampleStorage() {

      @Override
      public void addCounterexample(ARGState pTargetState, CounterexampleInfo pCounterexample) {
        ((ARGCPA) cpa).addCounterexample(pTargetState, pCounterexample);
      }
    });
  }

  private void addCounterexampleTo(final ReachedSet pReachedSet, final ProverEnvironment pProver, CounterexampleStorage pCounterexampleStorage) throws CPATransferException, InterruptedException {
    if (!(cpa instanceof ARGCPA)) {
      logger.log(Level.INFO, "Error found, but error path cannot be created without ARGCPA");
      return;
    }

    stats.errorPathCreation.start();
    try {
      logger.log(Level.INFO, "Error found, creating error path");

      Set<ARGState> targetStates = from(pReachedSet).filter(isTargetState).filter(ARGState.class).toSet();

      final boolean shouldCheckBranching;
      if (targetStates.size() == 1) {
        ARGState state = Iterables.getOnlyElement(targetStates);
        while (state.getParents().size() == 1 && state.getChildren().size() <= 1) {
          state = Iterables.getOnlyElement(state.getParents());
        }
        shouldCheckBranching = (state.getParents().size() > 1)
            || (state.getChildren().size() > 1);
      } else {
        shouldCheckBranching = true;
      }

      if (shouldCheckBranching) {
        Iterable<ARGState> arg = from(pReachedSet).filter(ARGState.class);

        // get the branchingFormula
        // this formula contains predicates for all branches we took
        // this way we can figure out which branches make a feasible path
        BooleanFormula branchingFormula = pmgr.buildBranchingFormula(arg);

        if (bfmgr.isTrue(branchingFormula)) {
          logger.log(Level.WARNING, "Could not create error path because of missing branching information!");
          return;
        }

        // add formula to solver environment
        pProver.push(branchingFormula);
      }

      Model model;

      try {

        // need to ask solver for satisfiability again,
        // otherwise model doesn't contain new predicates
        boolean stillSatisfiable = !pProver.isUnsat();

        if (!stillSatisfiable) {
          // should not occur
          logger.log(Level.WARNING, "Could not create error path information because of inconsistent branching information!");
          return;
        }

        model = pProver.getModel();

      } catch (SolverException e) {
        logger.log(Level.WARNING, "Solver could not produce model, cannot create error path.");
        logger.logDebugException(e);
        return;

      } finally {
        if (shouldCheckBranching) {
          pProver.pop(); // remove branchingFormula
        }
      }


      // get precise error path
      Map<Integer, Boolean> branchingInformation = pmgr.getBranchingPredicateValuesFromModel(model);
      ARGState root = (ARGState)pReachedSet.getFirstState();

      ARGPath targetPath;
      try {
        targetPath = ARGUtils.getPathFromBranchingInformation(root, pReachedSet.asCollection(), branchingInformation);
      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "Could not create error path");
        return;
      }

      // create and store CounterexampleInfo object
      CounterexampleInfo counterexample;


      // replay error path for a more precise satisfying assignment
      Solver solver = this.solver;
      PathFormulaManager pmgr = this.pmgr;

      // SMTInterpol does not support reusing the same solver
      if (solver.getFormulaManager().getVersion().toLowerCase().contains("smtinterpol")) {
        try {
          solver = Solver.create(config, logger, shutdownNotifier);
          FormulaManagerView formulaManager = solver.getFormulaManager();
          pmgr = new PathFormulaManagerImpl(formulaManager, config, logger, shutdownNotifier, cfa, AnalysisDirection.FORWARD);
        } catch (InvalidConfigurationException e) {
          // Configuration has somehow changed and can no longer be used to create the solver and path formula manager
          logger.logUserException(Level.WARNING, e, "Could not replay error path to get a more precise model");
          return;
        }
      }
      PathChecker pathChecker = new PathChecker(logger, shutdownNotifier, pmgr, solver, machineModel);
      try {
        CounterexampleTraceInfo info = pathChecker.checkPath(targetPath.getInnerEdges());

        if (info.isSpurious()) {
          logger.log(Level.WARNING, "Inconsistent replayed error path!");
          counterexample = CounterexampleInfo.feasible(targetPath, model);

        } else {
          counterexample = CounterexampleInfo.feasible(targetPath, info.getModel());

          counterexample.addFurtherInformation(fmgr.dumpFormula(bfmgr.and(info.getCounterExampleFormulas())),
              dumpCounterexampleFormula);
        }

      } catch (SolverException | CPATransferException e) {
        // path is now suddenly a problem
        logger.logUserException(Level.WARNING, e, "Could not replay error path to get a more precise model");
        counterexample = CounterexampleInfo.feasible(targetPath, model);
      }
      pCounterexampleStorage.addCounterexample(targetPath.getLastState(), counterexample);

    } finally {
      stats.errorPathCreation.stop();
    }
  }

  /**
   * Checks the reachability of the target states contained in the given
   * reached set by performing a satisfiability check with the given prover.
   *
   * @param pReachedSet the reached set containing the target states.
   * @param prover the prover to be used.
   * @param pTargetLocations the target locations.
   *
   * @return {@code true} if no target states are reachable, {@code false}
   * otherwise.
   *
   * @throws InterruptedException if the satisfiability check was interrupted.
   */
  private boolean checkTargetStates(final ReachedSet pReachedSet, final ProverEnvironment prover)
      throws SolverException, InterruptedException {
    List<AbstractState> targetStates = from(pReachedSet)
                                            .filter(isTargetState)
                                            .toList();

    if (checkTargetStates) {
      logger.log(Level.FINER, "Found", targetStates.size(), "potential target states");

      // create formula
      BooleanFormula program = BMCHelper.createFormulaFor(targetStates, bfmgr);

      logger.log(Level.INFO, "Starting satisfiability check...");
      stats.satCheck.start();
      prover.push(program);
      boolean safe = prover.isUnsat();
      // leave program formula on solver stack
      stats.satCheck.stop();

      if (safe) {
        pReachedSet.removeAll(targetStates);
        for (ARGState s : from(targetStates).filter(ARGState.class)) {
          s.removeFromARG();
        }
      }

      return safe;

    } else {
      // fast check for trivial cases
      return targetStates.isEmpty();
    }
  }

  /**
   * Checks if the bounded unrolling completely unrolled all reachable loop
   * iterations by performing a satisfiablity check on the formulas encoding
   * the reachability of the states where the bounded model check stopped due
   * to reaching the bound.
   *
   * If this is is the case, then the bounded model check is guaranteed to be
   * sound.
   *
   * @param pReachedSet the reached set containing the frontier of the bounded
   * model check, i.e. where the bounded model check stopped.
   * @param prover the prover to be used to prove that the stop states are
   * unreachable.
   *
   * @return {@code true} if the bounded model check covered all reachable
   * states and was thus sound, {@code false} otherwise.
   *
   * @throws InterruptedException if the satisfiability check is interrupted.
   */
  private boolean checkBoundingAssertions(final ReachedSet pReachedSet, final ProverEnvironment prover)
      throws SolverException, InterruptedException {
    FluentIterable<AbstractState> stopStates = from(pReachedSet)
                                                    .filter(IS_STOP_STATE);

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
  }

  private KInductionProver createInductionProver() {
    Supplier<Integer> bmcKAccessor = new Supplier<Integer> () {

      @Override
      public Integer get() {
        LoopstackCPA loopstackCPA = CPAs.retrieveCPA(cpa, LoopstackCPA.class);
        return loopstackCPA.getMaxLoopIterations();
      }

    };
    return induction ? new KInductionProver(
        cfa,
        logger,
        stepCaseAlgorithm,
        stepCaseCPA,
        invariantGenerator,
        stats,
        reachedSetFactory,
        isProgramConcurrent,
        targetLocationProvider,
        havocLoopTerminationConditionVariablesOnly,
        bmcKAccessor,
        isTargetState,
        shutdownNotifier) : null;
  }

  private static interface CounterexampleStorage {

    void addCounterexample(ARGState pTargetState, CounterexampleInfo pCounterexample);

  }

  static List<BooleanFormula> transform(Collection<CandidateInvariant> pCandidates, FormulaManagerView pFMGR, PathFormulaManager pPFMGR) throws CPATransferException, InterruptedException {

    List<BooleanFormula> formulas = new ArrayList<>(pCandidates.size());
    for (CandidateInvariant candidate : pCandidates) {
      formulas.add(candidate.getCandidate(pFMGR, pPFMGR));
    }
    return formulas;

  }

  @Override
  public boolean reset() {
    return true;
  }

}
