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
package org.sosy_lab.cpachecker.core.algorithm.pdr;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.BackwardTransition;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Blocks;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.AssignmentToPathAllocator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;

// TODO counterexample, logging, shutdown, refinement,
// generalization, build correct reachedSet in the end
/**
 * Property-Directed Reachability algorithm, also known as IC3.
 * It can be used to check whether a program is safe or not.
 */
public class PDRAlgorithm implements Algorithm, StatisticsProvider {

  /*
   *  Simple renaming due to the different meaning of this predicate when applied during the
   *  backwards-analysis in PDR.
   */
  private static final Predicate<AbstractState> IS_MAIN_ENTRY = AbstractStates.IS_TARGET_STATE;

  private final CFA cfa;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManagerView bfmgr;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final BackwardTransition backwardTransition;
  private final Algorithm algorithm;
  private final Configuration config;
  private final PDRStatistics stats = new PDRStatistics();

  private final AssignmentToPathAllocator assignmentToPathAllocator;

  private FrameSet frameSet;

  public PDRAlgorithm(
      ReachedSetFactory pReachedSetFactory,
      ConfigurableProgramAnalysis pCPA,
      Algorithm pAlgorithm,
      CFA pCFA,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    //    pConfig.inject(this); TODO use when actual options are determined and remove SuppressWarnings
    cfa = pCFA;
    backwardTransition = new BackwardTransition(pReachedSetFactory, pCPA, pAlgorithm);
    algorithm = pAlgorithm;
    config = pConfig;

    PredicateCPA predCpa = CPAs.retrieveCPA(pCPA, PredicateCPA.class);
    if (predCpa == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for PDRAlgorithm");
    }

    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr = predCpa.getPathFormulaManager();

    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;

    // re-initialized in run()
    frameSet = new DynamicFrameSet(cfa.getMainFunction(), fmgr, backwardTransition);
    assignmentToPathAllocator =
        new AssignmentToPathAllocator(config, shutdownNotifier, pLogger, cfa.getMachineModel());
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    CFANode startLocation = cfa.getMainFunction();
    ImmutableSet<CFANode> errorLocations =
        FluentIterable.from(pReachedSet).transform(AbstractStates.EXTRACT_LOCATION).toSet();
    pReachedSet.clear();

    // Utility prover environment that will be reused for small tests
    try (ProverEnvironment reusedProver =
        solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      // Check for 0-step counterexample
      for (CFANode errorLocation : errorLocations) {
        if (startLocation.equals(errorLocation)) {
          logger.log(Level.INFO, "Found errorpath: 0-step counterexample.");
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }

      // Check for 1-step counterexamples
      for (Block block : backwardTransition.getBlocksTo(errorLocations, IS_MAIN_ENTRY)) {
        reusedProver.push(block.getFormula());
        if (!reusedProver.isUnsat()) {
          logger.log(Level.INFO, "Found errorpath: 1-step counterexample.", " \nTransition : \n", block.getFormula());
          analyzeCounterexample(Collections.singletonList(block), pReachedSet);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }

        reusedProver.pop();
      }

      frameSet = new DynamicFrameSet(startLocation, fmgr, backwardTransition);

      /*
       * Main loop : Try to inductively strengthen highest frame set, propagate
       * states afterwards and check for termination.
       */
      while (!shutdownNotifier.shouldShutdown()) {
        frameSet.openNextFrameSet();
        logger.log(Level.INFO, "Increasing frontier to : ", frameSet.getMaxLevel());
        if (!strengthen(errorLocations)) {
          logger.log(Level.INFO, "Found errorpath. Program has a bug.");
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        //        try (ProverEnvironment propagationProver = solver.newProverEnvironment()) {
        //          frameSet.propagate(propagationProver, reusedProver); // TODO Enable when it works
        //        }
        if (isFrameSetConvergent()) {
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        shutdownNotifier.shutdownIfNecessary();
      }
    } catch (SolverException e) {
      throw new CPAException("Solver error.", e);
    }

    // Can't really reach this code
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  /**
   * Tries to prove that an error location cannot be reached with a number of steps
   * equal to {@link FrameSet#getMaxLevel()}.
   */
  private boolean strengthen(ImmutableSet<CFANode> pErrorLocations)
      throws InterruptedException, SolverException, CPAEnabledAnalysisPropertyViolationException,
          CPAException {

    for (CFANode errorLoc : pErrorLocations) {
      for (Block block : backwardTransition.getBlocksTo(errorLoc)) {
        CFANode errorPredecessor = block.getPredecessorLocation();

        try (ProverEnvironment prover =
            solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

          boolean isCurrentPathStillSatisfiable = true;

          /*
           * Try to block states until the transition from current error predecessor to error
           * location is longer possible.
           */
          while (isCurrentPathStillSatisfiable) {

            // Push frame states of error predecessor location.
            for (BooleanFormula frameState :
                frameSet.getStatesForLocation(errorPredecessor, frameSet.getMaxLevel())) {
              prover.push(fmgr.instantiate(frameState, block.getUnprimedContext().getSsa()));
            }

            // Push transition from error predecessor to corresponding error location.
            BooleanFormula localTransition = block.getFormula();
            prover.push(localTransition);

            // Transition is still possible. Get state from model and try to block it.
            if (!prover.isUnsat()) {
              Model model = prover.getModel();
              logger.log(Level.INFO, "Generating satisfying assignment for predecessor of bad state in strengthen().");
              BooleanFormula toBeBlocked =
                  getAbstractedSatisfyingState(model, block.getUnprimedContext());
              if (!backwardblock(errorPredecessor, toBeBlocked)) {
                return false;
              }
            } else {
              /*
               *  Transition from current predecessor location to error location is no longer possible.
               *  Continue with next one.
               */
              isCurrentPathStillSatisfiable = false;
            }
            shutdownNotifier.shutdownIfNecessary();
          }
        }
      }
    }
    return true;
  }

  // TODO Predicate abstraction
  /** Extract values for all variables to build formula for blocking. */
  private BooleanFormula getAbstractedSatisfyingState(Model pModel, PathFormula pUnprimedContext) {
    BooleanFormula toBeBlocked = bfmgr.makeTrue();

    for (String variableName : pUnprimedContext.getSsa().allVariables()) {
      CType type = pUnprimedContext.getSsa().getType(variableName);
      BitvectorFormula unprimedVar =
          (BitvectorFormula) pfmgr.makeFormulaForVariable(pUnprimedContext, variableName, type);
      BitvectorFormula value =
          fmgr.getBitvectorFormulaManager()
              .makeBitvector(fmgr.getFormulaType(unprimedVar), pModel.evaluate(unprimedVar));
      toBeBlocked =
          bfmgr.and(
              toBeBlocked,
              fmgr.getBitvectorFormulaManager().equal(fmgr.uninstantiate(unprimedVar), value));
    }

    return toBeBlocked;
  }

  /**
   * Recursively blocks bad states until either the initial one can be blocked or a counterexample
   * trace is found.
   */
  private boolean backwardblock(CFANode pErrorPredLocation, BooleanFormula pState)
      throws SolverException, InterruptedException, CPAEnabledAnalysisPropertyViolationException,
          CPAException {
    logger.log(Level.INFO, "Entering backwardblock.");
    PriorityQueue<ProofObligation> proofObligationQueue = new PriorityQueue<>();
    proofObligationQueue.offer(
        new ProofObligation(frameSet.getMaxLevel(), pErrorPredLocation, pState));

    // Inner loop : recursively block states.
    while (!proofObligationQueue.isEmpty()) {
      ProofObligation p =
          proofObligationQueue.poll(); // Inspect proof obligation with lowest frame level.

      // Frame level 0 implies that the program start location is reached. Thus a counterexample is found.
      if (p.getFrameLevel() == 0) {
        createCounterexampleTrace(p);
        return false;
      }

      try (ProverEnvironment prover =
          solver.newProverEnvironment(
              ProverOptions.GENERATE_UNSAT_CORE, ProverOptions.GENERATE_MODELS)) {

        // Checks if p.state is relative inductive to states known in predecessor locations.
        for (Block block : backwardTransition.getBlocksTo(p.getLocation())) {
          CFANode predLocation = block.getPredecessorLocation();

          int numberPushes = 0;

          // Push T(predLoc -> p.loc)
          BooleanFormula localTransition = block.getFormula();
          prover.push(localTransition);
          numberPushes++;

          // Push F(p.level - 1, predLoc) [unprimed]
          for (BooleanFormula frameState :
              frameSet.getStatesForLocation(predLocation, p.getFrameLevel() - 1)) {
            prover.push(fmgr.instantiate(frameState, block.getUnprimedContext().getSsa()));
            numberPushes++;
          }

          // Push p.state [primed]
          BooleanFormula primedState =
              fmgr.instantiate(p.getState(), block.getPrimedContext().getSsa());
          prover.push(primedState);
          numberPushes++;

          // Push not(p.state) [unprimed] if self-loop
          if (predLocation.equals(p.getLocation())) {
            BooleanFormula unprimedState =
                fmgr.instantiate(p.getState(), block.getUnprimedContext().getSsa());
            prover.push(bfmgr.not(unprimedState));
            numberPushes++;
          }

          if (!prover.isUnsat()) {
            /*
             * The consecution check failed. There is a predecessor for the current state that allows
             * it to be reached. Add new obligation to block predState at predLocation at one level lower.
             * Re-add old obligation for future re-inspection.
             */
            BooleanFormula predState =
                getAbstractedSatisfyingState(prover.getModel(), block.getUnprimedContext());
            proofObligationQueue.offer(
                new ProofObligation(p.getFrameLevel() - 1, predLocation, predState, p));
            proofObligationQueue.offer(p); // TODO add cause ?
            logger.log(Level.INFO, "Generating satisfying assignment for predecessor of bad state in backwardblock()."
                , " -- Length of queue is : ", proofObligationQueue.size());
          } else {
            // The consecution check succeeded. The state can't be reached and may therefore be blocked.

            /*
             *  TODO Maybe refine predicates here if abstract state leads to error and the concrete one does not.
             *  The NEGATED state must be generalized and added!
             */
            logger.log(Level.INFO, "Blocking state.");
            BooleanFormula generalizedState = generalize(p.getState(), prover);
            frameSet.blockState(generalizedState, p.getFrameLevel(), p.getLocation());
          }
          for(int i = 0; i < numberPushes; ++i) {
            prover.pop();
          }
        }
      }
    }
    return true;
  }

  /**
   * Tries to generalize the given clause by dropping literals that are not needed for the unsatisfiability
   * of the formulas on the given ProverEnvironment.
   */
  @SuppressWarnings("unused")
  private BooleanFormula generalize(BooleanFormula pClause, ProverEnvironment pProver) {
    return pClause; // TODO implement
  }

  /**
   * Checks if, for every location, any two neighboring frame sets are equal.
   * This signals termination of the algorithm and correctness of the checked program.
   */
  private boolean isFrameSetConvergent() {
    for (int currentLevel = 1;
        currentLevel < frameSet.getMaxLevel();
        ++currentLevel) { // TODO bounds ok ?
      Collection<Set<BooleanFormula>> statesAtCurrentLevel =
          frameSet.getStatesForAllLocations(currentLevel).values();
      Collection<Set<BooleanFormula>> statesAtNextLevel =
          frameSet.getStatesForAllLocations(currentLevel + 1).values();
      if (statesAtCurrentLevel.equals(statesAtNextLevel)) {
        logger.log(
            Level.INFO,
            "Frames converge at level ",
            currentLevel,
            " and ",
            String.valueOf(currentLevel + 1),
            ". Program is safe.");
        return true;
      }
    }
    return false;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  /**
   * Creates a counterexample trace based on the given proof obligation. It is the start of a chain of
   * obligations whose respective predecessors lead to the initial program location and thus represent
   * a proof that the program is not safe.
   */
  @SuppressWarnings("unused")
  private void createCounterexampleTrace(ProofObligation pFinalFailingObligation) {
    // TODO implement
  }

  private void analyzeCounterexample(List<Block> pBlocks, ReachedSet pTargetReachedSet)
      throws CPATransferException, InterruptedException {

    stats.getErrorPathCreationTimer().start();

    logger.log(Level.INFO, "Error found, creating error path");
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      BooleanFormula blockFormulaConjunctions = Blocks.conjoinBlockFormulas(pBlocks, fmgr);
      prover.push(blockFormulaConjunctions);

      // get the branchingFormula
      // this formula contains predicates for all branches we took
      // this way we can figure out which branches make a feasible path
      BooleanFormula branchingFormula = Blocks.conjoinBranchingFormulas(pBlocks, fmgr, pfmgr);

      prover.push(branchingFormula);

      List<ValueAssignment> model;
      try {
        // need to ask solver for satisfiability again,
        // otherwise model doesn't contain new predicates
        boolean stillSatisfiable = !prover.isUnsat();

        if (!stillSatisfiable) {
          // should not occur
          logger.log(
              Level.WARNING,
              "Could not create error path information because of inconsistent branching information!");
          return;
        }

        model = prover.getModelAssignments();

      } catch (SolverException e) {
        logger.log(Level.WARNING, "Solver could not produce model, cannot create error path.");
        logger.logDebugException(e);
        return;

      } finally {
        prover.pop(); // remove branchingFormula
      }

      // get precise error path
      Map<Integer, Boolean> branchingInformation =
          pfmgr.getBranchingPredicateValuesFromModel(model);

      ARGPath targetPath = Blocks.combineARGPaths(pBlocks, branchingInformation);

      // replay error path for a more precise satisfying assignment
      PathChecker pathChecker;
      try {
        Solver solver = this.solver;
        PathFormulaManager pmgr = this.pfmgr;

        if (solver.getVersion().toLowerCase().contains("smtinterpol")) {
          // SMTInterpol does not support reusing the same solver
          solver = Solver.create(config, logger, shutdownNotifier);
          FormulaManagerView formulaManager = solver.getFormulaManager();
          pmgr =
              new PathFormulaManagerImpl(
                  formulaManager,
                  config,
                  logger,
                  shutdownNotifier,
                  cfa,
                  AnalysisDirection.BACKWARD); // TODO direction?
        }

        pathChecker = new PathChecker(config, logger, pmgr, solver, assignmentToPathAllocator);

      } catch (InvalidConfigurationException e) {
        // Configuration has somehow changed and can no longer be used to create the solver and path formula manager
        logger.logUserException(
            Level.WARNING, e, "Could not replay error path to get a more precise model");
        return;
      }

      CounterexampleTraceInfo cexInfo =
          CounterexampleTraceInfo.feasible(
              ImmutableList.<BooleanFormula>of(blockFormulaConjunctions),
              model,
              branchingInformation);
      CounterexampleInfo counterexample =
          pathChecker.createCounterexample(targetPath, cexInfo, true);
      counterexample.getTargetPath().getLastState().addCounterexampleInformation(counterexample);

    } finally {
      stats.getErrorPathCreationTimer().stop();
    }
    Blocks.combineReachedSets(pBlocks, pTargetReachedSet);
  }
}
