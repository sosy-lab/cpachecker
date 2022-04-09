// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.assertAt;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.filterIteration;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.FormulaInContext;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class provides an implementation of interpolation-based model checking algorithm, adapted
 * for program verification. The original algorithm was proposed in the paper "Interpolation and
 * SAT-based Model Checking" from K. L. McMillan. The algorithm consists of two phases: BMC phase
 * and interpolation phase. In the BMC phase, it unrolls the CFA and collects the path formula to
 * target states. If the path formula is UNSAT, it enters the interpolation phase, and computes
 * interpolants which are overapproximations of k-step reachable states. If the union of
 * interpolants grows to an inductive set of states, the property is proved. Otherwise, it returns
 * back to the BMC phase and keeps unrolling the CFA.
 */
@Options(prefix = "imc")
public class IMCAlgorithm extends AbstractBMCAlgorithm implements Algorithm {

  @Option(secure = true, description = "toggle checking forward conditions")
  private boolean checkForwardConditions = true;

  @Option(
      secure = true,
      description = "toggle falling back if interpolation or forward-condition is disabled")
  private boolean fallBack = true;

  @Option(secure = true, description = "toggle removing unreachable stop states in ARG")
  private boolean removeUnreachableStopStates = false;

  @Option(secure = true, description = "toggle asserting targets at every iteration")
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

  private BooleanFormula finalFixedPoint;
  private BooleanFormula loopHeadInvariants;
  private boolean invariantGenerationRunning;

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
    loopHeadInvariants = bfmgr.makeTrue();
    invariantGenerationRunning =
        invariantGenerationStrategy != InvariantGeneratorFactory.DO_NOTHING;
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    invariantGenerator.start(extractLocation(pReachedSet.getFirstState()));
    try {
      invariantGeneratorHeadStart.waitForInvariantGenerator();
      return interpolationModelChecking(pReachedSet);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      invariantGenerator.cancel();
    }
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

    if (interpolation && !cfa.getAllLoopHeads().isPresent()) {
      logger.log(Level.WARNING, "Disable interpolation as loop structure could not be determined");
      interpolation = false;
    }
    if (interpolation && cfa.getAllLoopHeads().orElseThrow().size() > 1) {
      if (fallBack) {
        fallBackToBMC("Interpolation is not supported for multi-loop programs yet");
      } else {
        throw new CPAException("Multi-loop programs are not supported yet");
      }
    }

