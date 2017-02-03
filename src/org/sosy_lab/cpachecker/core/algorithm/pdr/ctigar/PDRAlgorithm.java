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
package org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.MoreFiles.DeleteOnCloseFile;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar.PDRSmt.ConsecutionResult;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Blocks;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.ForwardTransition;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Property-Directed Reachability algorithm, also known as IC3. It can be used to check whether a
 * program is safe or not.
 */
public class PDRAlgorithm implements Algorithm, StatisticsProvider {

  private final CFA cfa;
  private final Solver solver;
  private final PredicateCPA predCPA;
  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfmgr;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ForwardTransition stepwiseTransition;
  private final Algorithm algorithm;
  private final Configuration config;
  private final PDROptions optionsCollection;
  private final StatisticsDelegator compositeStats;
  private final Specification specification;

  // Those are null until initialized in run()
  private @Nullable PDRStatistics stats;
  private @Nullable TransitionSystem transition;
  private @Nullable PredicatePrecisionManager predicateManager;
  private @Nullable FrameSet frameSet;
  private @Nullable PDRSmt pdrSolver;

  /**
   * Creates a new PDRAlgorithm instance.
   *
   * @param pReachedSetFactory Used for creating temporary reached sets for backwards analysis.
   * @param pCPA The composite CPA that contains all needed CPAs.
   * @param pAlgorithm The algorithm used for traversing the cfa.
   * @param pCFA The control flow automaton of the program.
   * @param pConfig The configuration that contains the components and options for this algorithm.
   * @param pLogger The logging component.
   * @param pShutdownNotifier The component that is used to shutdown this algorithm if necessary.
   * @param pSpecification The specification of the verification task.
   * @throws InvalidConfigurationException If the configuration file is invalid or incomplete.
   */
  public PDRAlgorithm(
      ReachedSetFactory pReachedSetFactory,
      ConfigurableProgramAnalysis pCPA,
      Algorithm pAlgorithm,
      CFA pCFA,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification)
      throws InvalidConfigurationException {

    cfa = Objects.requireNonNull(pCFA);
    algorithm = Objects.requireNonNull(pAlgorithm);
    config = Objects.requireNonNull(pConfig);
    optionsCollection = new PDROptions(config);

    predCPA = CPAs.retrieveCPA(Objects.requireNonNull(pCPA), PredicateCPA.class);
    if (predCPA == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for PDRAlgorithm");
    }
    solver = predCPA.getSolver();
    fmgr = solver.getFormulaManager();
    pfmgr = predCPA.getPathFormulaManager();
    shutdownNotifier = Objects.requireNonNull(pShutdownNotifier);
    logger = Objects.requireNonNull(pLogger);
    compositeStats = new StatisticsDelegator("PDR related");
    stats = new PDRStatistics();
    compositeStats.register(stats);
    stepwiseTransition =
        new ForwardTransition(Objects.requireNonNull(pReachedSetFactory), pCPA, pAlgorithm, cfa);
    specification = Objects.requireNonNull(pSpecification);

    // initialized in run()
    transition = null;
    predicateManager = null;
    frameSet = null;
    pdrSolver = null;
  }

  /**
   * Checks if any target location can be directly reached from the given CFANode in 0 or 1 step.
   * One step is defined by the transition encoding of the stepwise transition.
   */
  private boolean checkBaseCases(CFANode pMainEntry, ReachedSet pReachedSet)
      throws SolverException, InterruptedException, CPAException {

    Set<CFANode> errorLocations = transition.getTargetLocations();

    // For trivially safe programs.
    if (errorLocations.isEmpty()) {
      logger.log(Level.INFO, "No target locations found. Program is trivially safe.");
      return true;
    }

    // Check for 0-step counterexample.
    if (errorLocations.contains(pMainEntry)) {
      logger.log(Level.INFO, "Found errorpath: Starting location is a target location.");
      return false; //TODO cex
    }

    // Check for 1-step counterexample: Is there a satisfiable block transition from start location
    // to any error location.
    for (Block blockToError :
        stepwiseTransition
            .getBlocksFrom(pMainEntry)
            .filter(b -> errorLocations.contains(b.getSuccessorLocation()))) {
      if (!solver.isUnsat(blockToError.getFormula())) {
        logger.log(Level.INFO, "Found errorpath: 1-step counterexample.");
        analyzeCounterexample(Collections.singletonList(blockToError), pReachedSet);
        return false;
      }
    }

    return true;
  }

