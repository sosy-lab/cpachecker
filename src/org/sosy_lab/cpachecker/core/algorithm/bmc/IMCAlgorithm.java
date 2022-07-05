// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class provides implementations of interpolation-based model checking algorithm (IMC) and
 * interpolation-sequence based model checking algorithm (ISMC), adapted for program verification.
 *
 * <ul>
 *   <li>The original IMC algorithm was proposed in the paper "Interpolation and SAT-based Model
 *       Checking" from K. L. McMillan. The algorithm consists of two phases: BMC phase and
 *       interpolation phase. In the BMC phase, it unrolls the CFA and collects the path formula to
 *       target states. If the path formula is UNSAT, it enters the interpolation phase, and
 *       computes interpolants which are overapproximations of k-step reachable states. If the union
 *       of interpolants grows to an inductive set of states, the property is proved. Otherwise, it
 *       returns back to the BMC phase and keeps unrolling the CFA.
 *   <li>The original ISMC algorithm was proposed in the paper "Interpolation-sequence based model
 *       checking" by Yakir Vizel and Orna Grumberg. The algorithm consists of two phases: BMC phase
 *       and interpolation phase. The BMC phase of ISMC is identical to that of IMC. If the
 *       collected path formula is UNSAT, it enters the interpolation phase, and computes the
 *       overapproximation of reachable states at each unrolling step in the form of an
 *       interpolation sequence. The overapproximation is then conjoined with the * ones obtained in
 *       the previous interpolation phases and forms a reachability vector. If the reachability
 *       vector reaches a fixed point, i.e. the overapproximated state set becomes inductive, the
 *       property is proved. Otherwise, it returns back to the BMC phase and keeps unrolling the
 *       CFA.
 * </ul>
 */
@Options(prefix = "imc")
public class IMCAlgorithm extends AbstractBMCAlgorithm implements Algorithm {
  private enum FixedPointComputeStrategy {
    NONE {
      @Override
      boolean isIMCEnabled() {
        return false;
      }

      @Override
      boolean isISMCEnabled() {
        return false;
      }
    },
    ITP {
      @Override
      boolean isIMCEnabled() {
        return true;
      }

      @Override
      boolean isISMCEnabled() {
        return false;
      }
    },
    ITPSEQ {
      @Override
      boolean isIMCEnabled() {
        return false;
      }

      @Override
      boolean isISMCEnabled() {
        return true;
      }
    },
    ITPSEQ_AND_ITP {
      @Override
      boolean isIMCEnabled() {
        return true;
      }

      @Override
      boolean isISMCEnabled() {
        return true;
      }
    };

    abstract boolean isIMCEnabled();

    abstract boolean isISMCEnabled();
  }

  private enum LoopBoundIncrementStrategy {
    CONST {
      @Override
      int computeNextLoopBoundToCheck(
          int currentLoopBound, int loopBoundIncrementValue, int nextLoopBoundToCheck) {
        assert currentLoopBound <= nextLoopBoundToCheck;
        if (currentLoopBound == nextLoopBoundToCheck) {
          return currentLoopBound + loopBoundIncrementValue;
        }
        return nextLoopBoundToCheck;
      }

      @Override
      int resetLoopBoundIncrementValue(int loopBoundIncrementValue) {
        return loopBoundIncrementValue;
      }

      @Override
      int adjustLoopBoundIncrementValue(int loopBoundIncrementValue, int imcInnerLoopIter) {
        return loopBoundIncrementValue;
      }
    },
    BY_IMC_INNER {
      @Override
      int computeNextLoopBoundToCheck(
          int currentLoopBound, int loopBoundIncrementValue, int nextLoopBoundToCheck) {
        return Math.max(currentLoopBound + loopBoundIncrementValue, nextLoopBoundToCheck);
      }

      @Override
      int resetLoopBoundIncrementValue(int loopBoundIncrementValue) {
        return LoopBoundManager.DEFAULT_LOOP_BOUND_INCREMENT_VALUE;
      }

      @Override
      int adjustLoopBoundIncrementValue(int loopBoundIncrementValue, int imcInnerLoopIter) {
        return imcInnerLoopIter;
      }
    };

    abstract int computeNextLoopBoundToCheck(
        int currentLoopBound, int loopBoundIncrementValue, int nextLoopBoundToCheck);

    abstract int resetLoopBoundIncrementValue(int loopBoundIncrementValue);