    logger.log(Level.FINE, "Performing interpolation-based model checking");
    PartitionedFormulas partitionedFormulas =
        new PartitionedFormulas(pfmgr, bfmgr, logger, assertTargetsAtEveryIteration);
    do {
      if (invariantGenerator.isProgramSafe()) {
        TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
      // Unroll
      shutdownNotifier.shutdownIfNecessary();
      stats.bmcPreparation.start();
      BMCHelper.unroll(logger, pReachedSet, algorithm, cpa);
      stats.bmcPreparation.stop();
      shutdownNotifier.shutdownIfNecessary();
      // BMC
      try (ProverEnvironment bmcProver =
          solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        BooleanFormula targetFormula =
            InterpolationHelper.buildReachTargetStateFormula(bfmgr, pReachedSet);
        bmcProver.push(targetFormula);
        boolean isTargetStateReachable = !bmcProver.isUnsat();
        if (isTargetStateReachable) {
          logger.log(Level.FINE, "A target state is reached by BMC");
          analyzeCounterexample(targetFormula, pReachedSet, bmcProver);
          return AlgorithmStatus.UNSOUND_AND_PRECISE;
        }
      }
      // Check if interpolation or forward-condition check is applicable
      if (interpolation
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
        boolean isStopStateUnreachable =
            solver.isUnsat(InterpolationHelper.buildBoundingAssertionFormula(bfmgr, pReachedSet));
        if (isStopStateUnreachable) {
          logger.log(Level.FINE, "The program cannot be further unrolled");
          InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }
      // Interpolation
      final int maxLoopIterations =
          CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();
      if (interpolation
          && maxLoopIterations > 1
          && !AbstractStates.getTargetStates(pReachedSet).isEmpty()) {
        partitionedFormulas.collectFormulasFromARG(pReachedSet);

        logger.log(Level.FINE, "Computing fixed points by interpolation");
        if (reachFixedPointByInterpolation(partitionedFormulas, pReachedSet)) {
          InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
          InterpolationHelper.storeFixedPointAsAbstractionAtLoopHeads(
              pReachedSet, finalFixedPoint, predAbsMgr, pfmgr);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }
      InterpolationHelper.removeUnreachableTargetStates(pReachedSet);
    } while (adjustConditions());
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private void fallBackToBMC(final String pReason) {
    logger.log(
        Level.WARNING, "Interpolation disabled because of " + pReason + ", falling back to BMC");
    interpolation = false;
  }

  private void fallBackToBMCWithoutForwardCondition(final String pReason) {
    logger.log(
        Level.WARNING,
        "Forward-condition disabled because of " + pReason + ", falling back to plain BMC");
    interpolation = false;
    checkForwardConditions = false;
  }

  /**
   * The method to iteratively compute fixed points by interpolation.
   *
   * @return {@code true} if a fixed point is reached, i.e., property is proved; {@code false} if
   *     the current over-approximation is unsafe.
   * @throws InterruptedException On shutdown request.
   */
  private boolean reachFixedPointByInterpolation(
      final PartitionedFormulas formulas, ReachedSet reachedSet)
      throws InterruptedException, CPAException, SolverException {
    logger.log(Level.ALL, "The SSA map is", formulas.getPrefixSsaMap());
    BooleanFormula currentImage = formulas.getPrefixFormula();
    BooleanFormula loopInv = getCurrentLoopHeadInvariants(reachedSet);

    ImmutableList<BooleanFormula> loops = formulas.getLoopFormulas();
    // suffix formula: T(S1, S2) && T(S1, S2) && ... && T(Sn-1, Sn) && ~P(Sn)
    BooleanFormula suffixFormula =
        bfmgr.and(
            bfmgr.and(loops.subList(1, formulas.getNumLoops())), formulas.getAssertionFormula());

    BlockFormulas blkFormula =
        new BlockFormulas(ImmutableList.of(currentImage, loops.get(0), suffixFormula));
    CounterexampleTraceInfo cex = itpMgr.buildCounterexampleTrace(blkFormula);
    while (cex.isSpurious()) {
      logger.log(Level.ALL, "The current image is", currentImage);
      assert cex.getInterpolants().size() == 2;
      BooleanFormula interpolant = cex.getInterpolants().get(1);
      logger.log(Level.ALL, "The interpolant is", interpolant);
      interpolant = fmgr.instantiate(fmgr.uninstantiate(interpolant), formulas.getPrefixSsaMap());
      logger.log(Level.ALL, "After changing SSA", interpolant);
      // Step 1: regular IMC check
      if (solver.implies(interpolant, currentImage)) {
        logger.log(Level.INFO, "The current image reaches a fixed point");
        finalFixedPoint = fmgr.uninstantiate(currentImage);
        return true;
      }
      // Step 2: IMC check strengthened by external invariant
      if (invariantGenerationRunning
          && solver.implies(
              bfmgr.and(interpolant, fmgr.instantiate(loopInv, formulas.getPrefixSsaMap())),
              currentImage)) {
        // Step 3: check if external invariant is inductive
        logger.log(Level.FINE, "Checking inductiveness of invariant ");
        BooleanFormula invariantTransition =
            bfmgr.and(
                fmgr.instantiate(loopInv, formulas.getPrefixSsaMap()), formulas.getLoopFormula(0));
        BooleanFormula nextInvariant = fmgr.instantiate(loopInv, formulas.getSsaMapOfLoop(0));
        if (solver.implies(invariantTransition, nextInvariant)) {
          logger.log(Level.INFO, "Fixed point reached with external invariants");
          finalFixedPoint = fmgr.uninstantiate(currentImage);
          return true;
        }
        // Step 4: check if image is relatively inductive to the external invariant
        logger.log(Level.FINE, "Checking relative inductiveness of image");
        BooleanFormula currentImageTransition =
            bfmgr.and(
                fmgr.instantiate(loopInv, formulas.getPrefixSsaMap()),
                currentImage,
                formulas.getLoopFormula(0));
        BooleanFormula nextImage = fmgr.instantiate(currentImage, formulas.getSsaMapOfLoop(0));
        if (solver.implies(currentImageTransition, nextImage)) {
          logger.log(Level.INFO, "Fixed point reached with external invariants");
          finalFixedPoint = currentImage;
          return true;
        }
      }
      currentImage = bfmgr.or(currentImage, interpolant);
      blkFormula = new BlockFormulas(ImmutableList.of(interpolant, loops.get(0), suffixFormula));
      cex = itpMgr.buildCounterexampleTrace(blkFormula);
    }
    logger.log(Level.FINE, "The overapproximation is unsafe, going back to BMC phase");
    return false;
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    throw new AssertionError(
        "Class "
            + getClass().getSimpleName()
            + " does not support this function. It should not be called.");
  }

  private BooleanFormula getCurrentLoopHeadInvariants(ReachedSet reachedSet)
      throws CPATransferException, InterruptedException {
    Iterable<AbstractState> loopHeadStates = getLoopHeadStatesAtFirstIteration(reachedSet);
    return fmgr.uninstantiate(
        assertAt(loopHeadStates, getCurrentLoopHeadInvariants(loopHeadStates), fmgr, true));
  }

  private FluentIterable<AbstractState> getLoopHeadStatesAtFirstIteration(ReachedSet reachedSet) {
    FluentIterable<AbstractState> loopHeadStates =
        AbstractStates.filterLocations(reachedSet, getLoopHeads());
    return filterIteration(loopHeadStates, 1, getLoopHeads());
  }

  // note: an exact copy from KInductionProver
  /**
   * Gets the most current invariants generated by the invariant generator.
   *
   * @return the most current invariants generated by the invariant generator.
   */
  private FormulaInContext getCurrentLoopHeadInvariants(Iterable<AbstractState> pAssertionStates) {
    Set<CFANode> stopLoopHeads =
        AbstractStates.extractLocations(
                AbstractStates.filterLocations(pAssertionStates, getLoopHeads()))
            .toSet();
    return pContext -> {
      shutdownNotifier.shutdownIfNecessary();
      if (!bfmgr.isFalse(loopHeadInvariants) && invariantGenerationRunning) {
        BooleanFormula lhi = bfmgr.makeFalse();
        for (CFANode loopHead : stopLoopHeads) {
          lhi = bfmgr.or(lhi, getCurrentLocationInvariants(loopHead, fmgr, pfmgr, pContext));
          shutdownNotifier.shutdownIfNecessary();
        }
        loopHeadInvariants = lhi;
      }
      return loopHeadInvariants;
    };
  }

  // note: an exact copy from KInductionProver
  private BooleanFormula getCurrentLocationInvariants(
      CFANode pLocation,
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager,
      PathFormula pContext)
      throws InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    InvariantSupplier currentInvariantsSupplier = getCurrentInvariantSupplier();

    return currentInvariantsSupplier.getInvariantFor(
        pLocation, Optional.empty(), pFormulaManager, pPathFormulaManager, pContext);
  }

  // note: an exact copy from KInductionProver
  private InvariantSupplier getCurrentInvariantSupplier() throws InterruptedException {
    if (invariantGenerationRunning) {
      try {
        return invariantGenerator.getSupplier();
      } catch (CPAException e) {
        logger.logUserException(Level.FINE, e, "Invariant generation failed.");
        invariantGenerationRunning = false;
      } catch (InterruptedException e) {
        shutdownNotifier.shutdownIfNecessary();
        logger.log(Level.FINE, "Invariant generation was cancelled.");
        logger.logDebugException(e);
        invariantGenerationRunning = false;
      }
    }
    return InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
  }
}
