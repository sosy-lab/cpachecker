// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
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
 * This class provides implementation of dual approximated reachability model checking
 * algorithm (DAR) adapted for program verification.
 *<p>
 * The original DAR algorithm was proposed in the paper "Intertwined Forward-Backward Reachability
 * Analysis Using Interpolants" from Y. Vizel, O. Grumberg and S. Shoham. The algorithm computes
 * two interpolant sequences - Forward (FRS) and Backward (BRS). FRS is initialized with initial
 * states formula and BRS with formula describing states that violate specification. The idea is
 * that FRS overapproximates reachability vector of states reachable from initial states,
 * on the other hand BRS overapproximates states that can reach violating states. In each iteration
 * the algorithm performs two phases - Local and Global strengthening. Let FRS = F0,F1,F2...,Fn
 * and BRS = B0,B1,B2...,Bn, the Local strengthening phase checks if Fi ∧ TR ∧ Bj is unsatisfiable,
 * if yes, then there is no counterexample of length n+1. In such case, it propagates the
 * "reason of unsatisfiability" via interpolants up to Fn+1, Bn+1 and proceeds into another
 * iteration. If no such (i,j) is found, it switches to Global strengthening phase. It performs BMC
 * and iteratively unrolls formula INIT ∧ TR ∧ ... ∧ TR ∧ Bn-i to check for satisfiability.
 * If some of the formulae is unsatisfiable, it creates interpolation sequence and strengthens
 * F0,...,Fi. If all of the formulae are satisfiable, BMC finds a counterexample.
 *<p/>
 */

@Options(prefix = "dar")
public class DARAlgorithm extends AbstractBMCAlgorithm implements Algorithm {
  @Option(
      secure = true,
      description =
          "toggle which strategy is used for computing fixed points in order to verify programs"
              + " with loops. If it is not set to true, DAR is not used.")
  private boolean isDAREnabled = true;

  @Option(
      secure = true,
      description =
          "Toggle if the algorithm should perform only global strengthening phase.")
  private boolean isLocalPhaseEnabled = true;

  @Option(
      secure = true,
      description = "toggle falling back if interpolation or forward-condition is disabled")
  private boolean fallBack = true;

  @Option(secure = true, description = "toggle removing unreachable stop states in ARG")
  private boolean removeUnreachableStopStates = false;

  @Option(secure = true, description = "toggle checking forward conditions")
  private boolean checkForwardConditions = true;

  @Option(secure = true, description = "toggle checking whether the safety property is inductive")
  private boolean checkPropertyInductiveness = false;

  @Option(secure = true, description = "toggle asserting targets at every iteration for DAR")
  private boolean assertTargetsAtEveryIteration = false;

  private final ConfigurableProgramAnalysis cpa;

  private final Algorithm algorithm;

  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;
  private final PredicateAbstractionManager predAbsMgr;
  private final InterpolationManager itpMgr;
  private final CFA cfa;

  private boolean isDualSequenceInitialized;
  private BooleanFormula finalFixedPoint;


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
    predAbsMgr = predCpa.getPredicateManager();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    itpMgr =
        new InterpolationManager(
            pfmgr, solver, Optional.empty(), Optional.empty(), pConfig,
            shutdownNotifier, logger);