    abstract int adjustLoopBoundIncrementValue(int loopBoundIncrementValue, int imcInnerLoopIter);
  }

  @Options(prefix = "imc")
  private class LoopBoundManager {
    private static final int DEFAULT_LOOP_BOUND_INCREMENT_VALUE = 1;

    @Option(secure = true, description = "toggle the strategy to increment loop bound for BMC")
    private LoopBoundIncrementStrategy loopBoundIncrementStrategyOfBMC =
        LoopBoundIncrementStrategy.CONST;

    @Option(secure = true, description = "toggle the strategy to increment loop bound for IMC")
    private LoopBoundIncrementStrategy loopBoundIncrementStrategyOfIMC =
        LoopBoundIncrementStrategy.CONST;

    @Option(secure = true, description = "toggle the strategy to increment loop bound for ISMC")
    private LoopBoundIncrementStrategy loopBoundIncrementStrategyOfISMC =
        LoopBoundIncrementStrategy.CONST;

    /** Not configurable by the user to guarantee that the shortest counterexample can be found */
    private int loopBoundIncrementValueOfBMC = DEFAULT_LOOP_BOUND_INCREMENT_VALUE;

    @Option(
        secure = true,
        description = "toggle the value to increment the loop bound by at each step for IMC")
    @IntegerOption(min = 1)
    private int loopBoundIncrementValueOfIMC = DEFAULT_LOOP_BOUND_INCREMENT_VALUE;

    @Option(
        secure = true,
        description = "toggle the value to increment the loop bound by at each step for ISMC")
    @IntegerOption(min = 1)
    private int loopBoundIncrementValueOfISMC = DEFAULT_LOOP_BOUND_INCREMENT_VALUE;

    private int nextLoopBoundForBMC = 1;
    private int nextLoopBoundForIMC = 2;
    private int nextLoopBoundForISMC = 2;

    private LoopBoundManager(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);

      // configuration checks
      if (!fixedPointComputeStrategy.isIMCEnabled()) {
        if (loopBoundIncrementStrategyOfBMC == LoopBoundIncrementStrategy.BY_IMC_INNER) {
          logger.log(
              Level.WARNING,
              "IMC is disabled, the loop bound is incremented by",
              loopBoundIncrementValueOfBMC,
              "each time for BMC.");
          loopBoundIncrementStrategyOfBMC = LoopBoundIncrementStrategy.CONST;
        }
        if (fixedPointComputeStrategy.isISMCEnabled()
            && loopBoundIncrementStrategyOfISMC == LoopBoundIncrementStrategy.BY_IMC_INNER) {
          logger.log(
              Level.WARNING,
              "IMC is disabled, the loop bound is incremented by",
              loopBoundIncrementValueOfISMC,
              "each time for ISMC.");
          loopBoundIncrementStrategyOfISMC = LoopBoundIncrementStrategy.CONST;
        }
      }
      if (loopBoundIncrementStrategyOfIMC == LoopBoundIncrementStrategy.BY_IMC_INNER
          && loopBoundIncrementValueOfIMC != DEFAULT_LOOP_BOUND_INCREMENT_VALUE) {
        logger.log(
            Level.WARNING,
            "The specified [ loopBoundIncrementValueOfIMC =",
            loopBoundIncrementValueOfIMC,
            "] will be overwritten by the configuration [ loopBoundIncrementStrategyOfIMC ="
                + " BY_IMC_INNER]");
      }
      resetLoopBoundIncrementValues();
    }

    private int getCurrentMaxLoopIterations() {
      return CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();
    }

    private void incrementLoopBoundsToCheck() {
      final int currentLoopBound = getCurrentMaxLoopIterations();
      nextLoopBoundForBMC =
          loopBoundIncrementStrategyOfBMC.computeNextLoopBoundToCheck(
              currentLoopBound, loopBoundIncrementValueOfBMC, nextLoopBoundForBMC);
      nextLoopBoundForIMC =
          loopBoundIncrementStrategyOfIMC.computeNextLoopBoundToCheck(
              currentLoopBound, loopBoundIncrementValueOfIMC, nextLoopBoundForIMC);
      nextLoopBoundForISMC =
          loopBoundIncrementStrategyOfISMC.computeNextLoopBoundToCheck(
              currentLoopBound, loopBoundIncrementValueOfISMC, nextLoopBoundForISMC);
      resetLoopBoundIncrementValues();
      logger.log(Level.FINEST, "Next loop iteration for BMC is", nextLoopBoundForBMC);
      logger.log(Level.FINEST, "Next loop iteration for IMC is", nextLoopBoundForIMC);
      logger.log(Level.FINEST, "Next loop iteration for ISMC is", nextLoopBoundForISMC);
    }

