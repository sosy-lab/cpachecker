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
package org.sosy_lab.cpachecker.core.algorithm.pdr.old;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.pdr.old.PDRSat.ConsecutionResult;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Blocks;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.ForwardTransition;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.AssignmentToPathAllocator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

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
  private final PredicatePrecisionManager predicateManager;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final ForwardTransition backwardTransition;
  private final Algorithm algorithm;
  private final Configuration config;
  private final PDRStatistics stats;

  private final AssignmentToPathAllocator assignmentToPathAllocator;

  private FrameSet frameSet;
  private PDRSat pdrSolver;

  /**
   * Creates a new PDRAlgorithm instance.
   * @param pReachedSetFactory used for creating temporary reached sets for backwards analysis
   * @param pCPA the composite CPA that contains all needed CPAs
   * @param pAlgorithm the algorithm used for traversing the cfa
   * @param pCFA the control flow automaton of the program
   * @param pConfig the configuration that contains the components and options for this algorithm
   * @param pLogger the logging component
   * @param pShutdownNotifier the component that is used to shutdown this algorithm if necessary
   * @throws InvalidConfigurationException if the configuration file is invalid or incomplete
   */
  public PDRAlgorithm(
      ReachedSetFactory pReachedSetFactory,
      ConfigurableProgramAnalysis pCPA,
      Algorithm pAlgorithm,
      CFA pCFA,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    cfa = pCFA;
    backwardTransition = new ForwardTransition(pReachedSetFactory, pCPA, pAlgorithm);
    algorithm = pAlgorithm;
    config = pConfig;

    PredicateCPA predCpa = CPAs.retrieveCPA(pCPA, PredicateCPA.class);
    if (predCpa == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for PDRAlgorithm");
    }

    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    pfmgr = predCpa.getPathFormulaManager();
    predicateManager = new PredicatePrecisionManager(fmgr, predCpa.getPredicateManager());
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    stats = new PDRStatistics();
    assignmentToPathAllocator =
        new AssignmentToPathAllocator(config, shutdownNotifier, pLogger, cfa.getMachineModel());

    // re-initialized in run()
    frameSet = new DynamicFrameSet(cfa.getMainFunction(), fmgr, backwardTransition);
    pdrSolver = new PDRSat(frameSet, solver, fmgr, pfmgr, predicateManager);
  }

  /** Checks for trivial cases and 0-/1-step counterexamples. */
  private boolean checkBaseCases(
      CFANode pStartLocation, ImmutableSet<CFANode> pErrorLocations, ReachedSet pReachedSet)
      throws CPATransferException, CPAException, InterruptedException, SolverException {

    // For trivially-safe tasks, no further effort is required
    if (pErrorLocations.isEmpty()) {
      return false;
    }

    // Check for 0-step counterexample
    for (CFANode errorLocation : pErrorLocations) {
      if (pStartLocation.equals(errorLocation)) {
        logger.log(Level.INFO, "Found errorpath: 0-step counterexample.");
        return false;
      }
    }

    // Check for 1-step counterexamples
    for (Block block : backwardTransition.getBlocksFrom(pErrorLocations, IS_MAIN_ENTRY)) {
      if (!solver.isUnsat(block.getFormula())) {
        logger.log(
            Level.INFO, "Found 1-step counterexample :", " \nTransition : \n", block.getFormula());
        analyzeCounterexample(Collections.singletonList(block), pReachedSet);
        return false;
      }
    }
    return true;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    CFANode startLocation = cfa.getMainFunction();
    ImmutableSet<CFANode> errorLocations =
        FluentIterable.from(pReachedSet).transform(AbstractStates.EXTRACT_LOCATION).toSet();
    pReachedSet.clear();

    try {
      if (!checkBaseCases(startLocation, errorLocations, pReachedSet)) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }

      frameSet = new DynamicFrameSet(startLocation, fmgr, backwardTransition);
      pdrSolver = new PDRSat(frameSet, solver, fmgr, pfmgr, predicateManager);

      /*
       * Main loop : Try to inductively strengthen highest frame set, propagate
       * states afterwards and check for termination.
       */
      while (!shutdownNotifier.shouldShutdown()) {
        frameSet.openNextFrameSet();
        logger.log(Level.INFO, "New frontier : " + frameSet.getMaxLevel());
        if (!strengthen(errorLocations, pReachedSet)) {
          logger.log(Level.INFO, "Found errorpath. Program has a bug.");
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        if (frameSet.propagate(pdrSolver, shutdownNotifier)) {
          logger.log(Level.INFO, "Program is safe.");
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        shutdownNotifier.shutdownIfNecessary();
      }
    } catch (SolverException e) {
      throw new CPAException("Solver error.", e);
    }

    // Shouldn't really reach this code
    throw new AssertionError("Could neither prove nor disprove safety of program.");
  }

  /**
   * Tries to prove that an error location cannot be reached with a number of steps less or equal to
   * 1 + {@link FrameSet#getMaxLevel()}.
   */
  private boolean strengthen(ImmutableSet<CFANode> pErrorLocations, ReachedSet pReachedSet)
      throws InterruptedException, SolverException, CPAEnabledAnalysisPropertyViolationException,
          CPAException {

    for (CFANode errorLoc : pErrorLocations) {
      for (Block block : backwardTransition.getBlocksFrom(errorLoc)) {
        CFANode errorPredecessorLoc = block.getPredecessorLocation();

        // Ask for state with transition to errorLoc (Counterexample To Inductiveness)
        Optional<BooleanFormula> cti = pdrSolver.getCTI(block);

        // Recursively block all discovered CTIs
        while (cti.isPresent()) {
          BooleanFormula badState = cti.get();

          if (!backwardblock(errorPredecessorLoc, badState, pReachedSet, errorLoc)) {
            return false;
          }
          cti = pdrSolver.getCTI(block); // Ask for next CTI
          shutdownNotifier.shutdownIfNecessary();
        }
      }
    }
    return true;
  }

  private boolean backwardblock(
      CFANode pErrorPredLocation, BooleanFormula pState, ReachedSet pReachedSet, CFANode pErrorLoc)
      throws SolverException, InterruptedException, CPAEnabledAnalysisPropertyViolationException,
          CPAException {

    PriorityQueue<ProofObligation> proofObligationQueue = new PriorityQueue<>();
    proofObligationQueue.offer(
        new ProofObligation(frameSet.getMaxLevel(), pErrorPredLocation, pState));

    // Inner loop : recursively block bad states.
    while (!proofObligationQueue.isEmpty()) {
      ProofObligation p =
          proofObligationQueue.poll(); // Inspect proof obligation with lowest frame level.
      logger.log(Level.INFO, "Current obligation : ", p);

      // Frame level 0 => program start location reached => counterexample found
      if (p.getFrameLevel() == 0) {
        analyzeCounterexample(p, pReachedSet, pErrorLoc);
        return false;
      }

      /*
       *  Checks if not(p.state) is relative inductive to states known in predecessor locations.
       *  Yes -> add negation of provided blockable states to frames.
       *  No -> recur on provided predecessor.
       */
      for (Block block : backwardTransition.getBlocksFrom(p.getLocation())) {
        ConsecutionResult result =
            pdrSolver.consecution(p.getFrameLevel() - 1, block, p.getState());

        if (result.consecutionSuccess()) {
          BooleanFormula blockableStates = result.getResultFormula();
          logger.log(
              Level.INFO,
              "Blocking states : ",
              blockableStates,
              ", at location : ",
              p.getLocation(),
              ", at level : ",
              p.getFrameLevel(),
              ", via location : ",
              block.getPredecessorLocation());
          frameSet.blockStates(blockableStates, p.getFrameLevel(), p.getLocation());
        } else {
          BooleanFormula predecessorState = result.getResultFormula();
          CFANode predLocation = block.getPredecessorLocation();
          logger.log(Level.INFO, "Found predecessor via ", predLocation);

          ProofObligation blockPredecessorState =
              new ProofObligation(p.getFrameLevel() - 1, predLocation, predecessorState, p);
          proofObligationQueue.offer(blockPredecessorState);
          proofObligationQueue.offer(p);
          logger.log(Level.INFO, "Queue : ", proofObligationQueue);
        }
      }
    }
    return true;
  }

  //  /**
  //   * Checks if, for every location, any two neighboring frame sets are equal.
  //   * This signals termination of the algorithm and correctness of the checked program.
  //   */
  //  private boolean isFrameSetConvergent() {
  //    for (int currentLevel = 1; currentLevel <= frameSet.getMaxLevel(); ++currentLevel) {
  //      Map<CFANode, Set<BooleanFormula>> statesAtCurrentLevel =
  //          frameSet.getStatesForAllLocations(currentLevel);
  //      Map<CFANode, Set<BooleanFormula>> statesAtNextLevel =
  //          frameSet.getStatesForAllLocations(currentLevel + 1);
  //      if (statesAtCurrentLevel.equals(statesAtNextLevel)) {
  //        return true;
  //      }
  //    }
  //    return false;
  //  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  /**
   * Analyzes the counterexample trace represented by the given proof
   * obligation, which is the start of a chain of obligations whose respective
   * predecessors lead to the target location.
   *
   * During the analysis, it populates the given reached set with the states
   * along the error trace.
   *
   * @param pFinalFailingObligation the proof obligation failing at the start location.
   * @param pTargetReachedSet the reached set to copy the states towards the
   * error state into.
   *
   * @throws InterruptedException if the analysis of the counterexample is
   * interrupted.
   * @throws CPAException if an exception occurs during the analysis of the
   * counterexample.
   */
  private void analyzeCounterexample(
      ProofObligation pFinalFailingObligation, ReachedSet pTargetReachedSet, CFANode pErrorLocation)
      throws CPAException, InterruptedException {

    // Reconstruct error trace from start location to direct error predecessor.
    List<Block> blocks = Lists.newArrayList();
    CFANode previousSuccessorLocation = pFinalFailingObligation.getLocation(); // Startloc
    ProofObligation currentObligation = pFinalFailingObligation;
    while (currentObligation.getCause().isPresent()) {
      currentObligation = currentObligation.getCause().get(); // level 1 at loc1
      CFANode predecessorLocation = previousSuccessorLocation; // loc0
      CFANode successorLocation = currentObligation.getLocation(); // loc1
      FluentIterable<Block> connectingBlocks = // getTo succ- filter those where pred = loc0
          backwardTransition
              .getBlocksFrom(successorLocation, false)
              .filter(Blocks.applyToPredecessorLocation(l -> l.equals(predecessorLocation)));
      blocks.add(Iterables.getOnlyElement(connectingBlocks));
      previousSuccessorLocation = successorLocation;
    }

    // Add block from direct error predecessor to error location to complete error trace.
    CFANode directErrorPredecessor = previousSuccessorLocation;
    FluentIterable<Block> blockToErrorLocation =
        backwardTransition
            .getBlocksFrom(pErrorLocation)
            .filter(Blocks.applyToPredecessorLocation(l -> l.equals(directErrorPredecessor)));
    blocks.add(Iterables.getOnlyElement(blockToErrorLocation));

    analyzeCounterexample(blocks, pTargetReachedSet);
  }

  /**
   * Analyzes the counterexample trace represented by the given list of blocks
   * from the program start to an error state and populates the given reached
   * set with the states along the error trace.
   *
   * @param pBlocks the blocks from the program start to the error state.
   * @param pTargetReachedSet the reached set to copy the states towards the
   * error state into.
   *
   * @throws InterruptedException if the analysis of the counterexample is
   * interrupted.
   * @throws CPATransferException if an exception occurs during the analysis of
   * the counterexample.
   */
  private void analyzeCounterexample(List<Block> pBlocks, ReachedSet pTargetReachedSet)
      throws CPATransferException, InterruptedException {

    stats.getErrorPathCreationTimer().start();

    logger.log(Level.INFO, "Error found, creating error path");
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      BooleanFormula blockFormulaConjunctions = Blocks.conjoinBlockFormulas(pBlocks, fmgr);
      prover.push(blockFormulaConjunctions);

      List<ValueAssignment> model;
      try {

        boolean satisfiable = !prover.isUnsat();
        if (!satisfiable) {
          // should not occur
          logger.log(
              Level.WARNING,
              "Counterexample export failed because the counterexample is spurious!");
          return;
        }

        // get the branchingFormula
        // this formula contains predicates for all branches we took
        // this way we can figure out which branches make a feasible path
        BooleanFormula branchingFormula = Blocks.conjoinBranchingFormulas(pBlocks, fmgr, pfmgr);

        prover.push(branchingFormula);
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

      Blocks.combineReachedSets(pBlocks, pTargetReachedSet);
      ARGPath targetPath =
          ARGUtils.getPathFromBranchingInformation(
              AbstractStates.extractStateByType(pTargetReachedSet.getFirstState(), ARGState.class),
              FluentIterable.from(pTargetReachedSet)
                  .transform(AbstractStates.toState(ARGState.class))
                  .filter(argState -> !argState.isDestroyed())
                  .toSet(),
              branchingInformation);

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
  }
}
