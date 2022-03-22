// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.InterpolationHelper.ItpDeriveDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class provides an implementation of interpolation-sequence based model checking algorithm,
 * adapted for program verification. The original algorithm was proposed in the paper
 * "Interpolation-sequence based model checking" by Yakir Vizel and Orna Grumberg. The algorithm
 * consists of two phases: BMC phase and interpolation phase. In the BMC phase, it unrolls the CFA
 * and collects the path formula to target states. If the path formula is UNSAT, it enters the
 * interpolation phase, and computes the overapproximation of reachable states at each unrolling
 * step in the form of an interpolation sequence. The overapproximation is then conjoined with the
 * ones obtained in the previous interpolation phases and forms a reachability vector. If the
 * reachability vector reaches a fixed point, i.e. the overapproximated state set becomes inductive,
 * the property is proved. Otherwise, it returns back to the BMC phase and keeps unrolling the CFA.
 */
@Options(prefix = "ismc")
public class ISMCAlgorithm extends AbstractBMCAlgorithm implements Algorithm {

  @Option(secure = true, description = "toggle checking forward conditions")
  private boolean checkForwardConditions = true;

  @Option(secure = true, description = "toggle using interpolation to verify programs with loops")
  private boolean interpolation = true;

  @Option(
      secure = true,
      description = "toggle falling back if interpolation or forward-condition is disabled")
  private boolean fallBack = true;

  @Option(secure = true, description = "toggle which direction to derive interpolants")
  private ItpDeriveDirection itpDeriveDirection = ItpDeriveDirection.BACKWARD;

  @Option(secure = true, description = "toggle removing unreachable stop states in ARG")
  private boolean removeUnreachableStopStates = false;

  @Option(secure = true, description = "toggle Impact-like covering for the fixed-point check")
  private boolean impactLikeCovering = false;

  private final ConfigurableProgramAnalysis cpa;

  private final Algorithm algorithm;

  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;
  private final PredicateAbstractionManager predAbsMgr;

  private final CFA cfa;

  private BooleanFormula finalFixedPoint;

  public ISMCAlgorithm(
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
    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, ISMCAlgorithm.class);
    solver = predCpa.getSolver();
    pfmgr = predCpa.getPathFormulaManager();
    predAbsMgr = predCpa.getPredicateManager();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();