  /** Resets everything that needs to be fresh for each new run. */
  private void prepareComponentsForNewRun() {
    compositeStats.unregisterAll();
    stats = new PDRStatistics();
    compositeStats.register(stats);
    frameSet = new DeltaEncodedFrameSet(solver, fmgr, transition, compositeStats);
    predicateManager =
        new PredicatePrecisionManager(
            fmgr,
            predCPA.getPredicateManager(),
            pfmgr,
            transition,
            cfa,
            compositeStats,
            solver,
            optionsCollection);
    pdrSolver =
        new PDRSmt(
            frameSet,
            solver,
            fmgr,
            pfmgr,
            predicateManager,
            transition,
            compositeStats,
            logger,
            stepwiseTransition,
            optionsCollection);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    CFANode mainEntry =
        FluentIterable.from(pReachedSet).transform(AbstractStates.EXTRACT_LOCATION).first().get();
    pReachedSet.clear();

    // Only need to create this at first run.
    if (transition == null) {
      try {
        transition = new TransitionSystem(cfa, stepwiseTransition, fmgr, pfmgr, mainEntry);
        logger.log(Level.INFO, transition);
      } catch (SolverException e) {
        logger.logException(Level.WARNING, e, null);
        throw new CPAException("Solver error occured while creating transition relation.", e);
      }
    }

    try {
      if (!checkBaseCases(mainEntry, pReachedSet)) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
      prepareComponentsForNewRun();

      /*
       * Main loop : Try to inductively strengthen highest frame set, propagate
       * states afterwards and check for termination.
       */
      while (!shutdownNotifier.shouldShutdown()) {
        frameSet.openNextFrame();
        logger.log(Level.INFO, "New frontier : ", frameSet.getMaxLevel());
        if (!strengthen(pReachedSet)) {
          logger.log(Level.INFO, "Found errorpath. Program has a bug.");
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        if (frameSet.propagate(shutdownNotifier)) {
          logger.log(Level.INFO, "Program is safe.");
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        shutdownNotifier.shutdownIfNecessary();
      }
    } catch (SolverException e) {
      logger.logException(Level.WARNING, e, null);
      throw new CPAException("Solver error.", e);
    }

    throw new AssertionError("Could neither prove nor disprove safety of program.");
  }

  /**
   * Tries to prove that an error location cannot be reached with a number of steps less or equal to
   * 1 + {@link FrameSet#getMaxLevel()}. Any state that can reach an error location in that amount
   * of steps will be proved unreachable. If this isn't possible, a counterexample trace is created.
   *
   * @return True, if all states able to reach an error location in 1 + {@link
   *     FrameSet#getMaxLevel()} steps could be blocked. False is a counterexample is found.
   */
  private boolean strengthen(ReachedSet pReached)
      throws InterruptedException, SolverException, CPAEnabledAnalysisPropertyViolationException,
          CPAException {

    // Ask for states with direct transition to any error location (Counterexample To Inductiveness)
    Optional<ConsecutionResult> cti = pdrSolver.getCTI();

    // Recursively block all discovered CTIs
    while (cti.isPresent()) {
      StatesWithLocation badStates = cti.get().getResult();
      CFANode reachedErrorLocation = getErrorLocationSuccessor(badStates);

      if (!backwardblock(badStates, reachedErrorLocation, pReached)) {
        return false;
      }
      cti = pdrSolver.getCTI(); // Ask for next CTI
      shutdownNotifier.shutdownIfNecessary();
    }
    return true;
  }

  /**
   * Assuming that the given state can reach an error location in exactly one step, computes the
   * concrete one it can transition to.
   */
  private CFANode getErrorLocationSuccessor(StatesWithLocation pState)
      throws CPAException, InterruptedException, SolverException {
    Set<CFANode> errorLocs = transition.getTargetLocations();
    FluentIterable<Block> oneStepReachableErrorLocations =
        stepwiseTransition
            .getBlocksFrom(pState.getLocation())
            .filter(b -> errorLocs.contains(b.getSuccessorLocation()));
    assert !oneStepReachableErrorLocations.isEmpty();

    // If there is only one 1-step reachable error location for pState,
    // just return that one.
    if (oneStepReachableErrorLocations.size() == 1) {
      return oneStepReachableErrorLocations.first().get().getSuccessorLocation();
    }

    for (Block b : oneStepReachableErrorLocations) {
      BooleanFormula transitionForBlock =
          fmgr.getBooleanFormulaManager().and(pState.getConcrete(), b.getFormula());
      if (!solver.isUnsat(transitionForBlock)) {
        return b.getSuccessorLocation();
      }
    }
    throw new AssertionError("States can't transition to a target location in one step.");
  }

  /**
   * Tries to prove by induction relative to the most general frame, that the given states are
   * unreachable. If predecessors are found that contradict this unreachability, they are
   * recursively handled in the same fashion, but at one level lower than their successors. <br>
   * This continues until the original states could be blocked, or a predecessor, that is an initial
   * state, is found. In this situation, a counterexample is created.
   *
   * @param pStatesToBlock The states that should be blocked at the highest level.
   * @return True, if the states could be blocked. False, if a counterexample is found.
   */
  private boolean backwardblock(
      StatesWithLocation pStatesToBlock, CFANode pErrorLocation, ReachedSet pReached)
      throws SolverException, InterruptedException, CPAEnabledAnalysisPropertyViolationException,
          CPAException {

    PriorityQueue<ProofObligation> proofObligationQueue = new PriorityQueue<>();
    proofObligationQueue.offer(new ProofObligation(frameSet.getMaxLevel(), pStatesToBlock));

    // Inner loop : recursively block bad states.
    while (!proofObligationQueue.isEmpty()) {
      logger.log(Level.INFO, "Queue : ", proofObligationQueue);
      ProofObligation p =
          proofObligationQueue.poll(); // Inspect proof obligation with lowest frame level.
      logger.log(Level.INFO, "Current obligation : ", p);

      // Frame level 0 => counterexample found
      if (p.getFrameLevel() == 0) {
        assert pdrSolver.isInitial(p.getState().getFormula());
        analyzeCounterexample(p, pReached, pErrorLocation);
        return false;
      }

      ConsecutionResult result = pdrSolver.consecution(p.getFrameLevel() - 1, p.getState());

      if (result.consecutionSuccess()) {
        BooleanFormula blockableStates = result.getResult().getFormula();
        logger.log(Level.INFO, "Blocking states : ", blockableStates);
        frameSet.blockStates(blockableStates, p.getFrameLevel());

        if (p.getFrameLevel() < frameSet.getMaxLevel()) {
          proofObligationQueue.offer(p.rescheduleToNextLevel());
        }
      } else {
        StatesWithLocation predecessorStates = result.getResult();
        logger.log(Level.INFO, "Found predecessor : ", predecessorStates.getFormula());
        ProofObligation blockPredecessorStates =
            new ProofObligation(p.getFrameLevel() - 1, predecessorStates, p);
        proofObligationQueue.offer(blockPredecessorStates);
        proofObligationQueue.offer(p);
      }
    }
    return true;
  }

  /** TODO This method will be removed at a later point. It is only used temporarily. */
  @SuppressWarnings("unused")
  private boolean isFrameSetConvergent() {
    for (int currentLevel = 1; currentLevel <= frameSet.getMaxLevel(); ++currentLevel) {
      Set<BooleanFormula> statesAtCurrentLevel = frameSet.getStates(currentLevel);
      Set<BooleanFormula> statesAtNextLevel = frameSet.getStates(currentLevel + 1);
      if (statesAtCurrentLevel.equals(statesAtNextLevel)) {
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
    pStatsCollection.add(compositeStats);
  }

  /**
   * Analyzes the counterexample trace represented by the given proof obligation, which is the start
   * of a chain of obligations whose respective predecessors lead to the target location.
   *
   * <p>During the analysis, it populates the given reached set with the states along the error
   * trace.
   *
   * @param pFinalFailingObligation the proof obligation failing at the start location.
   * @param pTargetReachedSet the reached set to copy the states towards the error state into.
   * @throws InterruptedException if the analysis of the counterexample is interrupted.
   * @throws CPAException if an exception occurs during the analysis of the counterexample.
   */
  private void analyzeCounterexample(
      ProofObligation pFinalFailingObligation, ReachedSet pTargetReachedSet, CFANode pErrorLocation)
      throws CPAException, InterruptedException {

    // Reconstruct error trace from start location to direct error predecessor.
    List<Block> blocks = Lists.newArrayList();
    CFANode previousPredecessorLocation = pFinalFailingObligation.getState().getLocation();
    ProofObligation currentObligation = pFinalFailingObligation;
    while (currentObligation.getCause().isPresent()) {
      currentObligation = currentObligation.getCause().get();
      CFANode predecessorLocation = previousPredecessorLocation;
      CFANode successorLocation = currentObligation.getState().getLocation();
      FluentIterable<Block> connectingBlocks =
          stepwiseTransition
              .getBlocksFrom(predecessorLocation)
              .filter(Blocks.applyToSuccessorLocation(l -> l.equals(successorLocation)));
      blocks.add(Iterables.getOnlyElement(connectingBlocks));
      previousPredecessorLocation = successorLocation;
    }

    // Add block from direct error predecessor to error location to complete error trace.
    CFANode directErrorPredecessor = previousPredecessorLocation;
    blocks.add(getBlockToErrorLocation(directErrorPredecessor, pErrorLocation));

    analyzeCounterexample(blocks, pTargetReachedSet);
  }

  // Temporal method to deal with occurrence of multiple blocks between an error predecessor location
  // and its corresponding error location successor.
  // Finds the correct one, i.e. the  one whose formula is the disjunction of the others.
  private Block getBlockToErrorLocation(CFANode pErrorPred, CFANode pErrorLoc)
      throws CPAException, InterruptedException {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    List<Block> blocksBetweenPredAndError =
        stepwiseTransition
            .getBlocksFrom(pErrorPred)
            .filter(Blocks.applyToSuccessorLocation(l -> l.equals(pErrorLoc)))
            .toList();

    // Just get the block with the most disjunction args.
    return blocksBetweenPredAndError
        .stream()
        .max(
            new java.util.Comparator<Block>() {

              @Override
              public int compare(Block pArg0, Block pArg1) {
                Set<BooleanFormula> d0 = bfmgr.toDisjunctionArgs(pArg0.getFormula(), true);
                Set<BooleanFormula> d1 = bfmgr.toDisjunctionArgs(pArg1.getFormula(), true);
                return d0.size() - d1.size();
              }
            })
        .get();
  }

  /**
   * Analyzes the counterexample trace represented by the given list of blocks from the program
   * start to an error state and populates the given reached set with the states along the error
   * trace.
   *
   * @param pBlocks the blocks from the program start to the error state.
   * @param pTargetReachedSet the reached set to copy the states towards the error state into.
   * @throws InterruptedException if the analysis of the counterexample is interrupted.
   * @throws CPATransferException if an exception occurs during the analysis of the counterexample.
   */
  private void analyzeCounterexample(List<Block> pBlocks, ReachedSet pTargetReachedSet)
      throws CPAException, InterruptedException {

    stats.errorPathCreation.start();

    logger.log(Level.INFO, "Error found, creating error path");

    List<ARGPath> paths = Lists.newArrayListWithCapacity(pBlocks.size());
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      for (Block block : pBlocks) {

        List<ValueAssignment> model;
        BooleanFormula pathFormula = block.getFormula();
        boolean branchingFormulaPushed = false;
        try {
          prover.push(pathFormula);
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
          BooleanFormula branchingFormula =
              pfmgr.buildBranchingFormula(
                  AbstractStates.projectToType(block.getReachedSet(), ARGState.class).toSet());

          prover.push(branchingFormula);
          branchingFormulaPushed = true;
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
          if (branchingFormulaPushed) {
            prover.pop(); // remove branching formula
          }
          prover.pop(); // remove path formula
        }

        // get precise error path
        Map<Integer, Boolean> branchingInformation =
            pfmgr.getBranchingPredicateValuesFromModel(model);

        boolean isLastPart = paths.size() == pBlocks.size() - 1;
        ARGPath targetPath =
            ARGUtils.getPathFromBranchingInformation(
                AbstractStates.extractStateByType(block.getPredecessor(), ARGState.class),
                FluentIterable.from(block.getReachedSet()).toSet(),
                branchingInformation,
                isLastPart);
        paths.add(targetPath);
      }
    }

    // This temp file will be automatically deleted when the try block terminates.
    try (DeleteOnCloseFile automatonFile =
        MoreFiles.createTempFile("counterexample-automaton", ".txt")) {
      try (Writer w =
          MoreFiles.openOutputFile(automatonFile.toPath(), Charset.defaultCharset()); ) {
        ARGUtils.producePathAutomaton(w, paths, "ReplayAutomaton", null);
      }

      Specification lSpecification =
          Specification.fromFiles(
              specification.getProperties(),
              ImmutableList.of(automatonFile.toPath()),
              cfa,
              config,
              logger);
      CoreComponentsFactory factory =
          new CoreComponentsFactory(config, logger, shutdownNotifier, new AggregatedReachedSets());
      ConfigurableProgramAnalysis lCpas = factory.createCPA(cfa, lSpecification);
      Algorithm lAlgorithm = CPAAlgorithm.create(lCpas, logger, config, shutdownNotifier);
      pTargetReachedSet.add(
          lCpas.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()),
          lCpas.getInitialPrecision(
              cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()));

      lAlgorithm.run(pTargetReachedSet);
    } catch (IOException e) {
      throw new CPAException("Could not reply error path", e);
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Invalid configuration in replay config: " + e.getMessage(), e);
    } finally {
      stats.errorPathCreation.stop();
    }

  }

  private static class PDRStatistics implements Statistics {

    private final Timer errorPathCreation = new Timer();

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      if (errorPathCreation.getNumberOfIntervals() > 0) {
        pOut.println("Time for error path creation:        " + errorPathCreation);
      }
    }

    @Override
    public @Nullable String getName() {
      return "PDR algorithm";
    }
  }
}