    finalFixedPoint = bfmgr.makeFalse();
    isDualSequenceInitialized = false;
  }
  
  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    try {
      return dualapproximatedreachabilityModelChecking(pReachedSet);
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
  private AlgorithmStatus dualapproximatedreachabilityModelChecking(ReachedSet pReachedSet)
      throws CPAException, SolverException, InterruptedException {
    if (getTargetLocations().isEmpty()) {
      pReachedSet.clearWaitlist();
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    if (cfa.getAllLoopHeads().orElseThrow().size() < 1) {
      if (isDAREnabled) {
        logger.log(
            Level.WARNING, "Disable interpolation as loop structure could not be determined");
        isDAREnabled = false;
      }
      if (checkPropertyInductiveness) {
        logger.log(
            Level.WARNING, "Disable induction check as loop structure could not be determined");
        checkPropertyInductiveness = false;
      }
    }
    if (cfa.getAllLoopHeads().orElseThrow().size() > 1) {
      if (isDAREnabled) {
        if (fallBack) {
          fallBackToBMC("Interpolation is not supported for multi-loop programs yet");
        } else {
          throw new CPAException("Multi-loop programs are not supported yet");
        }
      }
      if (checkPropertyInductiveness) {
        logger.log(
            Level.WARNING, "Disable induction check because the program contains multiple loops");
        checkPropertyInductiveness = false;
      }
    }

    logger.log(Level.FINE, "Performing dual approximated reachability model checking");
    PartitionedFormulas partitionedFormulas =
        new PartitionedFormulas(bfmgr, logger, assertTargetsAtEveryIteration);
    DualInterpolationSequence dualSequence =
        new DualInterpolationSequence();

    do {
      shutdownNotifier.shutdownIfNecessary();
      // Unrolling twice, so that we can obtain TR formula for another iteration from PartitionedFormulas
      stats.bmcPreparation.start();
      try {
        BMCHelper.unroll(logger, pReachedSet, algorithm, cpa);
      } finally {
        stats.bmcPreparation.stop();
      }

      // BMC
      try (ProverEnvironment bmcProver =
          solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        BooleanFormula targetFormula =
            InterpolationHelper.buildReachTargetStateFormula(bfmgr, pReachedSet);
        stats.satCheck.start();
        final boolean isTargetStateReachable;
        try {
          bmcProver.push(targetFormula);
          isTargetStateReachable = !bmcProver.isUnsat();
        } finally {
          stats.satCheck.stop();
        }
        if (isTargetStateReachable) {
          logger.log(Level.FINE, "A target state is reached by BMC");
          analyzeCounterexample(targetFormula, pReachedSet, bmcProver);
          return AlgorithmStatus.UNSOUND_AND_PRECISE;
        }
      }
      // Forward-condition check
      if (!isDAREnabled && checkForwardConditions) {
        stats.assertionsCheck.start();
        final boolean isStopStateUnreachable;
        try {
          isStopStateUnreachable =
              solver.isUnsat(
                  InterpolationHelper.buildBoundingAssertionFormula(bfmgr, pReachedSet));
        } finally {
          stats.assertionsCheck.stop();
        }
        if (isStopStateUnreachable) {
          logger.log(Level.FINE, "The program cannot be further unrolled");
          InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }
      shutdownNotifier.shutdownIfNecessary();

      if (getCurrentMaxLoopIterations() > 1) {
        stats.interpolationPreparation.start();
        partitionedFormulas.collectFormulasFromARG(pReachedSet);
        stats.interpolationPreparation.stop();
      }

      // DAR
      if (isDAREnabled && getCurrentMaxLoopIterations() > 1) { //&& !InterpolationHelper.checkAndAdjustARG
          //(logger, cpa, bfmgr, solver, pReachedSet, removeUnreachableStopStates)) {
        // If LoopFormulas are empty, then no Target state is reachable in ARG, which means the program
        // is safe.
        if (partitionedFormulas.getLoopFormulas().isEmpty()) {
          InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
          InterpolationHelper.storeFixedPointAsAbstractionAtLoopHeads(
              pReachedSet, finalFixedPoint, predAbsMgr, pfmgr);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }

        if (!isDualSequenceInitialized) {
          // Initialize FRS to [INIT]
          initializeFRS(partitionedFormulas, dualSequence);
          // Initialize BRS to [~P]
          initializeBRS(partitionedFormulas, dualSequence);
          isDualSequenceInitialized = true;
        }

        if (isLocalPhaseEnabled) {
          localStrengtheningPhase(dualSequence, partitionedFormulas);
        }

        if (!isLocalPhaseEnabled || dualSequence.isLocallyUnsafe()) {
          //TODO: Add Global phase, but in the meanwhile, try to just test it with BMC
          // (it should do the same, but slower)

          // BMC Global Phase to validate local unsafety (keep this as option and compare later with
          // Global phase from paper) BMC has already verified, that the program is safe in
          // current number of steps, so we can perform interpolation.
          List<BooleanFormula> itpSequence = getForwardInterpolationSequence(partitionedFormulas);
          updateReachabilityVector(dualSequence.getForwardReachVector(), itpSequence, false);
          itpSequence = getBackwardInterpolationSequence(partitionedFormulas);
          updateReachabilityVector(dualSequence.getBackwardReachVector(), itpSequence, false);
          dualSequence.setLocallySafe();
          isLocalPhaseEnabled = true;
          //iterativeLocalStrengthening(
          //    dualSequence, partitionedFormulas, dualSequence.getSize()-1);
          }
        if (checkFixedPoint(dualSequence) || solver.isUnsat(partitionedFormulas.getAssertionFormula())) {
          InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
          //InterpolationHelper.storeFixedPointAsAbstractionAtLoopHeads(
          //    pReachedSet, finalFixedPoint, predAbsMgr, pfmgr);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }
      InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
    } while (adjustConditions());
    return null;
  }

  private boolean checkFixedPoint(DualInterpolationSequence pDualSequence)
    throws InterruptedException, SolverException {
    BooleanFormula forwardImage = fmgr.uninstantiate(pDualSequence.getForwardReachVector().get(0));
    BooleanFormula backwardImage = pDualSequence.getBackwardReachVector().get(0);

    for (int i = 1; i < pDualSequence.getSize(); i++){
      if (solver.implies(fmgr.uninstantiate(pDualSequence.getForwardReachVector().get(i)), forwardImage)){
        finalFixedPoint = forwardImage;
        return true;
      }
      forwardImage = bfmgr.or(pDualSequence.getForwardReachVector().get(i), forwardImage);
    }

    for (int i = 1; i < pDualSequence.getSize(); i++){
      // SSA map went wrong and we computed wrongly backward sequence
      if (solver.isUnsat(pDualSequence.getBackwardReachVector().get(i))){
        isDAREnabled= false;
        return false;
      }
      if (solver.implies(pDualSequence.getBackwardReachVector().get(i), backwardImage)){
        finalFixedPoint = backwardImage;
        return true;
      }
      backwardImage = bfmgr.or(pDualSequence.getBackwardReachVector().get(i), backwardImage);
    }

    return false;
  }

  private void initializeBRS
      (PartitionedFormulas pPartitionedFormulas, DualInterpolationSequence pDualSequence) {
    BooleanFormula assertionFormula = pPartitionedFormulas.getAssertionFormula();
    assertionFormula = fmgr.uninstantiate(assertionFormula);
    pDualSequence.increaseBackwardReachVector(assertionFormula);
  }
  private void initializeFRS
      (PartitionedFormulas pPartitionedFormulas, DualInterpolationSequence pDualSequence){
    BooleanFormula prefixFormula = pPartitionedFormulas.getPrefixFormula();
    pDualSequence.increaseForwardReachVector(prefixFormula);
  }

  /**
   * Checks local safety of the sequences. Further, it extends them by new overapproximating
   * formulas.
   */
  private void localStrengtheningPhase
      (DualInterpolationSequence pDualSequence, PartitionedFormulas pPartitionedFormulas)
      throws CPAException, SolverException, InterruptedException {
    int indexOfLocalContradiction;
    indexOfLocalContradiction = findIndexOfUnsatisfiableLocalCheck
        (pDualSequence, pPartitionedFormulas);
    if (indexOfLocalContradiction == -1) {
      // No local strengthening point was found, switch to Global phase
      pDualSequence.setLocallyUnsafe();
    } else {
      // Local strengthening point was found, propagate the reason for contradiction to the end
      // of sequences.
      pDualSequence.setLocallySafe();
      iterativeLocalStrengthening(pDualSequence, pPartitionedFormulas, indexOfLocalContradiction);
    }
  }
  /**
   * Strengthens the forward and backward sequences from the point of contradiction to propagate it
   * to (Fn, B0) and (F0, Bn). Further it extends the sequences.
   */
  private void iterativeLocalStrengthening
      (DualInterpolationSequence pDualSequence, PartitionedFormulas pPartitionedFormulas,
       int pIndexOfLocalContradiction)
      throws CPAException, InterruptedException, SolverException {
    int lastIndexOfSequences = pDualSequence.getForwardReachVector().size() - 1;
    int indexFRS = pIndexOfLocalContradiction;
    int indexBRS = lastIndexOfSequences - pIndexOfLocalContradiction;

    while (indexFRS < lastIndexOfSequences) {
      BooleanFormula resultingForwardFormula = pDualSequence.getForwardReachVector().get(indexFRS + 1);
      BooleanFormula interpolant = constructForwardInterpolant
          (pDualSequence, pPartitionedFormulas, indexFRS);
      resultingForwardFormula = bfmgr.and(resultingForwardFormula, interpolant);
      pDualSequence.updateForwardReachVector(resultingForwardFormula, indexFRS + 1);
      indexFRS++;
    }
    BooleanFormula newForwardReachFormula =  constructForwardInterpolant
        (pDualSequence, pPartitionedFormulas, indexFRS);
    pDualSequence.increaseForwardReachVector(newForwardReachFormula);

    while (indexBRS < lastIndexOfSequences) {
      BooleanFormula resultingBackwardFormula = pDualSequence.getBackwardReachVector().get(indexBRS + 1);
      BooleanFormula interpolant = constructBackwardInterpolant
          (pDualSequence, pPartitionedFormulas, indexBRS);
      resultingBackwardFormula = bfmgr.and(resultingBackwardFormula, interpolant);
      pDualSequence.updateBackwardReachVector(resultingBackwardFormula, indexBRS + 1);
      indexBRS++;
    }
    BooleanFormula newBackwardReachFormula =  constructBackwardInterpolant
        (pDualSequence, pPartitionedFormulas, indexBRS);
    pDualSequence.increaseBackwardReachVector(newBackwardReachFormula);
  }

  /**
   * Creates forward interpolant (denoted as FI in the paper) of the contradictory formulas (F and TR, B)
   */
  private BooleanFormula constructForwardInterpolant(DualInterpolationSequence pDualSequence,
                                        PartitionedFormulas pPartitionedFormulas, int pIndex)
      throws CPAException, InterruptedException {
    int lastIndexOfSequences = pDualSequence.getForwardReachVector().size() - 1;
    List<BooleanFormula> transitionFormulae = pPartitionedFormulas.getLoopFormulas();
    BooleanFormula forwardFormula = pDualSequence.getForwardReachVector().get(pIndex);
    BooleanFormula backwardFormula = pDualSequence.getBackwardReachVector()
        .get(lastIndexOfSequences - pIndex);

    Optional<ImmutableList<BooleanFormula>> interpolants =
        itpMgr.interpolate(ImmutableList.of(
            forwardFormula,
            transitionFormulae.get(pIndex),
            backwardFormula));
    BooleanFormula interpolant = interpolants.orElseThrow().get(1);

    return interpolant;
  }
  /**
   * Creates backward interpolant (denoted as BI in the paper) of the contradictory formulas (B and TR, F)
   */
  private BooleanFormula constructBackwardInterpolant(DualInterpolationSequence pDualSequence,
                                                        PartitionedFormulas pPartitionedFormulas, int pIndex)
      throws CPAException, InterruptedException {
    int lastIndexOfSequences = pDualSequence.getBackwardReachVector().size() - 1;
    List<BooleanFormula> transitionFormulae = pPartitionedFormulas.getLoopFormulas();
    BooleanFormula forwardFormula = pDualSequence.getForwardReachVector()
        .get(lastIndexOfSequences - pIndex);
    BooleanFormula backwardFormula = pDualSequence.getBackwardReachVector().get(pIndex);

    Optional<ImmutableList<BooleanFormula>> interpolants =
        itpMgr.interpolate(ImmutableList.of(
            backwardFormula,
            transitionFormulae.get(lastIndexOfSequences - pIndex),
            forwardFormula));
    BooleanFormula interpolant = interpolants.orElseThrow().get(1);
    interpolant = fmgr.uninstantiate(interpolant);

    return interpolant;
  }

  /**
   * A method that looks for two consecutive formulas over-approximating sets of reachable states
   * from the dual sequence that does not have a transition between them. By that we can conclude
   * that no counterexample of length n+1 exists.
   */
  private int findIndexOfUnsatisfiableLocalCheck(DualInterpolationSequence pDualSequence,
                                                 PartitionedFormulas pPartitionedFormulas)
      throws SolverException, InterruptedException {
    List<BooleanFormula> FRS = pDualSequence.getForwardReachVector();
    List<BooleanFormula> BRS = pDualSequence.getBackwardReachVector();
    List<BooleanFormula> transitionFormulae = pPartitionedFormulas.getLoopFormulas();
    List<SSAMap> transitionSsaMap = pPartitionedFormulas.getLoopFormulasSsaMap();

    int n = FRS.size() - 1;

    for (int i = FRS.size() - 1; i >= 0; i--) {
      boolean isNotReachableWithOneTransition;
      stats.assertionsCheck.start();

      try {
          isNotReachableWithOneTransition =
              solver.isUnsat(bfmgr.and(transitionFormulae.get(i),
                  BRS.get(n-i),
                  FRS.get(i)));
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
   * A helper method to derive an s sequence.
   *
   * @throws InterruptedException On shutdown request.
   */
  private ImmutableList<BooleanFormula> getForwardInterpolationSequence(PartitionedFormulas pFormulas)
      throws InterruptedException, CPAException {
    logger.log(Level.FINE, "Extracting interpolation-sequence");
    ImmutableList<BooleanFormula> formulasToPush =
        new ImmutableList.Builder<BooleanFormula>()
            .add(pFormulas.getPrefixFormula())
            .addAll(pFormulas.getLoopFormulas().subList(0, pFormulas.getNumLoops()))
            .add(pFormulas.getAssertionFormula())
            .build();
    ImmutableList<BooleanFormula> itpSequence = itpMgr.interpolate(formulasToPush).orElseThrow();
    logger.log(Level.ALL, "Interpolation sequence:", itpSequence);
    return itpSequence;
  }

  /**
   * A helper method to derive an s sequence.
   *
   * @throws InterruptedException On shutdown request.
   */
  private List<BooleanFormula> getBackwardInterpolationSequence(PartitionedFormulas pFormulas)
      throws InterruptedException, CPAException {
    logger.log(Level.FINE, "Extracting interpolation-sequence");
    List<BooleanFormula> formulasToPush = new ArrayList<>();

    formulasToPush.add(pFormulas.getAssertionFormula());
    for (int i = pFormulas.getNumLoops()-1; i >= 0; i--){
      formulasToPush.add(pFormulas.getLoopFormulas().get(i));
    }
    formulasToPush.add(pFormulas.getPrefixFormula());

    List<BooleanFormula> itpSequence = itpMgr.interpolate(formulasToPush).orElseThrow();
    List<BooleanFormula> itpSequence2 = new ArrayList<>();
    for (BooleanFormula e : itpSequence) {
      itpSequence2.add(fmgr.uninstantiate(e));
    }

    logger.log(Level.ALL, "Interpolation sequence:", itpSequence);
    return itpSequence2;
  }

  /**
   * A method to collectFormulasFromARG the reachability vector with newly derived interpolants
   *
   * @param reachVector the reachability vector of the previous iteration
   * @param itpSequence the interpolation sequence derived at the current iteration
   */
  private void updateReachabilityVector(
      List<BooleanFormula> reachVector, List<BooleanFormula> itpSequence,
      boolean keepPreviousInterpolants) {
    logger.log(Level.FINE, "Updating reachability vector");

    while (reachVector.size() < itpSequence.size()) {
      reachVector.add(bfmgr.makeTrue());
    }
    for (int i = 1; i < reachVector.size(); ++i) {
      BooleanFormula image = reachVector.get(i);
      if (keepPreviousInterpolants) {
        reachVector.set(i, bfmgr.and(image, itpSequence.get(i - 1)));
      }
      else {
        reachVector.set(i, itpSequence.get(i - 1));
      }
    }
    logger.log(Level.ALL, "Updated reachability vector:", reachVector);
  }

  private int getCurrentMaxLoopIterations() {
    return CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();
  }

  private void fallBackToBMC(final String pReason) {
    logger.log(
        Level.WARNING, "Interpolation disabled because of " + pReason + ", falling back to BMC");
    isDAREnabled = false;
  }

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

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    throw new AssertionError(
        "Class "
            + getClass().getSimpleName()
            + " does not support this function. It should not be called.");
  }
}