// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.collect.FluentIterable.from;

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
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
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
@Options(prefix="imc")
public class IMCAlgorithm extends AbstractBMCAlgorithm implements Algorithm {

  @Option(secure = true, description = "try using interpolation to verify programs with loops")
  private boolean interpolation = false;

  @Option(secure = true, description = "toggle deriving the interpolants from suffix formulas")
  private boolean deriveInterpolantFromSuffix = false;

  private final ConfigurableProgramAnalysis cpa;

  private final Algorithm algorithm;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  private final CFA cfa;

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
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();

  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    try {
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
   *
   * @return {@code AlgorithmStatus.UNSOUND_AND_PRECISE} if an error location is reached, i.e.,
   *         unsafe; {@code AlgorithmStatus.SOUND_AND_PRECISE} if a fixed point is derived, i.e.,
   *         safe.
   */
  private AlgorithmStatus interpolationModelChecking(final ReachedSet pReachedSet)
      throws CPAException, SolverException, InterruptedException {
    if (!(cfa.getAllLoopHeads().isPresent() && cfa.getAllLoopHeads().orElseThrow().size() <= 1)) {
      throw new CPAException("Multi-loop programs are not supported yet");
    }

    logger.log(Level.FINE, "Performing interpolation-based model checking");
    PathFormula prefixFormula = pmgr.makeEmptyPathFormula();
    BooleanFormula loopFormula = bfmgr.makeTrue();
    BooleanFormula tailFormula = bfmgr.makeTrue();
    do {
      int maxLoopIterations = CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();

      shutdownNotifier.shutdownIfNecessary();
      logger.log(Level.FINE, "Unrolling with LBE, maxLoopIterations =", maxLoopIterations);
      stats.bmcPreparation.start();
      BMCHelper.unroll(logger, pReachedSet, algorithm, cpa);
      stats.bmcPreparation.stop();
      shutdownNotifier.shutdownIfNecessary();

      if (!from(pReachedSet).transformAndConcat(e -> ((ARGState) e).getCoveredByThis()).isEmpty()) {
        throw new CPAException(
            "Covered states exist in ARG, analysis result could be wrong.");
      }

      logger.log(Level.FINE, "Collecting prefix, loop, and suffix formulas");
      if (maxLoopIterations == 1) {
        prefixFormula = getLoopHeadFormula(pReachedSet, maxLoopIterations - 1);
      } else if (maxLoopIterations == 2) {
        loopFormula = getLoopHeadFormula(pReachedSet, maxLoopIterations - 1).getFormula();
      } else {
        tailFormula =
            bfmgr.and(
                tailFormula,
                getLoopHeadFormula(pReachedSet, maxLoopIterations - 1).getFormula());
      }
      BooleanFormula suffixFormula =
          bfmgr.and(tailFormula, getErrorFormula(pReachedSet, maxLoopIterations - 1));
      logger.log(Level.ALL, "The prefix is", prefixFormula.getFormula());
      logger.log(Level.ALL, "The loop is", loopFormula);
      logger.log(Level.ALL, "The suffix is", suffixFormula);

      BooleanFormula reachErrorFormula =
          bfmgr.and(prefixFormula.getFormula(), loopFormula, suffixFormula);
      if (maxLoopIterations == 1) {
        reachErrorFormula = bfmgr.or(reachErrorFormula, getErrorFormula(pReachedSet, -1));
      }
      if (!solver.isUnsat(reachErrorFormula)) {
        logger.log(Level.FINE, "A target state is reached by BMC");
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      } else {
        logger.log(Level.FINE, "No error is found up to maxLoopIterations = ", maxLoopIterations);
        if (pReachedSet.hasViolatedProperties()) {
          TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
        }
        BooleanFormula forwardConditionFormula =
            bfmgr.and(
                prefixFormula.getFormula(),
                loopFormula,
                tailFormula,
                getLoopHeadFormula(pReachedSet, maxLoopIterations).getFormula());
        if (solver.isUnsat(forwardConditionFormula)) {
          logger.log(Level.INFO, "The program cannot be further unrolled");
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }

      if (interpolation && maxLoopIterations > 1) {
        logger.log(Level.FINE, "Computing fixed points by interpolation");
        try (InterpolatingProverEnvironment<?> itpProver =
            solver.newProverEnvironmentWithInterpolation()) {
          if (reachFixedPointByInterpolation(
              prefixFormula,
              loopFormula,
              suffixFormula,
              itpProver)) {
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }
      }
    } while (adjustConditions());
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private static boolean isLoopStart(AbstractState as) {
    return AbstractStates.extractStateByType(as, LocationState.class)
        .getLocationNode()
        .isLoopStart();
  }

  private static FluentIterable<AbstractState> getLoopStart(final ReachedSet pReachedSet) {
    return from(pReachedSet).filter(IMCAlgorithm::isLoopStart);
  }

  private static FluentIterable<AbstractState> getLoopHeadEncounterState(
      final FluentIterable<AbstractState> pFluentIterable,
      final int numEncounterLoopHead) {
    return pFluentIterable.filter(
        e -> AbstractStates.extractStateByType(e, LoopBoundState.class).getDeepestIteration()
            - 1 == numEncounterLoopHead);
  }

  /**
   * A helper method to get the block formula at the specified loop head location. Typically it
   * expects zero or one loop head state in ARG, because multi-loop programs are excluded in the
   * beginning. In this case, it returns a false path formula if there is no loop head, or the path
   * formula at the unique loop head. However, an exception is caused by the pattern
   * "{@code ERROR: goto ERROR;}". Under this situation, it returns the disjunction of the path
   * formulas to each loop head state.
   *
   * @param pReachedSet Abstract Reachability Graph
   *
   * @param numEncounterLoopHead The encounter times of the loop head location
   *
   * @return The {@code PathFormula} at the specified loop head location if the loop head is unique.
   *
   * @throws InterruptedException On shutdown request.
   *
   */
  private PathFormula getLoopHeadFormula(ReachedSet pReachedSet, int numEncounterLoopHead)
      throws InterruptedException {
    List<AbstractState> loopHeads =
        getLoopHeadEncounterState(getLoopStart(pReachedSet), numEncounterLoopHead).toList();
    PathFormula formulaToLoopHeads =
        new PathFormula(
            bfmgr.makeFalse(),
            SSAMap.emptySSAMap(),
            PointerTargetSet.emptyPointerTargetSet(),
            0);
    for (AbstractState loopHeadState : loopHeads) {
      formulaToLoopHeads =
          pmgr.makeOr(
              formulaToLoopHeads,
              PredicateAbstractState.getPredicateState(loopHeadState)
                  .getAbstractionFormula()
                  .getBlockFormula());
    }
    return formulaToLoopHeads;
  }

  /**
   * A helper method to get the block formula at the specified error locations. It uses
   * {@code checkState} to ensure that there is a unique loop head location.
   *
   * @param pReachedSet Abstract Reachability Graph
   *
   * @param numEncounterLoopHead The encounter times of the loop head location
   *
   * @return A {@code BooleanFormula} of the disjunction of block formulas at every error location
   *         if they exist; {@code False} if there is no error location.
   *
   */
  private BooleanFormula getErrorFormula(ReachedSet pReachedSet, int numEncounterLoopHead) {
    return getLoopHeadEncounterState(
        AbstractStates.getTargetStates(pReachedSet),
        numEncounterLoopHead).transform(
            es -> PredicateAbstractState.getPredicateState(es)
                .getAbstractionFormula()
                .getBlockFormula()
                .getFormula())
            .stream()
            .collect(bfmgr.toDisjunction());
  }

  /**
   * A helper method to derive an interpolant. It computes C=itp(A,B) or C'=!itp(B,A).
   *
   * @param itpProver SMT solver stack
   *
   * @param pFormulaA Formula A (prefix and loop)
   *
   * @param pFormulaB Formula B (suffix)
   *
   * @return A {@code BooleanFormula} interpolant
   *
   * @throws InterruptedException On shutdown request.
   *
   */
  private <T> BooleanFormula getInterpolantFrom(
      InterpolatingProverEnvironment<T> itpProver,
      List<T> pFormulaA,
      List<T> pFormulaB)
      throws SolverException, InterruptedException {
    if (deriveInterpolantFromSuffix) {
      logger.log(Level.FINE, "Deriving the interpolant from suffix (formula B) and negate it");
      return bfmgr.not(itpProver.getInterpolant(pFormulaB));
    } else {
      logger.log(Level.FINE, "Deriving the interpolant from prefix and loop (formula A)");
      return itpProver.getInterpolant(pFormulaA);
    }
  }

  /**
   * The method to iteratively compute fixed points by interpolation.
   *
   * @param pPrefixPathFormula the prefix {@code PathFormula} with SSA map
   *
   * @param pLoopFormula the loop {@code BooleanFormula}
   *
   * @param pSuffixFormula the suffix {@code BooleanFormula}
   *
   * @return {@code true} if a fixed point is reached, i.e., property is proved; {@code false} if
   *         the current over-approximation is unsafe.
   *
   * @throws InterruptedException On shutdown request.
   *
   */
  private <T> boolean reachFixedPointByInterpolation(
      PathFormula pPrefixPathFormula,
      BooleanFormula pLoopFormula,
      BooleanFormula pSuffixFormula,
      InterpolatingProverEnvironment<T> itpProver)
      throws InterruptedException, SolverException {
    BooleanFormula prefixFormula = pPrefixPathFormula.getFormula();
    SSAMap prefixSsaMap = pPrefixPathFormula.getSsa();
    logger.log(Level.ALL, "The SSA map is", prefixSsaMap);
    BooleanFormula currentImage = bfmgr.makeFalse();
    currentImage = bfmgr.or(currentImage, prefixFormula);

    List<T> formulaA = new ArrayList<>();
    List<T> formulaB = new ArrayList<>();
    formulaB.add(itpProver.push(pSuffixFormula));
    formulaA.add(itpProver.push(pLoopFormula));
    formulaA.add(itpProver.push(prefixFormula));

    while (itpProver.isUnsat()) {
      logger.log(Level.ALL, "The current image is", currentImage);
      BooleanFormula interpolant = getInterpolantFrom(itpProver, formulaA, formulaB);
      logger.log(Level.ALL, "The interpolant is", interpolant);
      interpolant = fmgr.instantiate(fmgr.uninstantiate(interpolant), prefixSsaMap);
      logger.log(Level.ALL, "After changing SSA", interpolant);
      if (solver.implies(interpolant, currentImage)) {
        logger.log(Level.INFO, "The current image reaches a fixed point");
        return true;
      }
      currentImage = bfmgr.or(currentImage, interpolant);
      itpProver.pop();
      formulaA.remove(formulaA.size() - 1);
      formulaA.add(itpProver.push(interpolant));
    }
    logger.log(Level.FINE, "The overapproximation is unsafe, going back to BMC phase");
    return false;
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    throw new AssertionError(
        "Class IMCAlgorithm does not support this function. It should not be called.");
  }
}