    private void adjustLoopBoundIncrementValues(int imcInnerLoopIter) {
      loopBoundIncrementValueOfBMC =
          loopBoundIncrementStrategyOfBMC.adjustLoopBoundIncrementValue(
              loopBoundIncrementValueOfBMC, imcInnerLoopIter);
      loopBoundIncrementValueOfIMC =
          loopBoundIncrementStrategyOfIMC.adjustLoopBoundIncrementValue(
              loopBoundIncrementValueOfIMC, imcInnerLoopIter);
      loopBoundIncrementValueOfISMC =
          loopBoundIncrementStrategyOfISMC.adjustLoopBoundIncrementValue(
              loopBoundIncrementValueOfISMC, imcInnerLoopIter);
      assert loopBoundIncrementValueOfBMC > 0;
      assert loopBoundIncrementValueOfIMC > 0;
      assert loopBoundIncrementValueOfISMC > 0;
    }

    private void resetLoopBoundIncrementValues() {
      loopBoundIncrementValueOfBMC =
          loopBoundIncrementStrategyOfBMC.resetLoopBoundIncrementValue(
              loopBoundIncrementValueOfBMC);
      loopBoundIncrementValueOfIMC =
          loopBoundIncrementStrategyOfIMC.resetLoopBoundIncrementValue(
              loopBoundIncrementValueOfIMC);
      loopBoundIncrementValueOfISMC =
          loopBoundIncrementStrategyOfISMC.resetLoopBoundIncrementValue(
              loopBoundIncrementValueOfISMC);
      assert loopBoundIncrementValueOfBMC == DEFAULT_LOOP_BOUND_INCREMENT_VALUE;
      assert loopBoundIncrementValueOfIMC > 0;
      assert loopBoundIncrementValueOfISMC > 0;
    }

    private boolean performBMC() {
      final int currentLoopIter = getCurrentMaxLoopIterations();
      assert currentLoopIter <= nextLoopBoundForBMC;
      if (currentLoopIter == nextLoopBoundForBMC) {
        logger.log(Level.FINE, "Performing BMC at loop iteration", currentLoopIter);
        return true;
      } else {
        logger.log(Level.FINE, "Skipping BMC at loop iteration", currentLoopIter);
        return false;
      }
    }

    private boolean performIMC() {
      final int currentLoopIter = getCurrentMaxLoopIterations();
      assert currentLoopIter <= nextLoopBoundForIMC;
      if (currentLoopIter == nextLoopBoundForIMC) {
        logger.log(Level.FINE, "Performing IMC at loop iteration", currentLoopIter);
        return true;
      } else {
        logger.log(Level.FINE, "Skipping IMC at loop iteration", currentLoopIter);
        return false;
      }
    }

