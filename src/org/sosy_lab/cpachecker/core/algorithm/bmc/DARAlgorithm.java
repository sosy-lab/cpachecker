// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class provides implementation of dual approximated reachability model checking algorithm
 * (DAR) adapted for program verification.
 *
 * <p>The original DAR algorithm was proposed in the paper "Intertwined Forward-Backward
 * Reachability Analysis Using Interpolants" from Y. Vizel, O. Grumberg and S. Shoham. The algorithm
 * computes two interpolant sequences - Forward (FRS) and Backward (BRS). FRS is initialized with
 * initial states formula and BRS with formula describing states that violate specification. The
 * idea is that FRS overapproximates reachability vector of states reachable from initial states, on
 * the other hand BRS overapproximates states that can reach violating states. In each iteration the
 * algorithm performs two phases - Local and Global strengthening. Let FRS = F0,F1,F2...,Fn and BRS
 * = B0,B1,B2...,Bn, the Local strengthening phase checks if Fi ∧ TR ∧ Bj is unsatisfiable, if yes,
 * then there is no counterexample of length n+1. In such case, it propagates the "reason of
 * unsatisfiability" via interpolants up to Fn+1, Bn+1 and proceeds into another iteration. If no
 * such (i,j) is found, it switches to Global strengthening phase. It performs BMC and iteratively
 * unrolls formula INIT ∧ TR ∧ ... ∧ TR ∧ Bn-i to check for satisfiability. If some formula is
 * unsatisfiable, it creates interpolation sequence and strengthens F0,...,Fi. If all of the
 * formulas are satisfiable, BMC finds a counterexample.
 *
 * <p>
 */
@Options(prefix = "dar")
public class DARAlgorithm extends AbstractBMCAlgorithm implements Algorithm {

  private enum StrengtheningStatus {
    FAILED,
    SUCCEEDED,
    SUCCEEDED_AND_COMPLETE
  }

  @Option(
      secure = true,
      description = "toggle falling back if interpolation or forward-condition is disabled")
  private boolean fallBack = true;

  @Option(secure = true, description = "toggle removing unreachable stop states in ARG")
  private boolean removeUnreachableStopStates = false;

  @Option(secure = true, description = "toggle replace global phase with BMC")
  private boolean replaceGlobalPhaseWithBMC = false;

  @Option(secure = true, description = "toggle checking forward conditions")
  private boolean checkForwardConditions = true;

  private boolean isInterpolationEnabled = true;

  private final ConfigurableProgramAnalysis cpa;

  private final Algorithm algorithm;

  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;
  private final InterpolationManager itpMgr;
  private final CFA cfa;

  public DARAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      ShutdownManager pShutdownManager,
      CFA pCFA,
      final Specification specification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(
        pAlgorithm,
        pCPA,
        pConfig,
        pLogger,
        pReachedSetFactory,
        pShutdownManager,
        pCFA,
        specification,
        new BMCStatistics(),
        false /* no invariant generator */,
        pAggregatedReachedSets);
    pConfig.inject(this);

    cpa = pCPA;
    cfa = pCFA;
    algorithm = pAlgorithm;

