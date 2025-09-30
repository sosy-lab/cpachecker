// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
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

  /** The strategy to compute the next loop iteration to perform a certain check. */
  private enum LoopBoundIncrementStrategy {
    /** Checks are performed every k iteration, where k is a specified constant. */
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
    /** The interval between checks are determined by the number of IMC inner loop iterations. */
    EAGER {
      @Override
      int computeNextLoopBoundToCheck(
          int currentLoopBound, int loopBoundIncrementValue, int nextLoopBoundToCheck) {
        return Math.max(currentLoopBound + loopBoundIncrementValue, nextLoopBoundToCheck);
      }

      @Override
      int resetLoopBoundIncrementValue(int loopBoundIncrementValue) {
        return 1;
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

  @Options(prefix = "imc.loopBound")
  private class LoopBoundManager {
    private static class IndividualCheckInfoWrapper {
      private final String name;
      private final LoopBoundIncrementStrategy strategy;
      private int incrementValue;
      private int nextLoopIter;

      private IndividualCheckInfoWrapper(
          String pName,
          LoopBoundIncrementStrategy pStrategy,
          int pIncrementValue,
          int pNextLoopIter) {
        name = pName;
        strategy = pStrategy;
        incrementValue = pIncrementValue;
        nextLoopIter = pNextLoopIter;
      }
    }

    @Option(
        secure = true,
        description =
            """
            toggle the strategy to determine the next loop iteration
            to execute BMC phase of IMC or ISMC
            CONST: increased by one (to guarantee a shortest counterexample)
            EAGER: skip all iterations where a bug cannot be found\
            """)
    private LoopBoundIncrementStrategy incrementStrategyForBMC = LoopBoundIncrementStrategy.CONST;

    @Option(
        secure = true,
        description =
            """
            toggle the strategy to determine the next loop iteration
            to execute k-inductive check if "checkPropertyInductiveness" is enabled
            CONST: increased by by a constant (specified via loopBoundIncrementValueForKI)
            EAGER: skip all iterations where a bug cannot be found\
            """)
    private LoopBoundIncrementStrategy incrementStrategyForKI = LoopBoundIncrementStrategy.CONST;

    @Option(
        secure = true,
        description =
            """
            toggle the strategy to determine the next loop iteration
            to execute interpolation phase of IMC
            CONST: increased by a constant (specified via loopBoundIncrementValueForIMC)
            EAGER: skip all iterations where a bug cannot be found\
            """)
    private LoopBoundIncrementStrategy incrementStrategyForIMC = LoopBoundIncrementStrategy.CONST;

    /** Not configurable by the user to ensure soundness of ISMC */
    private final LoopBoundIncrementStrategy incrementStrategyForISMC =
        LoopBoundIncrementStrategy.CONST;

    /** Not configurable by user to ensure that the shortest counterexample can be found */
    private static final int incrementValueForBMC = 1;

    @Option(
        secure = true,
        description = "toggle the value to increment the loop bound by at each step for KI")
    @IntegerOption(min = 1)
    private int incrementValueForKI = 1;

    @Option(
        secure = true,
        description = "toggle the value to increment the loop bound by at each step for IMC")
    @IntegerOption(min = 1)
    private int incrementValueForIMC = 1;

    /** Not configurable by the user to guarantee soundness of ISMC */
    private static final int incrementValueForISMC = 1;

    private int nextLoopBoundForBMC = 1;
    private int nextLoopBoundForKI = 2;
    private int nextLoopBoundForIMC = 2;
    private int nextLoopBoundForISMC = 2;

    private final IndividualCheckInfoWrapper bmcInfo;
    private final IndividualCheckInfoWrapper kiInfo;
    private final IndividualCheckInfoWrapper imcInfo;
    private final IndividualCheckInfoWrapper ismcInfo;

    private LoopBoundManager(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);

      // configuration checks
      if (!fixedPointComputeStrategy.isIMCEnabled()) {
        if (incrementStrategyForBMC == LoopBoundIncrementStrategy.EAGER) {
          logger.log(
              Level.WARNING,
              "IMC is disabled, the loop bound is incremented by",
              incrementValueForBMC,
              "each time for BMC.");
          incrementStrategyForBMC = LoopBoundIncrementStrategy.CONST;
        }
        if (incrementStrategyForKI == LoopBoundIncrementStrategy.EAGER) {
          logger.log(
              Level.WARNING,
              "IMC is disabled, the loop bound is incremented by",
              incrementValueForKI,
              "each time for KI.");
          incrementStrategyForKI = LoopBoundIncrementStrategy.CONST;
        }
      }
      if (fixedPointComputeStrategy.isIMCEnabled()
          && incrementStrategyForIMC == LoopBoundIncrementStrategy.EAGER
          && incrementValueForIMC != 1) {
        logger.log(
            Level.WARNING,
            "The specified [ loopBoundIncrementValueOfIMC =",
            incrementValueForIMC,
            "] will be overwritten by the configuration [ loopBoundIncrementStrategyForIMC ="
                + " EAGER]");
      }

      bmcInfo =
          new IndividualCheckInfoWrapper(
              "BMC", incrementStrategyForBMC, incrementValueForBMC, nextLoopBoundForBMC);
      kiInfo =
          new IndividualCheckInfoWrapper(
              "KI", incrementStrategyForKI, incrementValueForKI, nextLoopBoundForKI);
      imcInfo =
          new IndividualCheckInfoWrapper(
              "IMC", incrementStrategyForIMC, incrementValueForIMC, nextLoopBoundForIMC);
      ismcInfo =
          new IndividualCheckInfoWrapper(
              "ISMC", incrementStrategyForISMC, incrementValueForISMC, nextLoopBoundForISMC);
      resetLoopBoundIncrementValues();
    }

    private int getCurrentMaxLoopIterations() {
      return CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();
    }

    private void incrementLoopBoundToCheck(
        int currentLoopBound, IndividualCheckInfoWrapper checkInfo) {
      checkInfo.nextLoopIter =
          checkInfo.strategy.computeNextLoopBoundToCheck(
              currentLoopBound, checkInfo.incrementValue, checkInfo.nextLoopIter);
      logger.log(
          Level.FINEST, "Next loop iteration for", checkInfo.name, "is", checkInfo.nextLoopIter);
    }

    private void incrementLoopBoundsToCheck() {
      final int currentLoopBound = getCurrentMaxLoopIterations();
      for (var i : new IndividualCheckInfoWrapper[] {bmcInfo, kiInfo, imcInfo, ismcInfo}) {
        incrementLoopBoundToCheck(currentLoopBound, i);
      }
      resetLoopBoundIncrementValues();
    }

    private void adjustLoopBoundIncrementValue(
        int imcInnerLoopIter, IndividualCheckInfoWrapper checkInfo) {
      checkInfo.incrementValue =
          checkInfo.strategy.adjustLoopBoundIncrementValue(
              checkInfo.incrementValue, imcInnerLoopIter);
      assert checkInfo.incrementValue > 0;
    }

    private void adjustLoopBoundIncrementValues(int imcInnerLoopIter) {
      for (var i : new IndividualCheckInfoWrapper[] {bmcInfo, kiInfo, imcInfo, ismcInfo}) {
        adjustLoopBoundIncrementValue(imcInnerLoopIter, i);
      }
      assert ismcInfo.incrementValue == 1;
    }

    private void resetLoopBoundIncrementValue(IndividualCheckInfoWrapper checkInfo) {
      checkInfo.incrementValue =
          checkInfo.strategy.resetLoopBoundIncrementValue(checkInfo.incrementValue);
      assert checkInfo.incrementValue > 0;
    }

    private void resetLoopBoundIncrementValues() {
      for (var i : new IndividualCheckInfoWrapper[] {bmcInfo, kiInfo, imcInfo, ismcInfo}) {
        resetLoopBoundIncrementValue(i);
      }
      assert bmcInfo.incrementValue == 1;
      assert ismcInfo.incrementValue == 1;
    }

    private boolean performCheckAtCurrentIteration(IndividualCheckInfoWrapper checkInfo) {
      final int currentLoopIter = getCurrentMaxLoopIterations();
      assert currentLoopIter <= checkInfo.nextLoopIter;
      if (currentLoopIter == checkInfo.nextLoopIter) {
        logger.log(Level.FINE, "Performing", checkInfo.name, "at loop iteration", currentLoopIter);
        return true;
      } else {
        logger.log(Level.FINE, "Skipping", checkInfo.name, "at loop iteration", currentLoopIter);
        return false;
      }
    }

    private boolean performBMCAtCurrentIteration() {
      return performCheckAtCurrentIteration(bmcInfo);
    }

    private boolean performKIAtCurrentIteration() {
      return performCheckAtCurrentIteration(kiInfo);
    }

    private boolean performIMCAtCurrentIteration() {
      return performCheckAtCurrentIteration(imcInfo);
    }

    private boolean performISMCAtCurrentIteration() {
      return performCheckAtCurrentIteration(ismcInfo);
    }
  }

  private enum InvariantsInjectionStrategy {
    REFINE_FP_CHECK,
    REFINE_ITP,
    REFINE_PROPERTY
  }

  /** The wrapper class to store the options to utilize auxiliary invariants. */
  @Options(prefix = "imc.invariants")
  private static class InvariantsOptions {
    @Option(
        secure = true,
        description = "toggle which strategy is used for injecting auxiliary invariants")
    private InvariantsInjectionStrategy injectionStrategy = InvariantsInjectionStrategy.REFINE_ITP;

    private InvariantsOptions(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }
  }

  @Option(secure = true, description = "toggle checking forward conditions")
  private boolean checkForwardConditions = true;

  @Option(secure = true, description = "toggle checking whether the safety property is inductive")
  private boolean checkPropertyInductiveness = false;

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

  @Option(
      secure = true,
      description =
          "toggle whether to compute fixed-point backward by swapping initial-state (prefix) and"
              + " assertion (target) formulas")
  private boolean backwardAnalysis = false;

  private final ConfigurableProgramAnalysis cpa;

  private final Algorithm algorithm;

  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;
  private final PredicateAbstractionManager predAbsMgr;
  private final InterpolationManager itpMgr;
  private final CFA cfa;

  /**
   * A Boolean variable to track whether the initial-state (prefix) formula approximated by
   * interpolants is precise enough, i.e. whether prefix &hArr; itp-approx.
   */
  private boolean isPrefixItpPrecise;

  private BooleanFormula finalFixedPoint;
  private BooleanFormula lastInductiveAuxInv;
  private LoopBoundManager loopBoundMgr;
  private InvariantsOptions invariantsOptions;

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
        false /* not an invariant generator */,
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

    isPrefixItpPrecise = false;
    finalFixedPoint = bfmgr.makeFalse();
    lastInductiveAuxInv = bfmgr.makeTrue();
    loopBoundMgr = new LoopBoundManager(pConfig);
    invariantsOptions = new InvariantsOptions(pConfig);

    if (fixedPointComputeStrategy.isIMCEnabled() || fixedPointComputeStrategy.isISMCEnabled()) {
      stats.numOfInterpolants = 0;
      stats.numOfInterpolationCalls = 0;
    }

    // check configuration compatibility
    if (invariantGeneratorForBMC.isRunning()) {
      if (invariantsOptions.injectionStrategy == InvariantsInjectionStrategy.REFINE_PROPERTY
          && fixedPointComputeStrategy.isIMCEnabled()
          && backwardAnalysis) {
        logger.log(
            Level.WARNING,
            "Property refinement by auxiliary invariants is not supported in backward IMC. "
                + "Setting imc.backwardAnalysis to false.");
        backwardAnalysis = false;
      }
      if (invariantsOptions.injectionStrategy == InvariantsInjectionStrategy.REFINE_ITP
          && fixedPointComputeStrategy.isISMCEnabled()) {
        logger.log(
            Level.WARNING,
            "Interpolant refinement by auxiliary invariants is not supported in ISMC");
      }
      if (invariantsOptions.injectionStrategy == InvariantsInjectionStrategy.REFINE_PROPERTY
          && fixedPointComputeStrategy.isISMCEnabled()) {
        logger.log(
            Level.WARNING, "Property refinement by auxiliary invariants is not supported in ISMC");
      }
      if (impactLikeCovering) {
        logger.log(
            Level.WARNING,
            "Invariants injection is not supported by ISMC with Impact-like covering");
      }
    }
    if (assertTargetsAtEveryIteration) {
      if (fixedPointComputeStrategy != FixedPointComputeStrategy.ITP) {
        logger.log(
            Level.WARNING,
            "Cannot assert targets at every iteration with current strategy for computing fixed"
                + " point. Setting imc.assertTargetsAtEveryIteration to false.");
        assertTargetsAtEveryIteration = false;
      } else if (backwardAnalysis) {
        logger.log(
            Level.WARNING,
            "Cannot assert targets at every iteration when performing backward analysis. Setting"
                + " imc.assertTargetsAtEveryIteration to false.");
        assertTargetsAtEveryIteration = false;
      }
    }
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    invariantGeneratorForBMC.start(extractLocation(pReachedSet.getFirstState()));
    try {
      invariantGeneratorForBMC.waitForHeadStart();
      return interpolationModelChecking(pReachedSet);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      invariantGeneratorForBMC.cancel();
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
      logger.log(Level.INFO, "Empty target set");
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }
    if (isSafetyProvenByInvariantGenerator(pReachedSet)) {
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }
    adjustConfigsAccordingToCFA();
    // Initialize variables for IMC/ISMC
    PartitionedFormulas partitionedFormulas =
        backwardAnalysis
            ? PartitionedFormulas.createBackwardPartitionedFormulas(bfmgr, logger)
            : PartitionedFormulas.createForwardPartitionedFormulas(
                bfmgr, logger, assertTargetsAtEveryIteration);
    // Store the reachability vector for ISMC and/or prefix formula
    // approximation for IMC
    List<BooleanFormula> reachVector = new ArrayList<>();
    logger.log(Level.FINE, "Performing interpolation-based model checking");
    do {
      shutdownNotifier.shutdownIfNecessary();
      if (isSafetyProvenByInvariantGenerator(pReachedSet)) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
      unrollProgram(pReachedSet);
      if (findCexByBMC(pReachedSet)) {
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
      adjustConfigsAccordingToARG(pReachedSet);
      // Forward-condition check
      if (checkForwardConditions && !isFurtherUnrollingPossible(pReachedSet)) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
      if (loopBoundMgr.getCurrentMaxLoopIterations() > 1
          && !AbstractStates.getTargetStates(pReachedSet).isEmpty()) {
        shutdownNotifier.shutdownIfNecessary();
        stats.interpolationPreparation.start();
        try {
          partitionedFormulas.collectFormulasFromARG(pReachedSet);
          updateAuxiliaryInvariant(pReachedSet, partitionedFormulas);
        } finally {
          stats.interpolationPreparation.stop();
        }
        if (isSafetyProvenByInvariantGenerator(pReachedSet)) {
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        // k-induction
        if (checkPropertyInductiveness
            && loopBoundMgr.performKIAtCurrentIteration()
            && isPropertyInductive(pReachedSet, partitionedFormulas)) {
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        // Interpolation
        if (isInterpolationEnabled()
            && reachFixedPoint(pReachedSet, partitionedFormulas, reachVector)) {
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }
      if (!isPrefixItpPrecise && !reachVector.isEmpty()) {
        isPrefixItpPrecise =
            solver.implies(reachVector.getFirst(), partitionedFormulas.getPrefixFormula());
      }
      InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
      loopBoundMgr.incrementLoopBoundsToCheck();
    } while (adjustConditions());
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  /** Check the loop structure of the input program and adjust configurations accordingly */
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
   * ARG and adjust configurations accordingly
   */
  private void adjustConfigsAccordingToARG(ReachedSet pReachedSet)
      throws CPAException, SolverException, InterruptedException {
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
  }

  private void fallBackToBMC(final String pReason) {
    if (isInterpolationEnabled()) {
      logger.log(
          Level.WARNING, "Interpolation disabled because of " + pReason + ", falling back to BMC");
      fixedPointComputeStrategy = FixedPointComputeStrategy.NONE;
    }
    if (checkPropertyInductiveness) {
      logger.log(
          Level.WARNING,
          "Induction check disabled because of " + pReason + ", falling back to BMC");
      checkPropertyInductiveness = false;
    }
  }

  private void fallBackToBMCWithoutForwardCondition(final String pReason) {
    logger.log(
        Level.WARNING,
        "Forward-condition disabled because of " + pReason + ", falling back to plain BMC");
    fixedPointComputeStrategy = FixedPointComputeStrategy.NONE;
    checkForwardConditions = false;
  }

  private void unrollProgram(ReachedSet pReachedSet) throws InterruptedException, CPAException {
    stats.bmcUnrolling.start();
    try {
      BMCHelper.unroll(logger, pReachedSet, algorithm, cpa);
    } finally {
      stats.bmcUnrolling.stop();
    }
  }

  /**
   * Check if a target (error) state is reachable by BMC within current unrolling bound
   *
   * @return {@code true} if a counterexample is found, i.e., property is violated; {@code false}
   *     otherwise
   * @throws InterruptedException On shutdown request.
   */
  private boolean findCexByBMC(ReachedSet pReachedSet)
      throws InterruptedException, CPATransferException, SolverException {
    if (!loopBoundMgr.performBMCAtCurrentIteration()) {
      return false;
    }
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

  /** Check forward conditions, i.e. if the program can be further unrolled */
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

  /** Check if the safety property can be proven by <i>k</i>-induction */
  private boolean isPropertyInductive(ReachedSet pReachedSet, PartitionedFormulas formulas)
      throws InterruptedException, SolverException {
    boolean isInductive;
    try (ProverEnvironment inductionProver = solver.newProverEnvironment()) {
      stats.satCheck.start();
      inductionProver.push(fmgr.instantiate(lastInductiveAuxInv, formulas.getPrefixSsaMap()));
      inductionProver.push(bfmgr.and(formulas.getLoopFormulas()));
      inductionProver.push(formulas.getAssertionFormula());
      try {
        isInductive = inductionProver.isUnsat();
      } finally {
        stats.satCheck.stop();
      }
    }
    if (isInductive) {
      InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
      logger.log(Level.FINE, "The safety property is inductive");
      // unlike IMC/ISMC, we cannot obtain a more precise fixed point here
      finalFixedPoint = bfmgr.makeTrue();
      InterpolationHelper.storeFixedPointAsAbstractionAtLoopHeads(
          pReachedSet, finalFixedPoint, predAbsMgr, pfmgr);
    }
    return isInductive;
  }

  private boolean reachFixedPoint(
      ReachedSet pReachedSet, PartitionedFormulas formulas, List<BooleanFormula> reachVector)
      throws CPAException, SolverException, InterruptedException {
    stats.fixedPointComputation.start();
    try {
      boolean hasReachedFixedPoint = false;
      switch (fixedPointComputeStrategy) {
        case ITP -> hasReachedFixedPoint = reachFixedPointByInterpolation(formulas, reachVector);
        case ITPSEQ ->
            hasReachedFixedPoint = reachFixedPointByInterpolationSequence(formulas, reachVector);
        case ITPSEQ_AND_ITP -> {
          hasReachedFixedPoint = reachFixedPointByInterpolationSequence(formulas, reachVector);
          if (!hasReachedFixedPoint) {
            hasReachedFixedPoint = reachFixedPointByInterpolation(formulas, reachVector);
          }
        }
        case NONE -> {}
      }
      if (hasReachedFixedPoint) {
        InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
        InterpolationHelper.storeFixedPointAsAbstractionAtLoopHeads(
            pReachedSet, finalFixedPoint, predAbsMgr, pfmgr);
        finalFixedPoint = fmgr.simplifyBooleanFormula(finalFixedPoint);
        finalFixedPoint = fmgr.simplify(finalFixedPoint);
        InterpolationHelper.storeFixedPointAsAbstractionAtLoopHeads(
            pReachedSet,
            backwardAnalysis ? bfmgr.not(finalFixedPoint) : finalFixedPoint,
            predAbsMgr,
            pfmgr);
      } else {
        logger.log(Level.FINE, "The overapproximation is unsafe, going back to BMC phase");
      }
      return hasReachedFixedPoint;
    } catch (RefinementFailedException e) {
      if (fallBack) {
        logger.logDebugException(e);
        fallBackToBMC(e.getMessage());
        return false;
      } else {
        throw e;
      }
    } finally {
      stats.fixedPointComputation.stop();
    }
  }

  private void updateAuxiliaryInvariant(ReachedSet reachedSet, PartitionedFormulas formulas)
      throws CPATransferException, InterruptedException, SolverException {
    BooleanFormula loopInv =
        invariantGeneratorForBMC.getUninstantiatedInvariants(
            reachedSet, getLoopHeads(), fmgr, pfmgr);
    // Examine aux. invariant
    if (!bfmgr.isTrue(loopInv) && !lastInductiveAuxInv.equals(loopInv)) {
      logger.log(Level.ALL, "The new auxiliary loop-head invariant is", loopInv);
      if (formulas.checkInductivenessOf(solver, loopInv)) {
        logger.log(Level.FINE, "The new auxiliary loop-head invariant is inductive");
        if (bfmgr.isTrue(lastInductiveAuxInv)) {
          // print only once
          logger.log(Level.INFO, "Got an inductive auxiliary invariant at loop head");
        }
        lastInductiveAuxInv = loopInv;
      } else {
        logger.log(Level.FINE, "The new auxiliary loop-head invariant is not inductive");
      }
    }
  }

  /**
   * The method to iteratively compute fixed points by interpolation.
   *
   * @return {@code true} if a fixed point is reached, i.e., property is proved; {@code false} if
   *     the current over-approximation is unsafe.
   * @throws InterruptedException On shutdown request.
   */
  private boolean reachFixedPointByInterpolation(
      final PartitionedFormulas formulas, List<BooleanFormula> reachVector)
      throws InterruptedException, CPAException, SolverException {
    if (!loopBoundMgr.performIMCAtCurrentIteration()) {
      return false;
    }

    assert !(fixedPointComputeStrategy.isISMCEnabled() && reachVector.isEmpty());
    if (reachVector.isEmpty()) {
      reachVector.add(bfmgr.makeTrue());
    }

    final boolean isLoopInvTrivial = bfmgr.isTrue(lastInductiveAuxInv);

    logger.log(Level.FINE, "Computing fixed points by interpolation (IMC)");
    logger.log(Level.ALL, "The SSA map is", formulas.getPrefixSsaMap());
    final BooleanFormula prefixFormula = formulas.getPrefixFormula();
    BooleanFormula accumImage = bfmgr.makeFalse();

    List<BooleanFormula> loops = formulas.getLoopFormulas();
    // suffix formula: T(S1, S2) && T(S1, S2) && ... && T(Sn-1, Sn)
    BooleanFormula suffixFormula = bfmgr.and(loops.subList(1, formulas.getNumLoops()));
    //  assertion = negated property ~P(Sn)
    BooleanFormula assertionFormula = formulas.getAssertionFormula();
    // refine property (assertion) with invariant
    if (!isLoopInvTrivial
        && invariantsOptions.injectionStrategy == InvariantsInjectionStrategy.REFINE_PROPERTY) {
      assertionFormula =
          bfmgr.or(
              assertionFormula,
              fmgr.instantiate(
                  bfmgr.not(lastInductiveAuxInv),
                  formulas.getLoopFormulaSsaMaps().get(formulas.getNumLoops() - 1)));
    }
    suffixFormula = bfmgr.and(suffixFormula, assertionFormula);

    Optional<ImmutableList<BooleanFormula>> interpolants =
        itpMgr.interpolate(ImmutableList.of(prefixFormula, loops.getFirst(), suffixFormula));
    assert interpolants.isPresent();
    final int initialIMCIter = stats.numOfInterpolationCalls;
    while (interpolants.isPresent()) {
      shutdownNotifier.shutdownIfNecessary();
      stats.numOfInterpolationCalls += 1;
      stats.numOfInterpolants += 1;
      logger.log(
          Level.ALL,
          "IMC inner loop iteration:",
          stats.numOfInterpolationCalls - initialIMCIter,
          "[ accumulated:",
          stats.numOfInterpolationCalls,
          "]");
      logger.log(Level.ALL, "The current image is", accumImage);
      assert interpolants.orElseThrow().size() == 2;
      if (!fixedPointComputeStrategy.isISMCEnabled() && !isPrefixItpPrecise) {
        final BooleanFormula prefixApproximation =
            bfmgr.and(
                reachVector.getFirst(), fmgr.uninstantiate(interpolants.orElseThrow().getFirst()));
        reachVector.set(0, prefixApproximation);
      }
      BooleanFormula interpolant = interpolants.orElseThrow().get(1);
      InterpolationHelper.recordInterpolantStats(fmgr, interpolant, stats);
      logger.log(Level.ALL, "The interpolant is", interpolant);
      interpolant = fmgr.instantiate(fmgr.uninstantiate(interpolant), formulas.getPrefixSsaMap());
      logger.log(Level.ALL, "After changing SSA", interpolant);

      // Refine the interpolant when possible
      if (!isLoopInvTrivial
          && invariantsOptions.injectionStrategy == InvariantsInjectionStrategy.REFINE_ITP) {
        interpolant =
            bfmgr.and(
                interpolant,
                fmgr.instantiate(
                    backwardAnalysis ? bfmgr.not(lastInductiveAuxInv) : lastInductiveAuxInv,
                    formulas.getPrefixSsaMap()));
        logger.log(Level.ALL, "The refined interpolant is", interpolant);
      }

      // Step 1: regular IMC fixed point check
      if (solver.implies(interpolant, bfmgr.or(prefixFormula, accumImage))) {
        logger.log(Level.INFO, "The current image reaches a fixed point");
        stats.fixedPointConvergenceLength = stats.numOfInterpolationCalls - initialIMCIter;
        finalFixedPoint = bfmgr.or(reachVector.getFirst(), fmgr.uninstantiate(accumImage));
        return true;
      }
      // Step 2: IMC fixed point check strengthened by non-trivial external invariant
      if (!isLoopInvTrivial
          && invariantsOptions.injectionStrategy == InvariantsInjectionStrategy.REFINE_FP_CHECK
          && solver.implies(
              bfmgr.and(
                  interpolant, fmgr.instantiate(lastInductiveAuxInv, formulas.getPrefixSsaMap())),
              bfmgr.or(prefixFormula, accumImage))) {
        logger.log(Level.INFO, "Fixed point reached with external inductive invariants");
        stats.fixedPointConvergenceLength = stats.numOfInterpolationCalls - initialIMCIter;
        finalFixedPoint = bfmgr.or(reachVector.getFirst(), fmgr.uninstantiate(accumImage));
        return true;
      }
      accumImage = bfmgr.or(accumImage, interpolant);
      interpolants =
          itpMgr.interpolate(ImmutableList.of(interpolant, loops.getFirst(), suffixFormula));
    }
    logger.log(
        Level.FINE,
        "Attempted to compute fixed point with",
        stats.numOfInterpolationCalls - initialIMCIter,
        "IMC inner iterations but did not succeed");
    loopBoundMgr.adjustLoopBoundIncrementValues(stats.numOfInterpolationCalls - initialIMCIter);
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
    if (!loopBoundMgr.performISMCAtCurrentIteration()) {
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
            .add(pFormulas.getPrefixFormula())
            .addAll(pFormulas.getLoopFormulas().subList(0, pFormulas.getNumLoops()))
            .add(pFormulas.getAssertionFormula())
            .build();
    ImmutableList<BooleanFormula> itpSequence = itpMgr.interpolate(formulasToPush).orElseThrow();
    logger.log(Level.ALL, "Interpolation sequence:", itpSequence);
    stats.numOfInterpolationCalls += 1;
    stats.numOfInterpolants += itpSequence.size();
    InterpolationHelper.recordInterpolantStats(fmgr, itpSequence, stats);
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
    final int startIdx = isPrefixItpPrecise ? 1 : 0;
    for (int i = startIdx; i < reachVector.size(); ++i) {
      final BooleanFormula image = reachVector.get(i);
      final BooleanFormula itp = fmgr.uninstantiate(itpSequence.get(i));
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
  private boolean checkFixedPointOfReachabilityVector(
      List<BooleanFormula> reachVector)
      throws InterruptedException, SolverException {
    logger.log(Level.FINE, "Checking fixed point of the reachability vector");

    if (impactLikeCovering) {
      BooleanFormula lastImage = reachVector.getLast();
      for (int i = 0; i < reachVector.size() - 1; ++i) {
        BooleanFormula imageAtI = reachVector.get(i);
        if (solver.implies(lastImage, imageAtI)) {
          logger.log(Level.INFO, "Fixed point reached");
          finalFixedPoint = bfmgr.or(reachVector);
          return true;
        }
      }
    } else {
      BooleanFormula currentImage = reachVector.get(1);
      for (int i = 2; i < reachVector.size(); ++i) {
        BooleanFormula imageAtI = reachVector.get(i);
        // Step 1: regular ISMC check
        if (solver.implies(imageAtI, currentImage)) {
          logger.log(Level.INFO, "Fixed point reached");
          finalFixedPoint = bfmgr.or(currentImage, reachVector.getFirst());
          stats.fixedPointConvergenceLength = reachVector.size();
          return true;
        }
        // Step 2: ISMC check strengthened by external invariant
        if (!bfmgr.isTrue(lastInductiveAuxInv)
            && solver.implies(bfmgr.and(imageAtI, lastInductiveAuxInv), currentImage)) {
          logger.log(Level.INFO, "Fixed point reached with external inductive invariants");
          finalFixedPoint = bfmgr.or(currentImage, reachVector.getFirst());
          stats.fixedPointConvergenceLength = reachVector.size();
          return true;
        }
        currentImage = bfmgr.or(currentImage, imageAtI);
      }
    }
    return false;
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

  private boolean isSafetyProvenByInvariantGenerator(ReachedSet reachedSet)
      throws CPATransferException, InterruptedException {
    if (invariantGeneratorForBMC.isProgramSafe()) {
      InterpolationHelper.removeUnreachableTargetStates(reachedSet);
      InterpolationHelper.storeFixedPointAsAbstractionAtLoopHeads(
          reachedSet,
          invariantGeneratorForBMC.getUninstantiatedInvariants(
              reachedSet, getLoopHeads(), fmgr, pfmgr),
          predAbsMgr,
          pfmgr);
      logger.log(Level.INFO, "Safety proven by invariant generator");
      return true;
    }
    return false;
  }
}