    finalFixedPoint = bfmgr.makeFalse();
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    try (InterpolatingProverEnvironment<?> itpProver =
        solver.newProverEnvironmentWithInterpolation()) {
      return runISMC(pReachedSet, itpProver);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      invariantGenerator.cancel();
    }
  }

  /**
   * The main method for interpolation-sequence based model checking.
   *
   * @param pReachedSet Abstract Reachability Graph (ARG)
   * @return {@code AlgorithmStatus.UNSOUND_AND_PRECISE} if an error location is reached, i.e.,
   *     unsafe; {@code AlgorithmStatus.SOUND_AND_PRECISE} if a fixed point is derived, i.e., safe.
   */
  private <T> AlgorithmStatus runISMC(
      final ReachedSet pReachedSet, InterpolatingProverEnvironment<T> itpProver)
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

    logger.log(Level.FINE, "Performing interpolation-sequence based model checking");
    // initialize the reachability vector
    List<BooleanFormula> reachVector = new ArrayList<>();
    do {
      /* note: an exact copy from IMCAlgorithm -- START */
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
      /* note: an exact copy from IMCAlgorithm -- END */

      // TODO: some implementation questions
      // - reuse solver environment or not?
      final int maxLoopIterations =
          CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();
      if (interpolation
          && maxLoopIterations > 1
          && !AbstractStates.getTargetStates(pReachedSet).isEmpty()) {
        List<BooleanFormula> partitionedFormulas = collectFormulas(pReachedSet);
        List<BooleanFormula> itpSequence = getInterpolationSequence(itpProver, partitionedFormulas);
        updateReachabilityVector(reachVector, itpSequence);

        if (reachFixedPoint(reachVector)) {
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

  /**
   * A helper method to collect formulas needed by ISMC algorithm. It assumes every target state
   * after the loop has the same abstraction-state path to root.
   *
   * @param pReachedSet Abstract Reachability Graph
   */
  private List<BooleanFormula> collectFormulas(final ReachedSet pReachedSet) {
    logger.log(Level.FINE, "Collecting BMC-partitioning formulas");
    FluentIterable<AbstractState> targetStatesAfterLoop =
        InterpolationHelper.getTargetStatesAfterLoop(pReachedSet);
    List<ARGState> abstractionStates =
        InterpolationHelper.getAbstractionStatesToRoot(targetStatesAfterLoop.get(0)).toList();

    List<BooleanFormula> formulas = new ArrayList<>();
    for (int i = 2; i < abstractionStates.size() - 1; ++i) {
      // TR(V_k, V_k+1)
      BooleanFormula transitionRelation =
          InterpolationHelper.getPredicateAbstractionBlockFormula(abstractionStates.get(i))
              .getFormula();
      if (i == 2) {
        // INIT(V_0) ^ TR(V_0, V_1)
        BooleanFormula initialCondition =
            InterpolationHelper.getPredicateAbstractionBlockFormula(abstractionStates.get(1))
                .getFormula();
        transitionRelation = bfmgr.and(initialCondition, transitionRelation);
      }
      formulas.add(transitionRelation);
    }

    // ~P
    formulas.add(InterpolationHelper.createDisjunctionFromStates(bfmgr, targetStatesAfterLoop));
    logger.log(Level.ALL, "Partitioned formulas:", formulas);
    return formulas;
  }

  /**
   * A helper method to derive an interpolation sequence.
   *
   * @param itpProver the prover with interpolation enabled
   * @param pFormulas the list of formulas to derive interpolants from, the conjunction of all
   *     formulas must be unsatisfiable
   * @throws InterruptedException On shutdown request.
   */
  private <T> List<BooleanFormula> getInterpolationSequence(
      InterpolatingProverEnvironment<T> itpProver, List<BooleanFormula> pFormulas)
      throws InterruptedException, SolverException {
    // TODO: consider using the methods that generates interpolation sequence in ImpactAlgorithm
    // should be something like: imgr.buildCounterexampleTrace(formulas)

    logger.log(Level.FINE, "Extracting interpolation-sequence");

    // push formulas
    List<T> pushedFormulas = new ArrayList<>();
    for (int i = 0; i < pFormulas.size(); ++i) {
      pushedFormulas.add(itpProver.push(pFormulas.get(i)));
    }
    if (!itpProver.isUnsat()) {
      throw new AssertionError("The formula must be UNSAT to retrieve the interpolant.");
    }

    // generate ITP sequence
    List<BooleanFormula> itpSequence = new ArrayList<>();
    for (int i = 1; i < pFormulas.size(); ++i) {
      List<T> formulaA = pushedFormulas.subList(0, i);
      List<T> formulaB = pushedFormulas.subList(i, pFormulas.size());

      BooleanFormula interpolant =
          InterpolationHelper.getInterpolantFrom(
              bfmgr, itpProver, itpDeriveDirection, formulaA, formulaB);
      itpSequence.add(fmgr.uninstantiate(interpolant)); // uninstantiate the formula
    }

    // pop formulas
    for (int i = 0; i < pFormulas.size(); ++i) {
      itpProver.pop();
    }

    logger.log(Level.ALL, "Interpolation sequence:", itpSequence);
    return itpSequence;
  }

  /**
   * A method to update the reachability vector with newly derived interpolants
   *
   * @param reachVector the reachability vector of the previous iteration
   * @param itpSequence the interpolation sequence derived at the current iteration
   */
  private void updateReachabilityVector(
      List<BooleanFormula> reachVector, List<BooleanFormula> itpSequence) {
    logger.log(Level.FINE, "Updating reachability vector");

    assert reachVector.size() + 1 == itpSequence.size();

    for (int i = 0; i < reachVector.size(); ++i) {
      BooleanFormula image = reachVector.get(i);
      BooleanFormula itp = itpSequence.get(i);
      reachVector.set(i, bfmgr.and(image, itp));
    }
    reachVector.add(itpSequence.get(itpSequence.size() - 1));
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
  private boolean reachFixedPoint(List<BooleanFormula> reachVector)
      throws InterruptedException, SolverException {
    logger.log(Level.FINE, "Checking fiexd point");

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
          logger.log(Level.INFO, "Fixed point reached");
          finalFixedPoint = currentImage;
          return true;
        }
        currentImage = bfmgr.or(currentImage, imageAtI);
      }
    }

    logger.log(Level.FINE, "The overapproximation is unsafe, going back to BMC phase");
    return false;
  }

  // note: an exact copy from IMCAlgorithm
  private void fallBackToBMC(final String pReason) {
    logger.log(
        Level.WARNING, "Interpolation disabled because of " + pReason + ", falling back to BMC");
    interpolation = false;
  }

  // note: an exact copy from IMCAlgorithm
  private void fallBackToBMCWithoutForwardCondition(final String pReason) {
    logger.log(
        Level.WARNING,
        "Forward-condition disabled because of " + pReason + ", falling back to plain BMC");
    interpolation = false;
    checkForwardConditions = false;
  }

  // note: an exact copy from IMCAlgorithm
  @Override
  protected CandidateGenerator getCandidateInvariants() {
    throw new AssertionError(
        "Class "
            + getClass().getSimpleName()
            + " does not support this function. It should not be called.");
  }
}