    @SuppressWarnings("resource")
    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, DARAlgorithm.class);
    solver = predCpa.getSolver();
    pfmgr = predCpa.getPathFormulaManager();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    itpMgr =
        new InterpolationManager(
            pfmgr, solver, Optional.empty(), Optional.empty(), pConfig, shutdownNotifier, logger);

    stats.numOfInterpolationCalls = 0;
    stats.numOfInterpolants = 0;
    stats.numOfDARGlobalPhases = 0;
    stats.numOfDARLocalPhases = 0;
    stats.numOfDARLocalInterpolants = 0;
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    try {
      return dualApproximatedReachabilityModelChecking(pReachedSet);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      invariantGenerator.cancel();
    }
  }

  /**
   * The main method for dual approximated reachability model checking.
   *
   * @param pReachedSet Abstract Reachability Graph (ARG)
   * @return {@code AlgorithmStatus.UNSOUND_AND_PRECISE} if an error location is reached, i.e.,
   *     unsafe; {@code AlgorithmStatus.SOUND_AND_PRECISE} if a fixed point is derived, i.e., safe.
   */
  private AlgorithmStatus dualApproximatedReachabilityModelChecking(ReachedSet pReachedSet)
      throws CPAException, SolverException, InterruptedException {
    if (getTargetLocations().isEmpty()) {
      pReachedSet.clearWaitlist();
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }
    adjustConfigsAccordingToCFA();

    logger.log(Level.FINE, "Performing dual approximated reachability model checking");
    PartitionedFormulas partitionedFormulas =
        PartitionedFormulas.createForwardPartitionedFormulas(bfmgr, logger, false);

    // Unroll CFA two times if possible, so we obtain all necessary formulas, i.e. INIT, TR and ~P.
    // If Interpolation is not enabled, it will just perform BMC.
    while (!isInterpolationEnabled || getCurrentMaxLoopIterations() <= 2) {
      shutdownNotifier.shutdownIfNecessary();
      unrollProgram(pReachedSet);
      if (isInterpolationEnabled && getCurrentMaxLoopIterations() > 1) {
        stats.interpolationPreparation.start();
        partitionedFormulas.collectFormulasFromARG(pReachedSet);
        stats.interpolationPreparation.stop();
      }
      if (findCexByBMC(pReachedSet) || !adjustConditions()) {
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
      // Check if interpolation or forward-condition check is applicable
      adjustConfigsAccordingToARG(pReachedSet);
      // Forward-condition check
      if (checkForwardConditions && !isFurtherUnrollingPossible(pReachedSet)) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
      InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
    }

    DualReachabilitySequence dualSequence = new DualReachabilitySequence(partitionedFormulas);
    // DAR, from the second iteration, when all formulas are collected
    while (!checkFixedPoint(dualSequence)) {
      shutdownNotifier.shutdownIfNecessary();
      if (performLocalStrengthening(dualSequence, partitionedFormulas)
          == StrengtheningStatus.FAILED) {
        switch (performGlobalStrengthening(partitionedFormulas, dualSequence, pReachedSet)) {
          case SUCCEEDED_AND_COMPLETE:
            return AlgorithmStatus.SOUND_AND_PRECISE;
          case FAILED:
            return AlgorithmStatus.UNSOUND_AND_PRECISE;
          default:
        }
      }
    }
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private boolean checkFixedPoint(DualReachabilitySequence pDualSequence)
      throws InterruptedException, SolverException {
    if (pDualSequence.getSize() < 1) {
      return false;
    }
    BooleanFormula forwardImage = pDualSequence.getForwardImageAt(0);
    BooleanFormula backwardImage = pDualSequence.getBackwardImageAt(0);

    for (int i = 1; i < pDualSequence.getSize(); i++) {
      BooleanFormula forwardFormula = pDualSequence.getForwardImageAt(i);
      BooleanFormula backwardFormula = pDualSequence.getBackwardImageAt(i);
      if ((solver.implies(forwardFormula, forwardImage)
              || solver.implies(backwardFormula, backwardImage))
          // The set that is represented by formula B_0 should not be empty.
          // If it is, then the algorithm catches it sooner.
          && !pDualSequence.getBackwardImageAt(0).equals(bfmgr.makeFalse())) {
        stats.fixedPointConvergenceLength = pDualSequence.getSize();
        logger.log(Level.INFO, "Fixed point reached");
        return true;
      }
      forwardImage = bfmgr.or(forwardFormula, forwardImage);
      backwardImage = bfmgr.or(backwardFormula, backwardImage);
    }
    return false;
  }

  /**
   * Checks local safety of the sequences. Further, it extends them by new overapproximating
   * formulas.
   *
   * @return {@link StrengtheningStatus#SUCCEEDED} if local phase confirmed the program is safe at
   *     bound n; {@link StrengtheningStatus#FAILED} otherwise.
   */
  private StrengtheningStatus performLocalStrengthening(
      DualReachabilitySequence pDualSequence, PartitionedFormulas pPartitionedFormulas)
      throws CPAException, SolverException, InterruptedException {
    stats.numOfDARLocalPhases += 1;
    final int indexOfLocalContradiction =
        findIndexOfUnsatisfiableLocalCheck(pDualSequence, pPartitionedFormulas);
    if (indexOfLocalContradiction == -1) {
      // No local strengthening point was found, switch to Global phase
      return StrengtheningStatus.FAILED;
    } else {
      // Local strengthening point was found, propagate the reason for contradiction to the end
      // of sequences.
      iterativeLocalStrengthening(pDualSequence, pPartitionedFormulas, indexOfLocalContradiction);
    }
    return StrengtheningStatus.SUCCEEDED;
  }

  /**
   * Checks global safety of the sequences. Further, it extends them by new overapproximating
   * formulas.
   *
   * @return {@link StrengtheningStatus#FAILED} if a counterexample is found or {@link
   *     AbstractBMCAlgorithm#adjustConditions} failed; {@link
   *     StrengtheningStatus#SUCCEEDED_AND_COMPLETE} if the program is safe and cannot be further
   *     unrolled; {@link StrengtheningStatus#SUCCEEDED} otherwise, i.e. the program is safe at
   *     bound n.
   */
  private StrengtheningStatus performGlobalStrengthening(
      PartitionedFormulas pFormulas, DualReachabilitySequence pDualSequence, ReachedSet pReachedSet)
      throws CPAException, InterruptedException, SolverException {
    // Global phase of DAR
    stats.numOfDARGlobalPhases += 1;
    ///
    Preconditions.checkArgument(pFormulas.getNumLoops() < pDualSequence.getSize());
    int globalUnsatIndex = 1;
    for (final int n = pDualSequence.getSize(); globalUnsatIndex <= n; ++globalUnsatIndex) {
      // Unrolling CFA if necessary
      if (pFormulas.getNumLoops() < globalUnsatIndex) {
        unrollProgram(pReachedSet);
        // BMC check for B_0
        if (globalUnsatIndex == n && findCexByBMC(pReachedSet)) {
          return StrengtheningStatus.FAILED;
        }
        // Check if interpolation or forward-condition check is applicable
        adjustConfigsAccordingToARG(pReachedSet);
        // Forward-condition check
        if (checkForwardConditions && !isFurtherUnrollingPossible(pReachedSet)) {
          return StrengtheningStatus.SUCCEEDED_AND_COMPLETE;
        }
        if (!adjustConditions()) {
          return StrengtheningStatus.FAILED;
        }
        stats.interpolationPreparation.start();
        pFormulas.collectFormulasFromARG(pReachedSet);
        stats.interpolationPreparation.stop();
        InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
        assert pFormulas.getNumLoops() == globalUnsatIndex;
      }
      if (globalUnsatIndex == n) {
        assert isGlobalQueryUnsat(pDualSequence, globalUnsatIndex, pFormulas);
        break;
      }
      if (!replaceGlobalPhaseWithBMC
          && isGlobalQueryUnsat(pDualSequence, globalUnsatIndex, pFormulas)) {
        break;
      }
    }
    assert globalUnsatIndex <= pDualSequence.getSize();
    if (!isInterpolationEnabled) {
      throw new CPAException("Interpolation disabled. Cannot continue.");
    }
    List<BooleanFormula> itpSequence =
        getInterpolationSequence(pFormulas, pDualSequence, globalUnsatIndex);
    strengthenForwardVectorWithInterpolants(pDualSequence, itpSequence, pFormulas);
    iterativeLocalStrengthening(pDualSequence, pFormulas, itpSequence.size() - 1);
    return StrengtheningStatus.SUCCEEDED;
  }

  /**
   * Strengthens the forward and backward sequences from the point of contradiction to propagate it
   * to (Fn, B0) and (F0, Bn). Further it extends the sequences.
   */
  private void iterativeLocalStrengthening(
      DualReachabilitySequence pDualSequence,
      PartitionedFormulas pPartitionedFormulas,
      int pIdxOfLocalContradiction)
      throws CPAException, InterruptedException {
    logger.log(Level.FINE, "Strengthening forward and backward reachability vectors locally");
    final int lastIdxOfSequences = pDualSequence.getSize() - 1;
    // Updating forward sequence
    for (int forwardIdx = pIdxOfLocalContradiction; forwardIdx < lastIdxOfSequences; ++forwardIdx) {
      BooleanFormula resultingForwardFormula = pDualSequence.getForwardImageAt(forwardIdx + 1);
      BooleanFormula interpolant =
          constructForwardInterpolant(pDualSequence, pPartitionedFormulas, forwardIdx);
      resultingForwardFormula = bfmgr.and(resultingForwardFormula, interpolant);
      pDualSequence.updateForwardReachVector(resultingForwardFormula, forwardIdx + 1);
    }
    // Updating backward sequence
    for (int backwardIdx = lastIdxOfSequences - pIdxOfLocalContradiction;
        backwardIdx < lastIdxOfSequences;
        ++backwardIdx) {
      BooleanFormula resultingBackwardFormula = pDualSequence.getBackwardImageAt(backwardIdx + 1);
      BooleanFormula interpolant =
          constructBackwardInterpolant(pDualSequence, pPartitionedFormulas, backwardIdx);
      resultingBackwardFormula = bfmgr.and(resultingBackwardFormula, interpolant);
      pDualSequence.updateBackwardReachVector(resultingBackwardFormula, backwardIdx + 1);
    }
    // Extend the reachability vectors
    BooleanFormula newForwardReachFormula =
        constructForwardInterpolant(pDualSequence, pPartitionedFormulas, lastIdxOfSequences);
    BooleanFormula newBackwardReachFormula =
        constructBackwardInterpolant(pDualSequence, pPartitionedFormulas, lastIdxOfSequences);
    pDualSequence.extendForwardReachVector(newForwardReachFormula);
    pDualSequence.extendBackwardReachVector(newBackwardReachFormula);
    logger.log(
        Level.ALL, "Updated forward reachability vector:", pDualSequence.getForwardReachVector());
    logger.log(
        Level.ALL, "Updated backward reachability vector:", pDualSequence.getBackwardReachVector());
  }

  /**
   * Creates forward interpolant (denoted as FI in the paper) of the contradictory formulas (F and
   * TR, B). Interpolant is instantiated with prefix SSA, so then it can be checked for
   * satisfiability with backward interpolant that has SSA indices of the first transition formula.
   */
  private BooleanFormula constructForwardInterpolant(
      DualReachabilitySequence pDualSequence, PartitionedFormulas pPartitionedFormulas, int pIndex)
      throws CPAException, InterruptedException {
    final int lastIndexOfSequences = pDualSequence.getSize() - 1;
    BooleanFormula transitionFormula = pPartitionedFormulas.getLoopFormulas().get(0);
    BooleanFormula forwardFormula = pDualSequence.getForwardImageAt(pIndex);
    BooleanFormula backwardFormula =
        pDualSequence.getBackwardImageAt(lastIndexOfSequences - pIndex);

    Optional<ImmutableList<BooleanFormula>> interpolants =
        itpMgr.interpolate(
            ImmutableList.of(bfmgr.and(forwardFormula, transitionFormula), backwardFormula));
    BooleanFormula interpolant = interpolants.orElseThrow().get(0);
    interpolant =
        fmgr.instantiate(fmgr.uninstantiate(interpolant), pPartitionedFormulas.getPrefixSsaMap());
    stats.numOfInterpolationCalls += 1;
    stats.numOfInterpolants += 1;
    stats.numOfDARLocalInterpolants += 1;
    InterpolationHelper.recordInterpolantStats(fmgr, interpolant, stats);
    return interpolant;
  }

  /**
   * Creates backward interpolant (denoted as BI in the paper) of the contradictory formulas (B and
   * TR, F) Interpolant is instantiated with SSA of first transition, so then it can be checked for
   * satisfiability with forward interpolant that has SSA indices of the prefix formula.
   */
  private BooleanFormula constructBackwardInterpolant(
      DualReachabilitySequence pDualSequence, PartitionedFormulas pPartitionedFormulas, int pIndex)
      throws CPAException, InterruptedException {
    final int lastIndexOfSequences = pDualSequence.getSize() - 1;
    BooleanFormula transitionFormula = pPartitionedFormulas.getLoopFormulas().get(0);
    BooleanFormula forwardFormula = pDualSequence.getForwardImageAt(lastIndexOfSequences - pIndex);
    BooleanFormula backwardFormula = pDualSequence.getBackwardImageAt(pIndex);
    SSAMap backwardSsa = pPartitionedFormulas.getLoopFormulaSsaMaps().get(0);

    Optional<ImmutableList<BooleanFormula>> interpolants =
        itpMgr.interpolate(
            ImmutableList.of(bfmgr.and(backwardFormula, transitionFormula), forwardFormula));
    BooleanFormula interpolant = interpolants.orElseThrow().get(0);
    interpolant = fmgr.instantiate(fmgr.uninstantiate(interpolant), backwardSsa);
    stats.numOfInterpolationCalls += 1;
    stats.numOfInterpolants += 1;
    stats.numOfDARLocalInterpolants += 1;
    InterpolationHelper.recordInterpolantStats(fmgr, interpolant, stats);
    return interpolant;
  }

  /**
   * Check if a target (error) state is reachable by BMC within current unrolling bound This method
   * is a slightly modified copy of {@link IMCAlgorithm#findCexByBMC}
   *
   * @return {@code true} if a counterexample is found, i.e., property is violated; {@code false}
   *     otherwise
   * @throws InterruptedException On shutdown request.
   */
  private boolean findCexByBMC(ReachedSet pReachedSet)
      throws InterruptedException, SolverException, CPAException {
    final boolean isTargetStateReachable;
    try (ProverEnvironment bmcProver = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      BooleanFormula targetFormula =
          InterpolationHelper.buildReachTargetStateFormula(bfmgr, pReachedSet);
      stats.satCheck.start();
      try {
        bmcProver.push(targetFormula);
        isTargetStateReachable = !bmcProver.isUnsat();
      } finally {
        stats.satCheck.stop();
      }
      if (isTargetStateReachable) {
        logger.log(Level.FINE, "A target state is reached by BMC");
        analyzeCounterexample(targetFormula, pReachedSet, bmcProver);
      }
    }
    return isTargetStateReachable;
  }

  /**
   * A method that looks for two consecutive formulas over-approximating sets of reachable states
   * from the dual sequence that does not have a transition between them. By that we can conclude
   * that no counterexample of length n+1 exists.
   */
  private int findIndexOfUnsatisfiableLocalCheck(
      DualReachabilitySequence pDualSequence, PartitionedFormulas pPartitionedFormulas)
      throws SolverException, InterruptedException {
    BooleanFormula transitionFormula = pPartitionedFormulas.getLoopFormulas().get(0);
    final int sequenceSize = pDualSequence.getSize();
    for (int i = sequenceSize - 1; i >= 0; i--) {
      stats.assertionsCheck.start();
      final boolean isNotReachableWithOneTransition;
      try {
        isNotReachableWithOneTransition =
            solver.isUnsat(
                bfmgr.and(
                    pDualSequence.getForwardImageAt(i),
                    transitionFormula,
                    pDualSequence.getBackwardImageAt(sequenceSize - i - 1)));
      } finally {
        stats.assertionsCheck.stop();
      }
      if (isNotReachableWithOneTransition) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Check the loop structure of the input program and adjust configurations accordingly. This
   * method is an exact copy of {@link IMCAlgorithm#adjustConfigsAccordingToCFA}
   */
  private void adjustConfigsAccordingToCFA() throws CPAException {
    if (!cfa.getAllLoopHeads().isPresent() || cfa.getAllLoopHeads().orElseThrow().size() > 1) {
      String reason =
          cfa.getAllLoopHeads().isPresent()
              ? "Multi-loop programs are not supported"
              : "Loop structure could not be determined";
      if (fallBack) {
        fallBackToBMC(reason);
      } else {
        throw new CPAException(reason);
      }
    }
  }

  /**
   * Determine if interpolation or forward-condition check is applicable with the current unrolled
   * ARG and adjust configurations accordingly. This method is an exact copy of {@link
   * IMCAlgorithm#adjustConfigsAccordingToARG}
   */
  private void adjustConfigsAccordingToARG(ReachedSet pReachedSet)
      throws CPAException, SolverException, InterruptedException {
    // Check if interpolation or forward-condition check is applicable
    if (isInterpolationEnabled
        && !InterpolationHelper.checkAndAdjustARG(
            logger, cpa, bfmgr, solver, pReachedSet, removeUnreachableStopStates)) {
      if (fallBack) {
        fallBackToBMC("The check of ARG failed");
      } else {
        throw new CPAException("ARG does not meet the requirements");
      }
    }
    if (checkForwardConditions && InterpolationHelper.hasCoveredStates(pReachedSet)) {
      if (fallBack) {
        fallBackToBMCWithoutForwardCondition(
            "Covered states in ARG: forward-condition might be unsound!");
      } else {
        throw new CPAException("ARG does not meet the requirements");
      }
    }
  }

  /**
   * Check forward conditions, i.e. if the program can be further unrolled. This method is an exact
   * copy of {@link IMCAlgorithm#isFurtherUnrollingPossible}
   */
  private boolean isFurtherUnrollingPossible(ReachedSet pReachedSet)
      throws SolverException, InterruptedException {
    stats.assertionsCheck.start();
    final boolean isStopStateUnreachable;
    try {
      isStopStateUnreachable =
          solver.isUnsat(InterpolationHelper.buildBoundingAssertionFormula(bfmgr, pReachedSet));
    } finally {
      stats.assertionsCheck.stop();
    }
    if (isStopStateUnreachable) {
      logger.log(Level.INFO, "The program cannot be further unrolled");
      InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
    }
    return !isStopStateUnreachable;
  }

  /** This method is an exact copy of {@link IMCAlgorithm#unrollProgram} */
  private void unrollProgram(ReachedSet pReachedSet) throws InterruptedException, CPAException {
    stats.bmcPreparation.start();
    try {
      BMCHelper.unroll(logger, pReachedSet, algorithm, cpa);
    } finally {
      stats.bmcPreparation.stop();
    }
  }

  private boolean isGlobalQueryUnsat(
      DualReachabilitySequence pDualSequence, int indexToCheck, PartitionedFormulas pFormulas)
      throws SolverException, InterruptedException {
    BooleanFormula backwardFormula =
        pDualSequence.getBackwardImageAt(pDualSequence.getSize() - indexToCheck);
    backwardFormula =
        fmgr.instantiate(
            fmgr.uninstantiate(backwardFormula),
            pFormulas.getLoopFormulaSsaMaps().get(indexToCheck - 1));

    BooleanFormula unrolledConcretePaths =
        bfmgr.and(
            new ImmutableList.Builder<BooleanFormula>()
                .add(pFormulas.getPrefixFormula())
                .addAll(pFormulas.getLoopFormulas().subList(0, indexToCheck))
                .add(backwardFormula)
                .build());
    boolean isUnsat;
    stats.assertionsCheck.start();
    try {
      isUnsat = solver.isUnsat(unrolledConcretePaths);
    } finally {
      stats.assertionsCheck.stop();
    }
    return isUnsat;
  }

  /**
   * A helper method to derive an interpolation sequence once global check shows there is no
   * counterexample of length n.
   *
   * @throws InterruptedException On shutdown request.
   */
  private ImmutableList<BooleanFormula> getInterpolationSequence(
      PartitionedFormulas pFormulas,
      DualReachabilitySequence pDualSequence,
      int pIndexOfGlobalUnsat)
      throws InterruptedException, CPAException {
    Preconditions.checkArgument(pIndexOfGlobalUnsat <= pDualSequence.getSize());
    logger.log(Level.FINE, "Extracting interpolation-sequence");
    BooleanFormula backwardFormula =
        pDualSequence.getBackwardImageAt(pDualSequence.getSize() - pIndexOfGlobalUnsat);

    // We cannot uninstantiate B_0, so we check if the global phase was unsat with B_0 or some B_i
    if (pDualSequence.getSize() > pIndexOfGlobalUnsat) {
      backwardFormula =
          fmgr.instantiate(
              fmgr.uninstantiate(backwardFormula),
              pFormulas.getLoopFormulaSsaMaps().get(pIndexOfGlobalUnsat - 1));
    } else {
      backwardFormula = pFormulas.getAssertionFormula();
    }

    ImmutableList<BooleanFormula> formulasToPush =
        new ImmutableList.Builder<BooleanFormula>()
            .add(bfmgr.and(pFormulas.getPrefixFormula(), pFormulas.getLoopFormulas().get(0)))
            .addAll(pFormulas.getLoopFormulas().subList(1, pIndexOfGlobalUnsat))
            .add(backwardFormula)
            .build();
    ImmutableList<BooleanFormula> itpSequence = itpMgr.interpolate(formulasToPush).orElseThrow();
    stats.numOfInterpolationCalls += 1;
    stats.numOfInterpolants += itpSequence.size();
    InterpolationHelper.recordInterpolantStats(fmgr, itpSequence, stats);
    logger.log(Level.ALL, "Interpolation sequence:", itpSequence);
    return itpSequence;
  }

  /**
   * A method that strengthens a forward sequence with interpolation sequence, that was collected
   * from unsatisfiable global check.
   *
   * @param pDualSequence contains the forward vector that needs to be strengthen
   * @param pItpSequence the interpolation sequence derived at the current iteration
   */
  private void strengthenForwardVectorWithInterpolants(
      DualReachabilitySequence pDualSequence,
      List<BooleanFormula> pItpSequence,
      PartitionedFormulas pFormulas) {
    logger.log(Level.FINE, "Strengthening forward reachability vector at global phase");
    SSAMap prefixSsaMap = pFormulas.getPrefixSsaMap();
    for (int i = 1; (i < pDualSequence.getSize()) && (i <= pItpSequence.size()); ++i) {
      BooleanFormula image = pDualSequence.getForwardImageAt(i);
      pDualSequence.updateForwardReachVector(
          bfmgr.and(
              image, fmgr.instantiate(fmgr.uninstantiate(pItpSequence.get(i - 1)), prefixSsaMap)),
          i);
    }
    logger.log(
        Level.ALL, "Updated forward reachability vector:", pDualSequence.getForwardReachVector());
  }

  /** This method is an exact copy of IMCAlgorithm.getCurrentMaxLoopIterations() */
  private int getCurrentMaxLoopIterations() {
    return CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();
  }

  private void fallBackToBMC(final String pReason) {
    if (isInterpolationEnabled) {
      logger.log(
          Level.WARNING, "Interpolation disabled because of " + pReason + ", falling back to BMC");
      isInterpolationEnabled = false;
    }
  }

  private void fallBackToBMCWithoutForwardCondition(final String pReason) {
    logger.log(
        Level.WARNING,
        "Forward-condition disabled because of " + pReason + ", falling back to plain BMC");
    isInterpolationEnabled = false;
    checkForwardConditions = false;
  }

  /** This method is an exact copy of {@link IMCAlgorithm#collectStatistics} */
  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(
        new Statistics() {
          @Override
          public void printStatistics(
              PrintStream out, Result result, UnmodifiableReachedSet reached) {
            itpMgr.printStatistics(writingStatisticsTo(out));
          }

          @Override
          public @Nullable String getName() {
            return "Interpolating SMT solver";
          }
        });
  }

  /** This method is an exact copy of {@link IMCAlgorithm#getCandidateInvariants} */
  @Override
  protected CandidateGenerator getCandidateInvariants() {
    throw new AssertionError(
        "Class "
            + getClass().getSimpleName()
            + " does not support this function. It should not be called.");
  }
}