    private boolean performISMC() {
      final int currentLoopIter = getCurrentMaxLoopIterations();
      assert currentLoopIter <= nextLoopBoundForISMC;
      if (currentLoopIter == nextLoopBoundForISMC) {
        logger.log(Level.FINE, "Performing ISMC at loop iteration", currentLoopIter);
        return true;
      } else {
        logger.log(Level.FINE, "Skipping ISMC at loop iteration", currentLoopIter);
        return false;
      }
    }
  }

  @Option(secure = true, description = "toggle checking forward conditions")
  private boolean checkForwardConditions = true;

  @Option(
      secure = true,
      description =
          "toggle which strategy is used for computing fixed points in order to verify programs"
              + " with loops. ITP enables IMC algorithm, and ITPSEQ enables ISMC algorithm."
              + " ITPSEQ_AND_ITP runs ISMC first, and if a fixed point is not reached by ISMC, IMC"
              + " is invoked.")
  private FixedPointComputeStrategy fixedPointComputeStrategy = FixedPointComputeStrategy.ITP;

  @Option(
      secure = true,
      description = "toggle falling back if interpolation or forward-condition is disabled")
  private boolean fallBack = true;

  @Option(secure = true, description = "toggle removing unreachable stop states in ARG")
  private boolean removeUnreachableStopStates = false;

  @Option(secure = true, description = "toggle asserting targets at every iteration for IMC")
  private boolean assertTargetsAtEveryIteration = false;

  @Option(secure = true, description = "toggle Impact-like covering for the ISMC fixed-point check")
  private boolean impactLikeCovering = false;

  private final ConfigurableProgramAnalysis cpa;

  private final Algorithm algorithm;

  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;
  private final PredicateAbstractionManager predAbsMgr;
  private final InterpolationManager itpMgr;
  private final CFA cfa;

  private BooleanFormula finalFixedPoint;
  private LoopBoundManager loopBoundMgr;

  public IMCAlgorithm(
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
    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, IMCAlgorithm.class);
    solver = predCpa.getSolver();
    pfmgr = predCpa.getPathFormulaManager();
    predAbsMgr = predCpa.getPredicateManager();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    itpMgr =
        new InterpolationManager(
            pfmgr, solver, Optional.empty(), Optional.empty(), pConfig, shutdownNotifier, logger);

    finalFixedPoint = bfmgr.makeFalse();
    loopBoundMgr = new LoopBoundManager(pConfig);

    if (assertTargetsAtEveryIteration
        && fixedPointComputeStrategy != FixedPointComputeStrategy.ITP) {
      logger.log(
          Level.WARNING,
          "Cannot assert targets at every iteration with current strategy for computing fixed"
              + " point.");
      assertTargetsAtEveryIteration = false;
    }
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    try {
      return interpolationModelChecking(pReachedSet);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      invariantGenerator.cancel();
    }
  }

  private boolean isInterpolationEnabled() {
    return fixedPointComputeStrategy != FixedPointComputeStrategy.NONE;
  }

  /**
   * The main method for interpolation-based model checking.
   *
   * @param pReachedSet Abstract Reachability Graph (ARG)
   * @return {@code AlgorithmStatus.UNSOUND_AND_PRECISE} if an error location is reached, i.e.,
   *     unsafe; {@code AlgorithmStatus.SOUND_AND_PRECISE} if a fixed point is derived, i.e., safe.
   */
  private AlgorithmStatus interpolationModelChecking(final ReachedSet pReachedSet)
      throws CPAException, SolverException, InterruptedException {
    if (getTargetLocations().isEmpty()) {
      pReachedSet.clearWaitlist();
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    if (isInterpolationEnabled() && !cfa.getAllLoopHeads().isPresent()) {
      logger.log(Level.WARNING, "Disable interpolation as loop structure could not be determined");
      fixedPointComputeStrategy = FixedPointComputeStrategy.NONE;
    }
    if (isInterpolationEnabled() && cfa.getAllLoopHeads().orElseThrow().size() > 1) {
      if (fallBack) {
        fallBackToBMC("Interpolation is not supported for multi-loop programs yet");
      } else {
        throw new CPAException("Multi-loop programs are not supported yet");
      }
    }

    logger.log(Level.FINE, "Performing interpolation-based model checking");
    List<BooleanFormula> reachVector = new ArrayList<>();
    PartitionedFormulas partitionedFormulas =
        new PartitionedFormulas(bfmgr, logger, assertTargetsAtEveryIteration);
    do {
      // Unroll
      shutdownNotifier.shutdownIfNecessary();
      stats.bmcPreparation.start();
      try {
        BMCHelper.unroll(logger, pReachedSet, algorithm, cpa);
      } finally {
        stats.bmcPreparation.stop();
      }
      shutdownNotifier.shutdownIfNecessary();
      // BMC
      if (loopBoundMgr.performBMC()) {
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
      }
      // Check if interpolation or forward-condition check is applicable
      if (isInterpolationEnabled()
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
      // Forward-condition check
      if (checkForwardConditions) {
        stats.assertionsCheck.start();
        final boolean isStopStateUnreachable;
        try {
          isStopStateUnreachable =
              solver.isUnsat(InterpolationHelper.buildBoundingAssertionFormula(bfmgr, pReachedSet));
        } finally {
          stats.assertionsCheck.stop();
        }
        if (isStopStateUnreachable) {
          logger.log(Level.FINE, "The program cannot be further unrolled");
          InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }
      // Interpolation
      if (isInterpolationEnabled()
          && loopBoundMgr.getCurrentMaxLoopIterations() > 1
          && !AbstractStates.getTargetStates(pReachedSet).isEmpty()) {
        stats.interpolationPreparation.start();
        partitionedFormulas.collectFormulasFromARG(pReachedSet);
        stats.interpolationPreparation.stop();
        if (reachFixedPoint(partitionedFormulas, reachVector)) {
          InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
          InterpolationHelper.storeFixedPointAsAbstractionAtLoopHeads(
              pReachedSet, finalFixedPoint, predAbsMgr, pfmgr);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }
      InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
      loopBoundMgr.incrementLoopBoundsToCheck();
    } while (adjustConditions());
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private void fallBackToBMC(final String pReason) {
    logger.log(
        Level.WARNING, "Interpolation disabled because of " + pReason + ", falling back to BMC");
    fixedPointComputeStrategy = FixedPointComputeStrategy.NONE;
  }

  private void fallBackToBMCWithoutForwardCondition(final String pReason) {
    logger.log(
        Level.WARNING,
        "Forward-condition disabled because of " + pReason + ", falling back to plain BMC");
    fixedPointComputeStrategy = FixedPointComputeStrategy.NONE;
    checkForwardConditions = false;
  }

  private boolean reachFixedPoint(
      final PartitionedFormulas formulas, List<BooleanFormula> reachVector)
      throws CPAException, SolverException, InterruptedException {
    stats.fixedPointComputation.start();
    try {
      boolean hasReachedFixedPoint = false;
      switch (fixedPointComputeStrategy) {
        case ITP:
          hasReachedFixedPoint = reachFixedPointByInterpolation(formulas);
          break;
        case ITPSEQ:
          hasReachedFixedPoint = reachFixedPointByInterpolationSequence(formulas, reachVector);
          break;
        case ITPSEQ_AND_ITP:
          hasReachedFixedPoint = reachFixedPointByInterpolationSequence(formulas, reachVector);
          if (!hasReachedFixedPoint) {
            hasReachedFixedPoint = reachFixedPointByInterpolation(formulas);
          }
          break;
        case NONE:
          break;
        default:
          throw new AssertionError("Unknown fixed-point strategy " + fixedPointComputeStrategy);
      }

      if (hasReachedFixedPoint) {
        return true;
      }
      logger.log(Level.FINE, "The overapproximation is unsafe, going back to BMC phase");
      return false;
    } finally {
      stats.fixedPointComputation.stop();
    }
  }

  /**
   * The method to iteratively compute fixed points by interpolation.
   *
   * @return {@code true} if a fixed point is reached, i.e., property is proved; {@code false} if
   *     the current over-approximation is unsafe.
   * @throws InterruptedException On shutdown request.
   */
  private boolean reachFixedPointByInterpolation(final PartitionedFormulas formulas)
      throws InterruptedException, CPAException, SolverException {
    if (!loopBoundMgr.performIMC()) {
      return false;
    }
    logger.log(Level.FINE, "Computing fixed points by interpolation (IMC)");
    logger.log(Level.ALL, "The SSA map is", formulas.getPrefixSsaMap());
    BooleanFormula currentImage = formulas.getPrefixFormula();

    List<BooleanFormula> loops = formulas.getLoopFormulas();
    // suffix formula: T(S1, S2) && T(S1, S2) && ... && T(Sn-1, Sn) && ~P(Sn)
    BooleanFormula suffixFormula =
        bfmgr.and(
            bfmgr.and(loops.subList(1, formulas.getNumLoops())), formulas.getAssertionFormula());

    Optional<ImmutableList<BooleanFormula>> interpolants =
        itpMgr.interpolate(ImmutableList.of(currentImage, loops.get(0), suffixFormula));
    assert interpolants.isPresent();
    int iter = 0;
    for (; interpolants.isPresent(); ++iter) {
      logger.log(Level.ALL, "IMC inner loop iteration:", iter);
      logger.log(Level.ALL, "The current image is", currentImage);
      assert interpolants.orElseThrow().size() == 2;
      BooleanFormula interpolant = interpolants.orElseThrow().get(1);
      logger.log(Level.ALL, "The interpolant is", interpolant);
      interpolant = fmgr.instantiate(fmgr.uninstantiate(interpolant), formulas.getPrefixSsaMap());
      logger.log(Level.ALL, "After changing SSA", interpolant);
      if (solver.implies(interpolant, currentImage)) {
        logger.log(Level.INFO, "The current image reaches a fixed point");
        finalFixedPoint = fmgr.uninstantiate(currentImage);
        return true;
      }
      currentImage = bfmgr.or(currentImage, interpolant);
      interpolants = itpMgr.interpolate(ImmutableList.of(interpolant, loops.get(0), suffixFormula));
    }
    logger.log(
        Level.FINE,
        "Attempted to compute fixed point in",
        iter,
        "IMC inner iterations but did not succeed");
    loopBoundMgr.adjustLoopBoundIncrementValues(iter);
    return false;
  }

  /**
   * The method to compute fixed points by interpolation-sequence.
   *
   * @return {@code true} if a fixed point is reached, i.e., property is proved; {@code false} if
   *     the current over-approximation is unsafe.
   * @throws InterruptedException On shutdown request.
   */
  private boolean reachFixedPointByInterpolationSequence(
      PartitionedFormulas pFormulas, List<BooleanFormula> reachVector)
      throws CPAException, InterruptedException, SolverException {
    if (!loopBoundMgr.performISMC()) {
      return false;
    }
    logger.log(Level.FINE, "Computing fixed points by interpolation-sequence (ISMC)");
    List<BooleanFormula> itpSequence = getInterpolationSequence(pFormulas);
    updateReachabilityVector(reachVector, itpSequence);
    return checkFixedPointOfReachabilityVector(reachVector);
  }

  /**
   * A helper method to derive an s sequence.
   *
   * @throws InterruptedException On shutdown request.
   */
  private ImmutableList<BooleanFormula> getInterpolationSequence(PartitionedFormulas pFormulas)
      throws InterruptedException, CPAException {
    logger.log(Level.FINE, "Extracting interpolation-sequence");
    ImmutableList<BooleanFormula> formulasToPush =
        new ImmutableList.Builder<BooleanFormula>()
            .add(bfmgr.and(pFormulas.getPrefixFormula(), pFormulas.getLoopFormulas().get(0)))
            .addAll(pFormulas.getLoopFormulas().subList(1, pFormulas.getNumLoops()))
            .add(pFormulas.getAssertionFormula())
            .build();
    ImmutableList<BooleanFormula> itpSequence = itpMgr.interpolate(formulasToPush).orElseThrow();
    logger.log(Level.ALL, "Interpolation sequence:", itpSequence);
    return itpSequence;
  }

  /**
   * A method to collectFormulasFromARG the reachability vector with newly derived interpolants
   *
   * @param reachVector the reachability vector of the previous iteration
   * @param itpSequence the interpolation sequence derived at the current iteration
   */
  private void updateReachabilityVector(
      List<BooleanFormula> reachVector, List<BooleanFormula> itpSequence) {
    logger.log(Level.FINE, "Updating reachability vector");

    assert reachVector.size() < itpSequence.size();
    while (reachVector.size() < itpSequence.size()) {
      reachVector.add(bfmgr.makeTrue());
    }
    assert reachVector.size() == itpSequence.size();
    for (int i = 0; i < reachVector.size(); ++i) {
      BooleanFormula image = reachVector.get(i);
      BooleanFormula itp = fmgr.uninstantiate(itpSequence.get(i));
      reachVector.set(i, bfmgr.and(image, itp));
    }
    logger.log(Level.ALL, "Updated reachability vector:", reachVector);
  }

  /**
   * A method to determine whether a fixed point has been reached.
   *
   * @param reachVector the reachability vector at current iteration
   * @return {@code true} if a fixed point is reached, i.e., property is proved; {@code false} if
   *     the current over-approximation is unsafe.
   * @throws InterruptedException On shutdown request.
   */
  private boolean checkFixedPointOfReachabilityVector(List<BooleanFormula> reachVector)
      throws InterruptedException, SolverException {
    logger.log(Level.FINE, "Checking fixed point of the reachability vector");

    if (impactLikeCovering) {
      BooleanFormula lastImage = reachVector.get(reachVector.size() - 1);
      for (int i = 0; i < reachVector.size() - 1; ++i) {
        BooleanFormula imageAtI = reachVector.get(i);
        if (solver.implies(lastImage, imageAtI)) {
          logger.log(Level.INFO, "Fixed point reached");
          finalFixedPoint = bfmgr.or(reachVector);
          return true;
        }
      }
    } else {
      BooleanFormula currentImage = reachVector.get(0);
      for (int i = 1; i < reachVector.size(); ++i) {
        BooleanFormula imageAtI = reachVector.get(i);
        if (solver.implies(imageAtI, currentImage)) {
          finalFixedPoint = currentImage;
          return true;
        }
        currentImage = bfmgr.or(currentImage, imageAtI);
      }
    }
    return false;
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    throw new AssertionError(
        "Class "
            + getClass().getSimpleName()
            + " does not support this function. It should not be called.");
  }
}
