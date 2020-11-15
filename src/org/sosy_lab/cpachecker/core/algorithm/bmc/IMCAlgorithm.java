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
import com.google.common.collect.ImmutableList;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
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

  @Option(secure = true, description = "toggle checking forward conditions")
  private boolean checkForwardConditions = true;

  @Option(secure = true, description = "toggle using interpolation to verify programs with loops")
  private boolean interpolation = true;

  @Option(
    secure = true,
    description = "toggle falling back if interpolation or forward-condition is disabled")
  private boolean fallBack = true;

  @Option(secure = true, description = "toggle deriving the interpolants from suffix formulas")
  private boolean deriveInterpolantFromSuffix = true;

  @Option(secure = true, description = "toggle removing unreachable stop states in ARG")
  private boolean removeUnreachableStopStates = false;

  private final ConfigurableProgramAnalysis cpa;

  private final Algorithm algorithm;

  private final FormulaManagerView fmgr;
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
   * @return {@code AlgorithmStatus.UNSOUND_AND_PRECISE} if an error location is reached, i.e.,
   *         unsafe; {@code AlgorithmStatus.SOUND_AND_PRECISE} if a fixed point is derived, i.e.,
   *         safe.
   */
  private AlgorithmStatus interpolationModelChecking(final ReachedSet pReachedSet)
      throws CPAException, SolverException, InterruptedException {
    if (getTargetLocations().isEmpty()) {
      for (AbstractState state : ImmutableList.copyOf(pReachedSet.getWaitlist())) {
        pReachedSet.removeOnlyFromWaitlist(state);
      }
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
    do {
      int maxLoopIterations = CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();
      // Unroll
      shutdownNotifier.shutdownIfNecessary();
      stats.bmcPreparation.start();
      BMCHelper.unroll(logger, pReachedSet, algorithm, cpa);
      stats.bmcPreparation.stop();
      shutdownNotifier.shutdownIfNecessary();
      // BMC
      boolean isTargetStateReachable = !solver.isUnsat(buildReachTargetStateFormula(pReachedSet));
      if (isTargetStateReachable) {
        logger.log(Level.FINE, "A target state is reached by BMC");
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
      // Check if interpolation or forward-condition check is applicable
      if (interpolation && !checkAndAdjustARG(pReachedSet)) {
        if (fallBack) {
          fallBackToBMC("The check of ARG failed");
        } else {
          throw new CPAException("ARG does not meet the requirements");
        }
      }
      if (checkForwardConditions && hasCoveredStates(pReachedSet)) {
        if (fallBack) {
          fallBackToBMCWithoutForwardCondition(
              "Covered states in ARG: forward-condition might be unsound!");
        } else {
          throw new CPAException("ARG does not meet the requirements");
        }
      }
      // Forward-condition check
      if (checkForwardConditions) {
        boolean isStopStateUnreachable = solver.isUnsat(buildBoundingAssertionFormula(pReachedSet));
        if (isStopStateUnreachable) {
          logger.log(Level.FINE, "The program cannot be further unrolled");
          removeUnreachableTargetStates(pReachedSet);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }
      // Interpolation
      if (interpolation
          && maxLoopIterations > 1
          && !AbstractStates.getTargetStates(pReachedSet).isEmpty()) {
        logger.log(Level.FINE, "Collecting prefix, loop, and suffix formulas");
        PartitionedFormulas formulas = collectFormulas(pReachedSet);
        formulas.printCollectedFormulas(logger);
        logger.log(Level.FINE, "Computing fixed points by interpolation");
        try (InterpolatingProverEnvironment<?> itpProver =
            solver.newProverEnvironmentWithInterpolation()) {
          if (reachFixedPointByInterpolation(itpProver, formulas)) {
            removeUnreachableTargetStates(pReachedSet);
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }
      }
      removeUnreachableTargetStates(pReachedSet);
    } while (adjustConditions());
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private void fallBackToBMC(final String pReason) {
    logger.log(Level.WARNING, pReason);
    logger.log(Level.WARNING, "Interpolation disabled: falling back to BMC");
    interpolation = false;
  }

  private void fallBackToBMCWithoutForwardCondition(final String pReason) {
    logger.log(Level.WARNING, pReason);
    logger.log(Level.WARNING, "Forward-condition disabled: falling back to plain BMC");
    interpolation = false;
    checkForwardConditions = false;
  }

  private static boolean hasCoveredStates(final ReachedSet pReachedSet) {
    return !from(pReachedSet).transformAndConcat(e -> ((ARGState) e).getCoveredByThis()).isEmpty();
  }

  private static void removeUnreachableTargetStates(ReachedSet pReachedSet) {
    if (pReachedSet.hasViolatedProperties()) {
      TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
    }
  }

  /**
   * A method to check whether interpolation is applicable. For interpolation to be applicable, ARG
   * must satisfy 1) no covered states exist and 2) there is a unique stop state. If there are
   * multiple stop states and the option {@code removeUnreachableStopStates} is {@code true}, this
   * method will remove unreachable stop states and only disable interpolation if there are multiple
   * reachable stop states. Enabling this option indeed increases the number of solved tasks, but
   * also results in some wrong proofs.
   *
   * @param pReachedSet Abstract Reachability Graph
   */
  private boolean checkAndAdjustARG(ReachedSet pReachedSet)
      throws SolverException, InterruptedException {
    if (hasCoveredStates(pReachedSet)) {
      logger.log(Level.WARNING, "Covered states in ARG: interpolation might be unsound!");
      return false;
    }
    FluentIterable<AbstractState> stopStates = getStopStates(pReachedSet);
    if (stopStates.size() > 1) {
      if (!removeUnreachableStopStates) {
        logger.log(Level.WARNING, "Multiple stop states: interpolation might be unsound!");
        return false;
      }
      List<AbstractState> unreachableStopStates = getUnreachableStopStates(stopStates);
      boolean hasMultiReachableStopStates = (stopStates.size() - unreachableStopStates.size() > 1);
      if (!unreachableStopStates.isEmpty()) {
        logger.log(Level.FINE, "Removing", unreachableStopStates.size(), "unreachable stop states");
        ARGReachedSet reachedSetARG = new ARGReachedSet(pReachedSet, cpa);
        for (ARGState s : from(unreachableStopStates).filter(ARGState.class)) {
          reachedSetARG.removeInfeasiblePartofARG(s);
        }
      }
      if (hasMultiReachableStopStates) {
        logger.log(Level.WARNING, "Multi reachable stop states: interpolation might be unsound!");
        return false;
      }
    }
    return true;
  }

  private List<AbstractState>
      getUnreachableStopStates(final FluentIterable<AbstractState> pStopStates)
      throws SolverException, InterruptedException {
    List<AbstractState> unreachableStopStates = new ArrayList<>();
    for (AbstractState stopState : pStopStates) {
      BooleanFormula reachFormula = buildReachFormulaForStates(FluentIterable.of(stopState));
      if (solver.isUnsat(reachFormula)) {
        unreachableStopStates.add(stopState);
      }
    }
    return unreachableStopStates;
  }

  /**
   * A helper method to collect formulas needed by IMC algorithm. It assumes every target state
   * after the loop has the same abstraction-state path to root.
   *
   * @param pReachedSet Abstract Reachability Graph
   */
  private PartitionedFormulas collectFormulas(final ReachedSet pReachedSet) {
    PathFormula prefixFormula = makeFalsePathFormula();
    BooleanFormula loopFormula = bfmgr.makeTrue();
    BooleanFormula tailFormula = bfmgr.makeTrue();
    FluentIterable<AbstractState> targetStatesAfterLoop = getTargetStatesAfterLoop(pReachedSet);
    if (!targetStatesAfterLoop.isEmpty()) {
      // Initialize prefix, loop, and tail using the first target state after the loop
      List<ARGState> abstractionStates =
          getAbstractionStatesToRoot(targetStatesAfterLoop.get(0)).toList();
      prefixFormula = buildPrefixFormula(abstractionStates);
      loopFormula = buildLoopFormula(abstractionStates);
      tailFormula = buildTailFormula(abstractionStates);
    }
    return new PartitionedFormulas(
        prefixFormula,
        loopFormula,
        bfmgr.and(tailFormula, createDisjunctionFromStates(targetStatesAfterLoop)));
  }

  private PathFormula makeFalsePathFormula() {
    return new PathFormula(
        bfmgr.makeFalse(),
        SSAMap.emptySSAMap(),
        PointerTargetSet.emptyPointerTargetSet(),
        0);
  }

  private PathFormula buildPrefixFormula(final List<ARGState> pAbstractionStates) {
    return getPredicateAbstractionBlockFormula(pAbstractionStates.get(1));
  }

  private BooleanFormula buildLoopFormula(final List<ARGState> pAbstractionStates) {
    return pAbstractionStates.size() > 3
        ? getPredicateAbstractionBlockFormula(pAbstractionStates.get(2)).getFormula()
        : bfmgr.makeTrue();
  }

  private BooleanFormula buildTailFormula(final List<ARGState> pAbstractionStates) {
    List<BooleanFormula> blockFormulas = new ArrayList<>();
    if (pAbstractionStates.size() > 4) {
      for (int i = 3; i < pAbstractionStates.size() - 1; ++i) {
        blockFormulas
            .add(getPredicateAbstractionBlockFormula(pAbstractionStates.get(i)).getFormula());
      }
    }
    return bfmgr.and(blockFormulas);
  }

  private static FluentIterable<ARGState> getAbstractionStatesToRoot(AbstractState pTargetState) {
    return from(ARGUtils.getOnePathTo((ARGState) pTargetState).asStatesList())
        .filter(PredicateAbstractState::containsAbstractionState);
  }

  private static boolean isTargetStateAfterLoopStart(AbstractState pTargetState) {
    return getAbstractionStatesToRoot(pTargetState).size() > 2;
  }

  private static FluentIterable<AbstractState>
      getTargetStatesAfterLoop(final ReachedSet pReachedSet) {
    return AbstractStates.getTargetStates(pReachedSet)
        .filter(IMCAlgorithm::isTargetStateAfterLoopStart);
  }

  private static PathFormula getPredicateAbstractionBlockFormula(AbstractState pState) {
    return PredicateAbstractState.getPredicateState(pState)
        .getAbstractionFormula()
        .getBlockFormula();
  }

  /**
   * A helper method to derive an interpolant. It computes either C=itp(A,B) or C'=!itp(B,A).
   *
   * @param itpProver SMT solver stack
   * @param pFormulaA Formula A (prefix and loop)
   * @param pFormulaB Formula B (suffix)
   * @return A {@code BooleanFormula} interpolant
   * @throws InterruptedException On shutdown request.
   */
  private <T> BooleanFormula getInterpolantFrom(
      InterpolatingProverEnvironment<T> itpProver,
      final List<T> pFormulaA,
      final List<T> pFormulaB)
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
   * @param itpProver the prover with interpolation enabled
   * @return {@code true} if a fixed point is reached, i.e., property is proved; {@code false} if
   *         the current over-approximation is unsafe.
   * @throws InterruptedException On shutdown request.
   */
  private <T> boolean reachFixedPointByInterpolation(
      InterpolatingProverEnvironment<T> itpProver,
      final PartitionedFormulas formulas)
      throws InterruptedException, SolverException {
    BooleanFormula prefixBooleanFormula = formulas.prefixFormula.getFormula();
    SSAMap prefixSsaMap = formulas.prefixFormula.getSsa();
    logger.log(Level.ALL, "The SSA map is", prefixSsaMap);
    BooleanFormula currentImage = bfmgr.makeFalse();
    currentImage = bfmgr.or(currentImage, prefixBooleanFormula);

    List<T> formulaA = new ArrayList<>();
    List<T> formulaB = new ArrayList<>();
    formulaB.add(itpProver.push(formulas.suffixFormula));
    formulaA.add(itpProver.push(formulas.loopFormula));
    formulaA.add(itpProver.push(prefixBooleanFormula));

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

  /**
   * This class wraps three formulas used in interpolation in order to avoid long parameter lists.
   * These formulas are: prefixFormula (from root to the first LH), loopFormula (from the first LH
   * to the second LH), and suffixFormula (from the second LH to targets). Note that prefixFormula
   * is a {@link PathFormula} as we need its {@link SSAMap} to update the SSA indices of derived
   * interpolants.
   */
  private static class PartitionedFormulas {

    private final PathFormula prefixFormula;
    private final BooleanFormula loopFormula;
    private final BooleanFormula suffixFormula;

    public void printCollectedFormulas(LogManager pLogger) {
      pLogger.log(Level.ALL, "Prefix:", prefixFormula.getFormula());
      pLogger.log(Level.ALL, "Loop:", loopFormula);
      pLogger.log(Level.ALL, "Suffix:", suffixFormula);
    }

    public PartitionedFormulas(
        PathFormula pPrefixFormula,
        BooleanFormula pLoopFormula,
        BooleanFormula pSuffixFormula) {
      prefixFormula = pPrefixFormula;
      loopFormula = pLoopFormula;
      suffixFormula = pSuffixFormula;
    }
  }

  private BooleanFormula createDisjunctionFromStates(final FluentIterable<AbstractState> pStates) {
    return pStates.transform(e -> getPredicateAbstractionBlockFormula(e).getFormula())
        .stream()
        .collect(bfmgr.toDisjunction());
  }

  private BooleanFormula buildReachFormulaForStates(final FluentIterable<AbstractState> pGoalStates) {
    List<BooleanFormula> pathFormulas = new ArrayList<>();
    for (AbstractState goalState : pGoalStates) {
      BooleanFormula pathFormula =
          getAbstractionStatesToRoot(goalState)
              .transform(e -> getPredicateAbstractionBlockFormula(e).getFormula())
              .stream()
              .collect(bfmgr.toConjunction());
      if (!PredicateAbstractState.containsAbstractionState(goalState)) {
        pathFormula =
            bfmgr.and(
                pathFormula,
                PredicateAbstractState.getPredicateState(goalState).getPathFormula().getFormula());
      }
      pathFormulas.add(pathFormula);
    }
    return bfmgr.or(pathFormulas);
  }

  private static FluentIterable<AbstractState> getStopStates(final ReachedSet pReachedSet) {
    return from(pReachedSet).filter(AbstractBMCAlgorithm::isStopState)
        .filter(AbstractBMCAlgorithm::isRelevantForReachability);
  }

  private BooleanFormula buildReachTargetStateFormula(final ReachedSet pReachedSet) {
    return buildReachFormulaForStates(AbstractStates.getTargetStates(pReachedSet));
  }

  private BooleanFormula buildBoundingAssertionFormula(final ReachedSet pReachedSet) {
    return buildReachFormulaForStates(getStopStates(pReachedSet));
  }
}