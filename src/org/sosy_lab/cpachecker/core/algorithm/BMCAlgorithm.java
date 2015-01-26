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

import javax.annotation.concurrent.GuardedBy;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPABuilder;
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
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
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
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
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

  private static class BMCStatistics implements Statistics {

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

  private final InvariantGenerator invariantGenerator;

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


  private final TargetLocationProvider tlp;

  private final boolean isProgramConcurrent;

  public BMCAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
                      Configuration pConfig, LogManager pLogger,
                      ReachedSetFactory pReachedSetFactory,
                      ShutdownNotifier pShutdownNotifier, CFA pCfa)
                      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this);

    algorithm = pAlgorithm;
    cpa = pCpa;
    config = pConfig;
    logger = pLogger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCfa;

    if (induction && useInvariantsForInduction) {
      invariantGenerator = new CPAInvariantGenerator(pConfig, pLogger, reachedSetFactory, pShutdownNotifier, cfa);
      CPABuilder builder = new CPABuilder(pConfig, pLogger, pShutdownNotifier, pReachedSetFactory);
      stepCaseCPA = builder.buildCPAWithSpecAutomatas(cfa);
      stepCaseAlgorithm = CPAAlgorithm.create(stepCaseCPA, pLogger, pConfig, pShutdownNotifier);
    } else {
      invariantGenerator = new DoNothingInvariantGenerator(reachedSetFactory);
      stepCaseCPA = cpa;
      stepCaseAlgorithm = algorithm;
    }
    stats.invariantGeneration = invariantGenerator.getTimeOfExecution();

    PredicateCPA predCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predCpa == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for BMCAlgorithm");
    }
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
    shutdownNotifier = pShutdownNotifier;
    machineModel = predCpa.getMachineModel();

    tlp = new TargetLocationProvider(reachedSetFactory, shutdownNotifier, logger, pConfig, cfa);

    isProgramConcurrent = from(cfa.getAllFunctionNames()).anyMatch(in(CONCURRENT_FUNCTIONS));
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

        ImmutableSet<CandidateInvariant> potentialInvariants = null;
        Set<CFAEdge> relevantAssumeEdges = null;
        ImmutableSet<CFANode> targetLocations = null;
        do {
          shutdownNotifier.shutdownIfNecessary();

          if (induction) {
            if (targetLocations == null && !isProgramConcurrent) {
              targetLocations = tlp.tryGetAutomatonTargetLocations(cfa.getMainFunction());
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

          soundInner = unroll(reachedSet, algorithm, cpa);
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
        BooleanFormula invariantInvalidity = bfmgr.not(bfmgr.and(from(assertAt(loopHeadStates, candidateInvariantBF, fmgr)).toList()));

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
    Iterable<AdjustableConditionCPA> conditionCPAs = getConditionCPAs(cpa);
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

      Set<ARGState> targetStates = from(pReachedSet).filter(IS_TARGET_STATE).filter(ARGState.class).toSet();

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
                                            .filter(IS_TARGET_STATE)
                                            .toList();

    if (checkTargetStates) {
      logger.log(Level.FINER, "Found", targetStates.size(), "potential target states");

      // create formula
      BooleanFormula program = createFormulaFor(targetStates, bfmgr);

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
      BooleanFormula assertions = createFormulaFor(stopStates, bfmgr);

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
  private BooleanFormula createFormulaFor(Iterable<AbstractState> states, BooleanFormulaManager pBFMGR) {
    BooleanFormula f = pBFMGR.makeBoolean(false);

    for (PredicateAbstractState e : AbstractStates.projectToType(states, PredicateAbstractState.class)) {
      f = pBFMGR.or(f, e.getPathFormula().getFormula());
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

    private final Boolean trivialResult;

    private final ReachedSet reachedSet;

    private final Loop loop;

    private final Solver stepCaseSolver;

    private final FormulaManagerView stepCaseFMGR;

    private final BooleanFormulaManagerView stepCaseBFMGR;

    private final PathFormulaManager stepCasePFMGR;

    private final ReachedSetInitializer reachedSetInitializer = new ReachedSetInitializer() {

      @Override
      public void initialize(ReachedSet pReachedSet) throws CPAException, InterruptedException {
        ensureReachedSetInitialized(pReachedSet);
      }
    };

    private ProverEnvironment prover = null;

    private UnmodifiableReachedSet invariantsReachedSet;

    private BooleanFormula currentInvariants;

    private int stackDepth = 0;

    @GuardedBy("this")
    private ImmutableSet<CFANode> targetLocations = null;

    @GuardedBy("this")
    private boolean targetLocationsChanged = false;

    private BooleanFormula previousFormula = null;

    private int previousK = -1;

    @GuardedBy("this")
    private ImmutableSet<CandidateInvariant> potentialLoopHeadInvariants = ImmutableSet.of();

    private Set<CandidateInvariant> knownLoopHeadInvariants = new HashSet<>();

    private boolean invariantGenerationRunning = true;

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
        LoopStructure loops = cfa.getLoopStructure().get();

        // Induction is currently only possible if there is a single loop.
        // This check can be weakened in the future,
        // e.g. it is ok if there is only a single loop on each path.
        if (loops.getCount() > 1) {
          logger.log(Level.WARNING, "Could not use induction for proving program safety, program has too many loops");
          invariantGenerator.cancel();
          trivialResult = false;
        } else if (loops.getCount() == 0) {
          // induction is unnecessary, program has no loops
          invariantGenerator.cancel();
          trivialResult = true;
        } else {
          stats.inductionPreparation.start();

          loop = Iterables.getOnlyElement(loops.getAllLoops());
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
            if (!isProgramConcurrent) {
              CFANode loopHead = Iterables.getOnlyElement(loop.getLoopHeads());
              targetLocations = tlp.tryGetAutomatonTargetLocations(loopHead);
            }
          }
          stats.inductionPreparation.stop();
        }
      }

      PredicateCPA stepCasePredicateCPA = CPAs.retrieveCPA(stepCaseCPA, PredicateCPA.class);
      stepCaseSolver = stepCasePredicateCPA.getSolver();
      stepCaseFMGR = stepCaseSolver.getFormulaManager();
      stepCaseBFMGR = stepCaseFMGR.getBooleanFormulaManager();
      stepCasePFMGR = stepCasePredicateCPA.getPathFormulaManager();
      currentInvariants = stepCaseBFMGR.makeBoolean(true);

      invariantsReachedSet = reachedSetFactory.create();
      this.reachedSet = reachedSet;
      this.loop = loop;
    }

    public ImmutableSet<CandidateInvariant> setPotentialLoopHeadInvariants(ImmutableSet<CandidateInvariant> pPotentialLoopHeadInvariants) {
      synchronized (this) {
        return this.potentialLoopHeadInvariants = from(pPotentialLoopHeadInvariants).filter(not(in(knownLoopHeadInvariants))).toSet();
      }
    }

    private ImmutableSet<CandidateInvariant> getPotentialLoopHeadInvariants() {
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
      UnmodifiableReachedSet currentInvariantsReachedSet = getCurrentInvariantsReachedSet();
      if (currentInvariantsReachedSet != invariantsReachedSet || !isProverInitialized()) {
        CFANode loopHead = Iterables.getOnlyElement(getLoop().getLoopHeads());
        invariantsReachedSet = currentInvariantsReachedSet;
        // get global invariants
        BooleanFormula invariants = getCurrentInvariants();
        injectInvariants(currentInvariantsReachedSet, loopHead);
        if (isProverInitialized()) {
          pop();
        } else {
          prover = stepCaseSolver.newProverEnvironmentWithModelGeneration();
        }
        invariants = stepCaseFMGR.instantiate(invariants, SSAMap.emptySSAMap().withDefault(1));
        push(invariants);
      }
      assert isProverInitialized();
      return prover;
    }

    private UnmodifiableReachedSet getCurrentInvariantsReachedSet() {
      if (!invariantGenerationRunning) {
        return invariantsReachedSet;
      }
      try {
        return invariantGenerator.get();
      } catch (CPAException e) {
        logger.log(Level.FINE, "Invariant generation encountered an exception.", e);
        invariantGenerationRunning = false;
        return invariantsReachedSet;
      } catch (InterruptedException e) {
        logger.log(Level.FINE, "Invariant generation has terminated:", e);
        invariantGenerationRunning = false;
        return invariantsReachedSet;
      }
    }

    /**
     * Gets the most current invariants generated by the invariant generator.
     *
     * @return the most current invariants generated by the invariant generator.
     */
    private BooleanFormula getCurrentInvariants() {
      if (!stepCaseBFMGR.isFalse(currentInvariants) && invariantGenerationRunning) {
        UnmodifiableReachedSet currentInvariantsReachedSet = getCurrentInvariantsReachedSet();
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
      InvariantsCPA invariantsCPA = CPAs.retrieveCPA(stepCaseCPA, InvariantsCPA.class);
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
        invariantsCPA.injectInvariant(pLocation, invariant.asFormula());
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
        return stepCaseBFMGR.makeBoolean(true); // no invariants available
      }

      Set<CFANode> targetLocations = getCurrentPotentialTargetLocations();
      // Check if the invariant generation was able to prove correctness for the program
      if (targetLocations != null && AbstractStates.filterLocations(pReachedSet, targetLocations).isEmpty()) {
        logger.log(Level.INFO, "Invariant generation found no target states.");
        invariantGenerator.cancel();
        return stepCaseBFMGR.makeBoolean(false);
      }

      BooleanFormula invariant = stepCaseBFMGR.makeBoolean(false);

      for (AbstractState locState : AbstractStates.filterLocation(pReachedSet, pLocation)) {
        BooleanFormula f = AbstractStates.extractReportedFormulas(stepCaseFMGR, locState);
        logger.log(Level.ALL, "Invariant:", f);

        invariant = stepCaseBFMGR.or(invariant, f);
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
      if (stepCaseBFMGR.isFalse(getCurrentInvariants())) {
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
      LoopstackCPA stepCaseloopstackCPA = CPAs.retrieveCPA(stepCaseCPA, LoopstackCPA.class);

      BooleanFormula safePredecessors;
      ReachedSet reached = getCurrentReachedSet();

      // Initialize the reached set if necessary
      ensureReachedSetInitialized(reached);

      // Create the formula asserting the safety for k consecutive predecessors
      if (previousFormula != null && this.previousK == k) {
        safePredecessors = stepCaseBFMGR.not(previousFormula);
      } else {
        final Iterable<AbstractState> predecessorTargetStates;
        if (k <= 1) {
          predecessorTargetStates = Collections.emptySet();
        } else {
          stepCaseloopstackCPA.setMaxLoopIterations(k);

          unroll(reached, reachedSetInitializer, stepCaseAlgorithm, stepCaseCPA);
          predecessorTargetStates = from(reached).filter(IS_TARGET_STATE);
        }
        safePredecessors = stepCaseBFMGR.not(createFormulaFor(predecessorTargetStates, stepCaseBFMGR));
      }
      stepCaseloopstackCPA.setMaxLoopIterations(k + 1);

      Map<CandidateInvariant, BooleanFormula> assumptionsAtState = new HashMap<>();

      Iterable<AbstractState> loopHeadStates = AbstractStates.filterLocations(reached, loop.getLoopHeads());

      for (BooleanFormula knownLoopHeadInvariant : transform(knownLoopHeadInvariants, stepCaseFMGR, stepCasePFMGR)) {
        // Assert the invariant at all loop head states
        safePredecessors = stepCaseBFMGR.and(safePredecessors,
            stepCaseBFMGR.and(from(assertAt(loopHeadStates, knownLoopHeadInvariant, stepCaseFMGR)).toList()));
      }

      BooleanFormula combinedPotentialLoopHeadInvariantAssertion = stepCaseBFMGR.makeBoolean(true);
      ImmutableSet<CandidateInvariant> potentialLoopHeadInvariants = getPotentialLoopHeadInvariants();
      for (CandidateInvariant potentialLoopHeadInvariant : potentialLoopHeadInvariants) {
        BooleanFormula potentialLoopHeadInvariantAssertion = stepCaseBFMGR.and(from(assertAt(loopHeadStates, potentialLoopHeadInvariant.getCandidate(stepCaseFMGR, stepCasePFMGR), stepCaseFMGR)).toList());
        combinedPotentialLoopHeadInvariantAssertion = stepCaseBFMGR.and(combinedPotentialLoopHeadInvariantAssertion, potentialLoopHeadInvariantAssertion);
        assumptionsAtState.put(potentialLoopHeadInvariant, potentialLoopHeadInvariantAssertion);
      }

      // Create the formula asserting the faultiness of the successor
      unroll(reached, reachedSetInitializer, stepCaseAlgorithm, stepCaseCPA);
      Set<AbstractState> targetStates = from(reached).filter(IS_TARGET_STATE).toSet();
      BooleanFormula unsafeSuccessor = createFormulaFor(from(targetStates), stepCaseBFMGR);
      this.previousFormula = unsafeSuccessor;

      ProverEnvironment prover = getProver();

      loopHeadStates = AbstractStates.filterLocations(reached, loop.getLoopHeads());
      BooleanFormula combinedPotentialLoopHeadInvariantContradiction = stepCaseBFMGR.makeBoolean(false);
      for (CandidateInvariant potentialLoopHeadInvariant : potentialLoopHeadInvariants) {

        BooleanFormula potentialLoopHeadInvariantAssertion = assumptionsAtState.get(potentialLoopHeadInvariant);
        BooleanFormula potentialLoopHeadInvariantContradiction = stepCaseBFMGR.not(stepCaseBFMGR.and(from(assertAt(loopHeadStates, potentialLoopHeadInvariant.getCandidate(stepCaseFMGR, stepCasePFMGR), stepCaseFMGR)).toList()));
        combinedPotentialLoopHeadInvariantContradiction = stepCaseBFMGR.or(combinedPotentialLoopHeadInvariantContradiction, potentialLoopHeadInvariantContradiction);

        // Try to prove the loop head invariant itself
        push(potentialLoopHeadInvariantAssertion);
        push(potentialLoopHeadInvariantContradiction);
        boolean isInvariant = prover.isUnsat();
        if (isInvariant) {
          knownLoopHeadInvariants.add(potentialLoopHeadInvariant);
          if (invariantGenerator instanceof CPAInvariantGenerator) {
            CPAInvariantGenerator invGen = (CPAInvariantGenerator) invariantGenerator;
            InvariantsCPA invariantsCPA = CPAs.retrieveCPA(invGen.getCPAs(), InvariantsCPA.class);
            Optional<AssumeEdge> assumption = potentialLoopHeadInvariant.getAssumeEdge();
            if (invariantsCPA != null && assumption.isPresent()) {
              invariantsCPA.injectInvariant(loop.getLoopHeads().iterator().next(), assumption.get());
            }
          }
        }
        // Pop loop invariant contradiction
        pop();
        // Pop loop invariant predecessor safety assertion
        pop();
      }
      this.previousK = k + 1;

      ImmutableSet<CFANode> newTargetLocations = from(targetStates).transform(AbstractStates.EXTRACT_LOCATION).toSet();
      setCurrentPotentialTargetLocations(newTargetLocations);

      stats.inductionPreparation.stop();

      logger.log(Level.INFO, "Starting induction check...");

      stats.inductionCheck.start();

      push(safePredecessors); // k consecutive iterations are SAFE


      // First check with candidate loop invariant
      push(combinedPotentialLoopHeadInvariantAssertion); // loop invariant holds for predecessors
      push(stepCaseBFMGR.or(unsafeSuccessor, combinedPotentialLoopHeadInvariantContradiction)); // combined contradiction to successor safety or loop invariant
      boolean sound = prover.isUnsat();

      UnmodifiableReachedSet localInvariantsReachedSet = invariantsReachedSet;
      UnmodifiableReachedSet currentInvariantsReachedSet = getCurrentInvariantsReachedSet();

      while (!sound && currentInvariantsReachedSet != localInvariantsReachedSet) {
        localInvariantsReachedSet = currentInvariantsReachedSet;
        BooleanFormula invariants = getCurrentInvariants();
        invariants = stepCaseFMGR.instantiate(invariants, SSAMap.emptySSAMap().withDefault(1));
        push(invariants);
        sound = prover.isUnsat();
        pop();
        currentInvariantsReachedSet = getCurrentInvariantsReachedSet();
      }

      pop(); // pop combined contradiction
      pop(); // pop loop invariant assertion for predecessors*/

      // If first check failed and a candidate loop invariant was tried out, check without the candidate loop invariant
      if (!sound && !potentialLoopHeadInvariants.isEmpty()) {
        push(unsafeSuccessor); // push plain contradiction to successor safety
        sound = prover.isUnsat();

        getCurrentInvariants();
        currentInvariantsReachedSet = invariantsReachedSet;
        while (!sound && currentInvariantsReachedSet != localInvariantsReachedSet) {
          localInvariantsReachedSet = currentInvariantsReachedSet;
          BooleanFormula invariants = getCurrentInvariants();
          invariants = stepCaseFMGR.instantiate(invariants, SSAMap.emptySSAMap().withDefault(1));
          push(invariants);
          sound = prover.isUnsat();
          pop();
          currentInvariantsReachedSet = getCurrentInvariantsReachedSet();
        }

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

    private void ensureReachedSetInitialized(ReachedSet pReachedSet) throws InterruptedException, CPAException {
      if (pReachedSet.size() > 1) {
        return;
      }
      CFANode loopHead = Iterables.getOnlyElement(getLoop().getLoopHeads());
      if (havocLoopTerminationConditionVariablesOnly) {
        CFANode mainEntryNode = cfa.getMainFunction();
        Precision precision = stepCaseCPA.getInitialPrecision(mainEntryNode, StateSpacePartition.getDefaultPartition());
        precision = excludeEdges(precision, CFAUtils.leavingEdges(loopHead));
        pReachedSet.add(stepCaseCPA.getInitialState(mainEntryNode, StateSpacePartition.getDefaultPartition()), precision);
        stepCaseAlgorithm.run(pReachedSet);
        Collection<AbstractState> loopHeadStates = new ArrayList<>();
        Iterables.addAll(loopHeadStates, filterLocation(pReachedSet, loopHead));
        pReachedSet.clear();
        Collection<String> loopTerminationConditionVariables = getTerminationConditionVariables(loop);
        for (AbstractState loopHeadState : loopHeadStates) {
          // Havoc the "loop termination condition" variables in predicate analysis state
          PredicateAbstractState pas = extractStateByType(loopHeadState, PredicateAbstractState.class);
          PathFormula pathFormula = pas.getPathFormula();
          SSAMapBuilder ssaMapBuilder = pathFormula.getSsa().builder();
          Set<String> containedVariables = ssaMapBuilder.allVariables();
          for (String variable : loopTerminationConditionVariables) {
            if (containedVariables.contains(variable)) {
              CType type = ssaMapBuilder.getType(variable);
              int freshIndex = ssaMapBuilder.getFreshIndex(variable);
              ssaMapBuilder.setIndex(variable, type, freshIndex);
            }
          }

          AbstractState newLoopHeadState = stepCaseCPA.getInitialState(loopHead, StateSpacePartition.getDefaultPartition());

          PredicateAbstractState newPAS = extractStateByType(newLoopHeadState, PredicateAbstractState.class);
          newPAS.setPathFormula(stepCasePFMGR.makeNewPathFormula(pathFormula, ssaMapBuilder.build()));

          pReachedSet.add(newLoopHeadState, stepCaseCPA.getInitialPrecision(loopHead, StateSpacePartition.getDefaultPartition()));
        }
      } else {
        Precision precision = stepCaseCPA.getInitialPrecision(loopHead, StateSpacePartition.getDefaultPartition());
        pReachedSet.add(stepCaseCPA.getInitialState(loopHead, StateSpacePartition.getDefaultPartition()), precision);
      }
    }

  }

  private Collection<String> getTerminationConditionVariables(Loop pLoop) throws CPATransferException, InterruptedException {
    Collection<String> result = new HashSet<>();
    result.add(CFASingleLoopTransformation.PROGRAM_COUNTER_VAR_NAME);
    CFANode loopHead = Iterables.getOnlyElement(pLoop.getLoopHeads());
    Set<CFANode> visited = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    waitlist.offer(loopHead);
    visited.add(loopHead);
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      assert pLoop.getLoopNodes().contains(current);
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(current)) {
        CFANode successor = leavingEdge.getSuccessor();
        if (!isLoopExitEdge(leavingEdge, pLoop)) {
          if (visited.add(successor)) {
            waitlist.offer(successor);
          }
        } else {
          PathFormula formula = pmgr.makeFormulaForPath(Collections.singletonList(leavingEdge));
          result.addAll(fmgr.extractVariableNames(fmgr.uninstantiate(formula.getFormula())));
        }
      }
    }
    return result;
  }

  private static boolean isLoopExitEdge(CFAEdge pEdge, Loop pLoop) {
    return !pLoop.getLoopNodes().contains(pEdge.getSuccessor());
  }

  private Iterable<BooleanFormula> assertAt(Iterable<AbstractState> pStates, final BooleanFormula pUninstantiatedFormula, final FormulaManagerView pFMGR) {
    return from(pStates).transform(new Function<AbstractState, BooleanFormula>() {

      @Override
      public BooleanFormula apply(AbstractState pInput) {
        return assertAt(pInput, pUninstantiatedFormula, pFMGR);
      }

    });
  }

  private BooleanFormula assertAt(AbstractState pState, BooleanFormula pUninstantiatedFormula, FormulaManagerView pFMGR) {
    PredicateAbstractState pas = AbstractStates.extractStateByType(pState, PredicateAbstractState.class);
    PathFormula pathFormula = pas.getPathFormula();
    BooleanFormula instantiatedFormula = pFMGR.instantiate(pUninstantiatedFormula, pathFormula.getSsa().withDefault(1));
    BooleanFormula stateFormula = pathFormula.getFormula();
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
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
  private boolean unroll(ReachedSet pReachedSet, Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA) throws CPAException, InterruptedException {
    return unroll(pReachedSet, new ReachedSetInitializer() {

      @Override
      public void initialize(ReachedSet pReachedSet) {
        // Do nothing
      }

    }, pAlgorithm, pCPA);
  }

  private boolean unroll(ReachedSet pReachedSet, ReachedSetInitializer pInitializer, Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA) throws CPAException, InterruptedException {
    adjustReachedSet(pReachedSet, pInitializer, pCPA);
    return pAlgorithm.run(pReachedSet);
  }

  /**
   * Adjusts the given reached set so that the involved adjustable condition
   * CPAs are able to operate properly without being negatively influenced by
   * states generated earlier under different conditions while trying to
   * retain as many states as possible.
   *
   * @param pReachedSet the reached set to be adjusted.
   * @param pReachedSetInitializer initializes the reached set.
   * @throws InterruptedException
   * @throws CPAException
   */
  private void adjustReachedSet(ReachedSet pReachedSet, ReachedSetInitializer pInitializer, ConfigurableProgramAnalysis pCPA) throws CPAException, InterruptedException {
    Preconditions.checkArgument(!pReachedSet.isEmpty());
    CFANode initialLocation = extractLocation(pReachedSet.getFirstState());
    for (AdjustableConditionCPA conditionCPA : getConditionCPAs(pCPA)) {
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
      pInitializer.initialize(pReachedSet);
      pReachedSet.add(
          pCPA.getInitialState(initialLocation, StateSpacePartition.getDefaultPartition()),
          pCPA.getInitialPrecision(initialLocation, StateSpacePartition.getDefaultPartition()));
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
      return Precisions.replaceByType(pPrecision, newPrecision, Predicates.instanceOf(EdgeExclusionPrecision.class));
    }
    return pPrecision;
  }

  private static Iterable<AdjustableConditionCPA> getConditionCPAs(ConfigurableProgramAnalysis pCPA) {
    return CPAs.asIterable(pCPA).filter(AdjustableConditionCPA.class);
  }

  private static interface CounterexampleStorage {

    void addCounterexample(ARGState pTargetState, CounterexampleInfo pCounterexample);

  }

  private static interface ReachedSetInitializer {

    void initialize(ReachedSet pReachedSet) throws CPAException, InterruptedException;

  }

  private static List<BooleanFormula> transform(Collection<CandidateInvariant> pCandidates, FormulaManagerView pFMGR, PathFormulaManager pPFMGR) throws CPATransferException, InterruptedException {

    List<BooleanFormula> formulas = new ArrayList<>(pCandidates.size());
    for (CandidateInvariant candidate : pCandidates) {
      formulas.add(candidate.getCandidate(pFMGR, pPFMGR));
    }
    return formulas;

  }

  private class CandidateInvariant {

    private final CFAEdge edge;

    public CandidateInvariant(CFAEdge pEdge) throws CPATransferException, InterruptedException {
      Preconditions.checkNotNull(pEdge);
      this.edge = pEdge;
    }

    public Optional<AssumeEdge> getAssumeEdge() {
      if (edge instanceof AssumeEdge) {
        AssumeEdge assumeEdge = (AssumeEdge) edge;
        CFANode predecessor = assumeEdge.getPredecessor();
        AssumeEdge otherEdge = CFAUtils.leavingEdges(predecessor).filter(not(equalTo(edge))).filter(AssumeEdge.class).iterator().next();
        return Optional.of(otherEdge);
      }
      return Optional.absent();
    }

    public BooleanFormula getCandidate(FormulaManagerView pFMGR, PathFormulaManager pPFMGR) throws CPATransferException, InterruptedException {
      PathFormula invariantPathFormula = pPFMGR.makeFormulaForPath(Collections.<CFAEdge>singletonList(edge));
      return pFMGR.getBooleanFormulaManager().not(pFMGR.uninstantiate(invariantPathFormula.getFormula()));
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO instanceof CandidateInvariant) {
        CandidateInvariant other = (CandidateInvariant) pO;
        return edge.equals(other.edge);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getAssumeEdge());
    }

    @Override
    public String toString() {
      try {
        return getCandidate(fmgr, pmgr).toString();
      } catch (CPATransferException e) {
        return String.format("not (%s)", edge);
      } catch (InterruptedException e) {
        return String.format("not (%s)", edge);
      }
    }

  }

}
