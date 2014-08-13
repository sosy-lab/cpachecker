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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.FILTER_ABSTRACTION_STATES;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.DoNothingInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.ReachedSetUtils;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.edgeexclusion.EdgeExclusionPrecision;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsPrecision;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

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

  private class BMCStatistics implements Statistics {

    private final Timer satCheck = new Timer();
    private final Timer errorPathCreation = new Timer();
    private final Timer assertionsCheck = new Timer();

    private final Timer inductionPreparation = new Timer();
    private final Timer inductionCheck = new Timer();
    private Timer invariantGeneration;
    private int inductionCutPoints = 0;

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      if (satCheck.getNumberOfIntervals() > 0) {
        out.println("Time for final sat check:            " + satCheck);
      }
      if (errorPathCreation.getNumberOfIntervals() > 0) {
        out.println("Time for error path creation:        " + errorPathCreation);
      }
      if (assertionsCheck.getNumberOfIntervals() > 0) {
        out.println("Time for bounding assertions check:  " + assertionsCheck);
      }
      if (inductionCheck.getNumberOfIntervals() > 0) {
        out.println("Number of cut points for induction:  " + inductionCutPoints);
        out.println("Time for induction formula creation: " + inductionPreparation);
        if (invariantGeneration.getNumberOfIntervals() > 0) {
          out.println("  Time for invariant generation:     " + invariantGeneration);
        }
        out.println("Time for induction check:            " + inductionCheck);
      }
    }

    @Override
    public String getName() {
      return "BMC algorithm";
    }
  }

  @Option(description = "If BMC did not find a bug, check whether "
      + "the bounding did actually remove parts of the state space "
      + "(this is similar to CBMC's unwinding assertions).")
  private boolean boundingAssertions = true;

  @Option(description="Check reachability of target states after analysis "
      + "(classical BMC). The alternative is to check the reachability "
      + "as soon as the target states are discovered, which is done if "
      + "cpa.predicate.targetStateSatCheck=true.")
  private boolean checkTargetStates = true;

  @Option(description="try using induction to verify programs with loops")
  private boolean induction = false;

  @Option(description="Generate invariants and add them to the induction hypothesis.")
  private boolean useInvariantsForInduction = false;

  @Option(description="dump counterexample formula to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpCounterexampleFormula = Paths.get("ErrorPath.%d.smt2");

  private final BMCStatistics stats = new BMCStatistics();
  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;

  private final InvariantGenerator invariantGenerator;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final Solver solver;
  private final MachineModel machineModel;

  private final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;

  private final ShutdownNotifier shutdownNotifier;

  private final List<? extends AdjustableConditionCPA> conditionCPAs;

  private final BooleanFormulaManagerView bfmgr;

  public BMCAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
                      Configuration pConfig, LogManager pLogger,
                      ReachedSetFactory pReachedSetFactory,
                      ShutdownNotifier pShutdownNotifier, CFA pCfa)
                      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this);
    algorithm = pAlgorithm;
    cpa = pCpa;
    logger = pLogger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCfa;

    if (induction && useInvariantsForInduction) {
      invariantGenerator = new CPAInvariantGenerator(pConfig, pLogger, reachedSetFactory, pShutdownNotifier, cfa);
    } else {
      invariantGenerator = new DoNothingInvariantGenerator(reachedSetFactory);
    }
    stats.invariantGeneration = invariantGenerator.getTimeOfExecution();

    PredicateCPA predCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predCpa == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for BMCAlgorithm");
    }
    fmgr = predCpa.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
    solver = predCpa.getSolver();
    shutdownNotifier = pShutdownNotifier;
    conditionCPAs = CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class).toList();
    machineModel = predCpa.getMachineModel();
  }

  @Override
  public boolean run(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    final ReachedSet reachedSet = pReachedSet;

    CFANode initialLocation = extractLocation(reachedSet.getFirstState());

    invariantGenerator.start(initialLocation);

    try {
      logger.log(Level.INFO, "Creating formula for program");
      boolean soundInner;


      try (ProverEnvironment prover = solver.newProverEnvironmentWithModelGeneration();
          @SuppressWarnings("resource")
          KInductionProver kInductionProver = induction ? new KInductionProver() : null) {

        ImmutableSet<BooleanFormula> potentialInvariants = null;
        Set<CFAEdge> relevantAssumeEdges = null;
        ImmutableSet<CFANode> targetLocations = null;
        do {
          shutdownNotifier.shutdownIfNecessary();

          if (induction) {
            if (targetLocations == null && invariantGenerator instanceof CPAInvariantGenerator) {
              CPAInvariantGenerator invariantGenerator = (CPAInvariantGenerator) BMCAlgorithm.this.invariantGenerator;
              InvariantsCPA invariantsCPA = CPAs.retrieveCPA(invariantGenerator.getCPAs(), InvariantsCPA.class);
              if (invariantsCPA != null) {
                targetLocations = invariantsCPA.tryGetTargetLocations(cfa.getMainFunction());
              } else {
                targetLocations = kInductionProver.getCurrentPotentialTargetLocations();
              }
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

          soundInner = unroll(reachedSet);
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

          prover.pop(); // remove program formula from solver stack

          if (!safe) {
            return soundInner;
          } else if (induction && !kInductionProver.isTrivial()) {
            if (targetLocations != null) {
              if (relevantAssumeEdges == null || kInductionProver.haveCurrentPotentialTargetLocationsChanged()) {
                relevantAssumeEdges = getRelevantAssumeEdges(pReachedSet, targetLocations);
              }
              if (potentialInvariants != null) {
                potentialInvariants = from(potentialInvariants).filter(not(in(kInductionProver.knownLoopHeadInvariants))).toSet();
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

  private ImmutableSet<BooleanFormula> guessLoopHeadInvariants(ReachedSet pReachedSet, final Set<CFAEdge> pAssumeEdges,
      ProverEnvironment pProver, Loop pLoop, ImmutableSet<BooleanFormula> pPreviousLoopHeadInvariants) throws CPAException, InterruptedException {

    if (pAssumeEdges.isEmpty()) {
      return ImmutableSet.of();
    }

    Iterable<AbstractState> loopHeadStates = AbstractStates.filterLocations(pReachedSet, pLoop.getLoopHeads());

    ImmutableSet.Builder<BooleanFormula> candidateInvariants = new ImmutableSet.Builder<>();

    for (CFAEdge assumeEdge : pAssumeEdges) {
      PathFormula invariantPathFormula = pmgr.makeFormulaForPath(Collections.singletonList(assumeEdge));
      BooleanFormula negatedCandidateInvariant = fmgr.uninstantiate(invariantPathFormula.getFormula());
      BooleanFormula candidateInvariant = bfmgr.not(negatedCandidateInvariant);

      if (pPreviousLoopHeadInvariants == null || pPreviousLoopHeadInvariants.contains(candidateInvariant)) {

        // Is there any loop head state, where the assumption does not hold?
        BooleanFormula invariantInvalidity = bfmgr.not(bfmgr.and(from(assertAt(loopHeadStates, candidateInvariant)).toList()));

        pProver.push(invariantInvalidity);
        if (pProver.isUnsat()) {
          candidateInvariants.add(candidateInvariant);
        } else if (logger.wouldBeLogged(Level.ALL)) {
          logger.log(Level.ALL, candidateInvariant, "is not an invariant:", pProver.getModel());
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
    FluentIterable<AbstractState> targetStates = from(pReachedSet).filter(IS_TARGET_STATE);
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
    for (AdjustableConditionCPA condCpa : conditionCPAs) {
      if (!condCpa.adjustPrecision()) {
        // this cpa said "do not continue"
        logger.log(Level.INFO, "Terminating because of", condCpa.getClass().getSimpleName());
        return false;
      }
    }
    return !conditionCPAs.isEmpty();
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

      Set<ARGState> targetStates = from(pReachedSet).filter(ARGState.class).filter(IS_TARGET_STATE).toSet();

      final boolean shouldCheckBranching;
      if (targetStates.size() == 1) {
        ARGState state = Iterables.getOnlyElement(targetStates);
        while (state.getParents().size() == 1) {
          state = Iterables.getOnlyElement(state.getParents());
        }
        shouldCheckBranching = !state.getParents().isEmpty();
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

        try {
          model = pProver.getModel();
        } catch (SolverException e) {
          logger.log(Level.WARNING, "Solver could not produce model, cannot create error path.");
          logger.logDebugException(e);
          return;
        }
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

      } catch (CPATransferException e) {
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
  private boolean checkTargetStates(final ReachedSet pReachedSet, final ProverEnvironment prover) throws InterruptedException {
    List<AbstractState> targetStates = from(pReachedSet)
                                            .filter(IS_TARGET_STATE)
                                            .toList();

    if (checkTargetStates) {
      logger.log(Level.FINER, "Found", targetStates.size(), "potential target states");

      // create formula
      BooleanFormula program = createFormulaFor(targetStates);

      logger.log(Level.INFO, "Starting satisfiability check...");
      stats.satCheck.start();
      prover.push(program);
      boolean safe = prover.isUnsat();
      // leave program formula on solver stack
      stats.satCheck.stop();

      if (safe) {
        pReachedSet.removeAll(targetStates);
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
  private boolean checkBoundingAssertions(final ReachedSet pReachedSet, final ProverEnvironment prover) throws InterruptedException {
    FluentIterable<AbstractState> stopStates = from(pReachedSet)
                                                    .filter(IS_STOP_STATE);

    if (boundingAssertions) {
      // create formula for unwinding assertions
      BooleanFormula assertions = createFormulaFor(stopStates);

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

  /**
   * Create a disjunctive formula of all the path formulas in the supplied iterable.
   */
  private BooleanFormula createFormulaFor(Iterable<AbstractState> states) {
    BooleanFormula f = bfmgr.makeBoolean(false);

    for (PredicateAbstractState e : AbstractStates.projectToType(states, PredicateAbstractState.class)) {
      f = bfmgr.or(f, e.getPathFormula().getFormula());
    }

    return f;
  }


  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  /**
   * Instances of this class are used to prove the safety of a program by
   * applying an inductive approach based on k-induction.
   */
  private class KInductionProver implements AutoCloseable {

    private ProverEnvironment prover = null;

    private final Boolean trivialResult;

    private final ReachedSet reachedSet;

    private final Loop loop;

    private UnmodifiableReachedSet invariantsReachedSet;

    private BooleanFormula currentInvariants = bfmgr.makeBoolean(true);

    private int stackDepth = 0;

    private ImmutableSet<CFANode> targetLocations = null;

    private boolean targetLocationsChanged = false;

    private BooleanFormula previousFormula = null;

    private int previousK = -1;

    private ImmutableSet<BooleanFormula> potentialLoopHeadInvariants = ImmutableSet.of();

    private Set<BooleanFormula> knownLoopHeadInvariants = new HashSet<>();

    /**
     * Creates an instance of the KInductionProver.
     */
    public KInductionProver() {
      List<CFAEdge> incomingEdges = null;
      ReachedSet reachedSet = null;
      Loop loop = null;
      if (!cfa.getLoopStructure().isPresent()) {
        logger.log(Level.WARNING, "Could not use induction for proving program safety, loop structure of program could not be determined.");
        trivialResult = false;
      } else {
        Multimap<String, Loop> loops = cfa.getLoopStructure().get();

        // Induction is currently only possible if there is a single loop.
        // This check can be weakened in the future,
        // e.g. it is ok if there is only a single loop on each path.
        if (loops.size() > 1) {
          logger.log(Level.WARNING, "Could not use induction for proving program safety, program has too many loops");
          invariantGenerator.cancel();
          trivialResult = false;
        } else if (loops.isEmpty()) {
          // induction is unnecessary, program has no loops
          invariantGenerator.cancel();
          trivialResult = true;
        } else {
          stats.inductionPreparation.start();

          loop = Iterables.getOnlyElement(loops.values());
          // function edges do not count as incoming/outgoing edges
          incomingEdges = from(loop.getIncomingEdges()).filter(not(instanceOf(CFunctionReturnEdge.class))).toList();

          if (incomingEdges.size() > 1) {
            logger.log(Level.WARNING, "Could not use induction for proving program safety, loop has too many incoming edges", incomingEdges);
            trivialResult = false;
          } else if (loop.getLoopHeads().size() > 1) {
            logger.log(Level.WARNING, "Could not use induction for proving program safety, loop has too many loop heads");
            trivialResult = false;
          } else {
            trivialResult = null;
            reachedSet = reachedSetFactory.create();
            CFANode loopHead = Iterables.getOnlyElement(loop.getLoopHeads());

            if (invariantGenerator instanceof CPAInvariantGenerator) {
              CPAInvariantGenerator invariantGenerator = (CPAInvariantGenerator) BMCAlgorithm.this.invariantGenerator;
              InvariantsCPA invariantsCPA = CPAs.retrieveCPA(invariantGenerator.getCPAs(), InvariantsCPA.class);
              if (invariantsCPA != null) {
                targetLocations = invariantsCPA.tryGetTargetLocations(loopHead);
              }
            }

            Precision precision = cpa.getInitialPrecision(loopHead);
            reachedSet.add(cpa.getInitialState(loopHead), precision);
          }
          stats.inductionPreparation.stop();
        }
      }
      this.reachedSet = reachedSet;
      this.loop = loop;
    }

    public ImmutableSet<BooleanFormula> setPotentialLoopHeadInvariants(ImmutableSet<BooleanFormula> pPotentialLoopHeadInvariants) {
      synchronized (this) {
        return this.potentialLoopHeadInvariants = from(pPotentialLoopHeadInvariants).filter(not(in(knownLoopHeadInvariants))).toSet();
      }
    }

    private ImmutableSet<BooleanFormula> getPotentialLoopHeadInvariants() {
      synchronized (this) {
        return this.potentialLoopHeadInvariants;
      }
    }

    /**
     * Checks if the result of the k-induction check has been determined to
     * be trivial by the constructor.
     *
     * @return {@code true} if the constructor was able to determine a constant
     * result for the k-induction check, {@code false} otherwise.
     */
    private boolean isTrivial() {
      return this.trivialResult != null;
    }

    /**
     * If available, gets the constant result of the k-induction check as
     * determined by the constructor. Do not call this function if there is no
     * such trivial constant result. This can be checked by calling
     * {@link isTrivial}.
     *
     * @return the trivial constant result of the k-induction check.
     */
    private boolean getTrivialResult() {
      Preconditions.checkState(isTrivial(), "The proof is non-trivial.");
      return trivialResult;
    }

    /**
     * Gets the current reached set describing the loop iterations unrolled for
     * the inductive step. The reached set is only available if no trivial
     * constant result for the k-induction check was determined by the
     * constructor, as can be checked by calling {@link isTrivial}.
     *
     * @return the current reached set describing the loop iterations unrolled
     * for the inductive step.
     */
    private ReachedSet getCurrentReachedSet() {
      Preconditions.checkState(!isTrivial(), "No reached set created, because the proof is trivial.");
      assert reachedSet != null;
      return reachedSet;
    }

    /**
     * Gets the single loop of the program. This loop is only available if no
     * trivial constant result for the k-induction check was determined by the
     * constructor, as can be checked by calling {@link isTrivial}.
     *
     * @return the single loop of the program.
     */
    private Loop getLoop() {
      Preconditions.checkState(!isTrivial(), "No loop computed, because the proof is trivial.");
      assert loop != null;
      return loop;
    }

    /**
     * Checks if the prover is already initialized.
     *
     * @return {@code true} if the prover is initialized, {@code false}
     * otherwise.
     */
    private boolean isProverInitialized() {
      return prover != null;
    }

    /**
     * Gets the prover environment to be used within the KInductionProver.
     *
     * This prover may be preinitialized with additional supporting invariants.
     * The presence of these invariants, including pushing them onto and
     * popping them off of the prover stack, is taken care of automatically.
     *
     * @return the prover environment to be used within the KInductionProver.
     *
     * @throws CPAException if the supporting invariant generation encountered
     * an exception.
     * @throws InterruptedException if the supporting invariant generation is
     * interrupted.
     */
    private ProverEnvironment getProver() throws CPAException, InterruptedException {
      UnmodifiableReachedSet currentInvariantsReachedSet = invariantGenerator.get();
      if (currentInvariantsReachedSet != invariantsReachedSet) {
        CFANode loopHead = Iterables.getOnlyElement(getLoop().getLoopHeads());
        invariantsReachedSet = currentInvariantsReachedSet;
        // get global invariants
        BooleanFormula invariants = getCurrentInvariants();
        injectInvariants(currentInvariantsReachedSet, loopHead);
        if (isProverInitialized()) {
          pop();
        } else {
          prover = solver.newProverEnvironmentWithModelGeneration();
        }
        invariants = fmgr.instantiate(invariants, SSAMap.emptySSAMap().withDefault(1));
        push(invariants);
      }
      assert isProverInitialized();
      return prover;
    }

    /**
     * Gets the most current invariants generated by the invariant generator.
     *
     * @return the most current invariants generated by the invariant generator.
     *
     * @throws CPAException if the invariant generation encountered an exception.
     * @throws InterruptedException if the invariant generation is interrupted.
     */
    private BooleanFormula getCurrentInvariants() throws CPAException, InterruptedException {
      if (!bfmgr.isFalse(currentInvariants)) {
        UnmodifiableReachedSet currentInvariantsReachedSet = invariantGenerator.get();
        if (currentInvariantsReachedSet != invariantsReachedSet || haveCurrentPotentialTargetLocationsChanged()) {
          CFANode loopHead = Iterables.getOnlyElement(getLoop().getLoopHeads());
          currentInvariants = extractInvariantsAt(currentInvariantsReachedSet, loopHead);
        }
      }
      return currentInvariants;
    }

    /**
     * Attempts to inject the generated invariants into the bounded analysis
     * CPAs to improve their performance.
     *
     * Currently, this is only supported for the InvariantsCPA. If the
     * InvariantsCPA is not activated for both the bounded analysis as well as
     * the invariant generation, this function does nothing.
     *
     * @param pReachedSet the invariant generation reached set.
     * @param pLocation the location for which to extract and re-inject the
     * invariants.
     */
    private void injectInvariants(UnmodifiableReachedSet pReachedSet, CFANode pLocation) {
      InvariantsCPA invariantsCPA = CPAs.retrieveCPA(cpa, InvariantsCPA.class);
      if (invariantsCPA == null) {
        return;
      }
      InvariantsState invariant = null;
      for (AbstractState locState : AbstractStates.filterLocation(pReachedSet, pLocation)) {
        InvariantsState disjunctivePart = AbstractStates.extractStateByType(locState, InvariantsState.class);
        if (disjunctivePart != null) {
          if (invariant == null) {
            invariant = disjunctivePart;
          } else {
            invariant = invariant.join(disjunctivePart, InvariantsPrecision.getEmptyPrecision());
          }
        } else {
          return;
        }
      }
      if (invariant != null) {
        invariantsCPA.injectInvariant(pLocation, invariant);
      }
    }

    @Override
    public void close() {
      if (isProverInitialized()) {
        while (stackDepth-- > 0) {
          prover.pop();
        }
        prover.close();
      }
    }

    /**
     * Pops the last formula from the prover stack.
     */
    private void pop() {
      Preconditions.checkState(isProverInitialized());
      Preconditions.checkState(stackDepth > 0);
      prover.pop();
      --stackDepth;
    }

    /**
     * Pushes the given formula to the prover stack.
     *
     * @param pFormula the formula to be pushed.
     */
    private void push(BooleanFormula pFormula) {
      Preconditions.checkState(isProverInitialized());
      prover.push(pFormula);
      ++stackDepth;
    }

    /**
     * Extracts the generated invariants for the given location from the
     * given reached set produced by the invariant generator.
     *
     * @param pReachedSet the reached set produced by the invariant generator.
     * @param pLocation the location to extract the invariants for.
     *
     * @return the extracted invariants as a boolean formula.
     */
    private BooleanFormula extractInvariantsAt(UnmodifiableReachedSet pReachedSet, CFANode pLocation) {

      if (pReachedSet.isEmpty()) {
        return bfmgr.makeBoolean(true); // no invariants available
      }

      Set<CFANode> targetLocations = getCurrentPotentialTargetLocations();
      // Check if the invariant generation was able to prove correctness for the program
      if (targetLocations != null && AbstractStates.filterLocations(pReachedSet, targetLocations).isEmpty()) {
        logger.log(Level.INFO, "Invariant generation found no target states.");
        invariantGenerator.cancel();
        return bfmgr.makeBoolean(false);
      }

      BooleanFormula invariant = bfmgr.makeBoolean(false);

      for (AbstractState locState : AbstractStates.filterLocation(pReachedSet, pLocation)) {
        BooleanFormula f = AbstractStates.extractReportedFormulas(fmgr, locState);
        logger.log(Level.ALL, "Invariant:", f);

        invariant = bfmgr.or(invariant, f);
      }
      return invariant;
    }

    public ImmutableSet<CFANode> getCurrentPotentialTargetLocations() {
      synchronized (this) {
        return this.targetLocations;
      }
    }

    private void setCurrentPotentialTargetLocations(ImmutableSet<CFANode> pTargetLocations) {
      synchronized (this) {
        this.targetLocationsChanged = pTargetLocations.equals(this.targetLocations);
        this.targetLocations = pTargetLocations;
      }
    }

    private boolean haveCurrentPotentialTargetLocationsChanged() {
      synchronized (this) {
        return this.targetLocationsChanged;
      }
    }

    /**
     * Attempts to perform the inductive check.
     *
     * @return <code>true</code> if k-induction successfully proved the
     * correctness, <code>false</code> if the attempt was inconclusive.
     *
     * @throws CPAException if the bounded analysis constructing the step case
     * encountered an exception.
     * @throws InterruptedException if the bounded analysis constructing the
     * step case was interrupted.
     */
    public final boolean check() throws CPAException, InterruptedException {
      // Early return if there is a trivial result for the inductive approach
      if (isTrivial()) {
        return getTrivialResult();
      }

      // Early return if the invariant generation proved the program correct
      if (bfmgr.isFalse(getCurrentInvariants())) {
        return true;
      }

      stats.inductionPreparation.start();

      // Proving program safety with induction consists of two parts:
      // 1) Prove all paths safe that go only one iteration through the loop.
      //    This is part of the classic bounded model checking done above,
      //    so we don't care about this here.
      // 2) Assume that one loop iteration is safe and prove that the next one is safe, too.

      // Create initial reached set:
      // Run algorithm in order to create formula (A & B)
      logger.log(Level.INFO, "Running algorithm to create induction hypothesis");

      LoopstackCPA loopstackCPA = CPAs.retrieveCPA(cpa, LoopstackCPA.class);
      int k = loopstackCPA.getMaxLoopIterations();

      BooleanFormula safePredecessors;
      ReachedSet reached = getCurrentReachedSet();

      // Create the formula asserting the safety for k consecutive predecessors
      if (previousFormula != null && this.previousK == k - 1) {
        safePredecessors = bfmgr.not(previousFormula);
      } else {
        final Iterable<AbstractState> predecessorTargetStates;
        if (k <= 1) {
          predecessorTargetStates = Collections.emptySet();
        } else {
          loopstackCPA.setMaxLoopIterations(k - 1);

          unroll(reached);
          predecessorTargetStates = from(reached).filter(IS_TARGET_STATE);

          loopstackCPA.setMaxLoopIterations(k);
        }
        safePredecessors = bfmgr.not(createFormulaFor(predecessorTargetStates));
      }

      Map<BooleanFormula, BooleanFormula> assumptionsAtState = new HashMap<>();

      Iterable<AbstractState> loopHeadStates = AbstractStates.filterLocations(reached, loop.getLoopHeads());

      for (BooleanFormula knownLoopHeadInvariant : knownLoopHeadInvariants) {
        // Assert the invariant at all loop head states
        safePredecessors = bfmgr.and(safePredecessors,
            bfmgr.and(from(assertAt(loopHeadStates, knownLoopHeadInvariant)).toList()));
      }

      BooleanFormula combinedPotentialLoopHeadInvariantAssertion = bfmgr.makeBoolean(true);
      for (BooleanFormula potentialLoopHeadInvariant : getPotentialLoopHeadInvariants()) {
        BooleanFormula potentialLoopHeadInvariantAssertion = bfmgr.and(from(assertAt(loopHeadStates, potentialLoopHeadInvariant)).toList());
        combinedPotentialLoopHeadInvariantAssertion = bfmgr.and(combinedPotentialLoopHeadInvariantAssertion, potentialLoopHeadInvariant);
        assumptionsAtState.put(potentialLoopHeadInvariant, potentialLoopHeadInvariantAssertion);
      }

      // Create the formula asserting the faultiness of the successor
      unroll(reached);
      Set<AbstractState> targetStates = from(reached).filter(IS_TARGET_STATE).toSet();
      BooleanFormula unsafeSuccessor = createFormulaFor(from(targetStates));
      this.previousFormula = unsafeSuccessor;

      ProverEnvironment prover = getProver();

      loopHeadStates = AbstractStates.filterLocations(reached, loop.getLoopHeads());
      BooleanFormula combinedPotentialLoopHeadInvariantContradiction = bfmgr.makeBoolean(false);
      for (BooleanFormula potentialLoopHeadInvariant : getPotentialLoopHeadInvariants()) {
        BooleanFormula potentialLoopHeadInvariantAssertion = assumptionsAtState.get(potentialLoopHeadInvariant);
        BooleanFormula potentialLoopHeadInvariantContradiction = bfmgr.not(bfmgr.and(from(assertAt(loopHeadStates, potentialLoopHeadInvariant)).toList()));
        combinedPotentialLoopHeadInvariantContradiction = bfmgr.or(combinedPotentialLoopHeadInvariantContradiction, potentialLoopHeadInvariantContradiction);

        // Try to prove the loop head invariant itself
        push(potentialLoopHeadInvariantAssertion);
        push(potentialLoopHeadInvariantContradiction);
        if (prover.isUnsat()) {
          knownLoopHeadInvariants.add(potentialLoopHeadInvariant);
        } else {
          pop();
        }
        pop();
      }
      this.previousK = k;

      ImmutableSet<CFANode> newTargetLocations = from(targetStates).transform(AbstractStates.EXTRACT_LOCATION).toSet();
      setCurrentPotentialTargetLocations(newTargetLocations);

      stats.inductionPreparation.stop();

      logger.log(Level.INFO, "Starting induction check...");

      stats.inductionCheck.start();

      // First check with candidate loop invariant
      push(safePredecessors); // k consecutive iterations are SAFE
      push(combinedPotentialLoopHeadInvariantAssertion); // loop invariant holds for predecessors
      push(bfmgr.or(unsafeSuccessor, combinedPotentialLoopHeadInvariantContradiction)); // combined contradiction to successor safety or loop invariant
      boolean sound = prover.isUnsat();

      pop(); // pop combined contradiction
      pop(); // pop loop invariant assertion for predecessors

      // If first check failed and a candidate loop invariant was tried out, check without the candidate loop invariant
      if (!sound && potentialLoopHeadInvariants.isEmpty()) {
        push(unsafeSuccessor); // push plain contradiction to successor safety
        sound = prover.isUnsat();
        if (!sound && logger.wouldBeLogged(Level.ALL)) {
          logger.log(Level.ALL, "Model returned for induction check:", prover.getModel());
        }
        pop(); // pop plain contradiction to successor safety ("unsafe successor")
      }
      pop(); // pop assertion of safe predecessors

      stats.inductionCheck.stop();

      logger.log(Level.FINER, "Soundness after induction check:", sound);

      return sound;
    }

  }

  private Iterable<BooleanFormula> assertAt(Iterable<AbstractState> pStates, final BooleanFormula pUninstantiatedFormula) {
    return from(pStates).transform(new Function<AbstractState, BooleanFormula>() {

      @Override
      public BooleanFormula apply(AbstractState pInput) {
        return assertAt(pInput, pUninstantiatedFormula);
      }

    });
  }

  private BooleanFormula assertAt(AbstractState pState, BooleanFormula pUninstantiatedFormula) {
    PredicateAbstractState pas = AbstractStates.extractStateByType(pState, PredicateAbstractState.class);
    PathFormula pathFormula = pas.getPathFormula();
    BooleanFormula instantiatedFormula = fmgr.instantiate(pUninstantiatedFormula, pathFormula.getSsa().withDefault(1));
    BooleanFormula stateFormula = pathFormula.getFormula();
    return bfmgr.or(bfmgr.not(stateFormula), instantiatedFormula);
  }

  /**
   * Unrolls the given reached set using the algorithm provided to this
   * instance of the bounded model checking algorithm.
   *
   * @param pReachedSet the reached set to unroll.
   *
   * @return {@code true} if the unrolling was sound, {@code false} otherwise.
   *
   * @throws CPAException if an exception occurred during unrolling the reached
   * set.
   * @throws InterruptedException if the unrolling is interrupted.
   */
  private boolean unroll(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    return unroll(pReachedSet, Collections.<CFAEdge>emptySet());
  }

  /**
   * Unrolls the given reached set using the algorithm provided to this
   * instance of the bounded model checking algorithm.
   *
   * @param pReachedSet the reached set to unroll.
   * @param pExcludedEdges edges that are excluded in the current edge
   * exclusion precision and should stay excluded.
   *
   * @return {@code true} if the unrolling was sound, {@code false} otherwise.
   *
   * @throws CPAException
   * @throws InterruptedException
   */
  private boolean unroll(ReachedSet pReachedSet, Iterable<CFAEdge> pExcludedEdges) throws CPAException, InterruptedException {
    adjustReachedSet(pReachedSet, pExcludedEdges);
    return algorithm.run(pReachedSet);
  }

  /**
   * Adjusts the given reached set so that the involved adjustable condition
   * CPAs are able to operate properly without being negatively influenced by
   * states generated earlier under different conditions while trying to
   * retain as many states as possible.
   *
   * @param pReachedSet the reached set to be adjusted.
   * @param pExcludedEdges the edges that were excluded and should also be
   * excluded in the future in case the reached set needs to be cleared and
   * reinitialized.
   */
  private void adjustReachedSet(ReachedSet pReachedSet, Iterable<CFAEdge> pExcludedEdges) {
    Preconditions.checkArgument(!pReachedSet.isEmpty());
    CFANode initialLocation = extractLocation(pReachedSet.getFirstState());
    for (AdjustableConditionCPA conditionCPA : conditionCPAs) {
      if (conditionCPA instanceof ReachedSetAdjustingCPA) {
        ((ReachedSetAdjustingCPA) conditionCPA).adjustReachedSet(pReachedSet);
      } else {
        pReachedSet.clear();
        logger.log(Level.WARNING, "Completely clearing the reached set after condition adjustment due to " + conditionCPA.getClass()
            + ". This may drastically impede the efficiency of iterative deepening. Implement ReachedSetAdjustingCPA to avoid this problem.");
        break;
      }
    }
    if (pReachedSet.isEmpty()) {
      Precision precision = cpa.getInitialPrecision(initialLocation);
      precision = excludeEdges(precision, pExcludedEdges);
      pReachedSet.add(cpa.getInitialState(initialLocation), precision);
    }
  }

  /**
   * Excludes the given edges from the given precision if the EdgeExclusionCPA
   * is activated to allow for such edge exclusions.
   *
   * @param pPrecision the precision to exclude the edges from.
   * @param pEdgesToIgnore the edges to be excluded.
   * @return the new precision.
   */
  private Precision excludeEdges(Precision pPrecision, Iterable<CFAEdge> pEdgesToIgnore) {
    EdgeExclusionPrecision oldPrecision = Precisions.extractPrecisionByType(pPrecision, EdgeExclusionPrecision.class);
    if (oldPrecision != null) {
      EdgeExclusionPrecision newPrecision = oldPrecision.excludeMoreEdges(pEdgesToIgnore);
      return Precisions.replaceByType(pPrecision, newPrecision, EdgeExclusionPrecision.class);
    }
    return pPrecision;
  }

  private static interface CounterexampleStorage {

    void addCounterexample(ARGState pTargetState, CounterexampleInfo pCounterexample);

  }

}
