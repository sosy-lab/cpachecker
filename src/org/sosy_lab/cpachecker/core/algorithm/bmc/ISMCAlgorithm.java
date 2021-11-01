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
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
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
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    try {
      return runISMC(pReachedSet);
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
  private AlgorithmStatus runISMC(final ReachedSet pReachedSet)
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
    // nitialize the reachability vector
    List<BooleanFormula> reachVector = new ArrayList<BooleanFormula>();
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

      // TODO: some implemetation questions
      // - reuse solver environment or not?
      if (interpolation
          && maxLoopIterations > 1
          && !AbstractStates.getTargetStates(pReachedSet).isEmpty()) {
        
        logger.log(Level.FINE, "Collecting BMC-partitioning formulas");
        List<BooleanFormula> partitionedFormulas = collectFormulas(pReachedSet);
        logger.log(Level.ALL, "Partitioned formulas:", partitionedFormulas);

        logger.log(Level.FINE, "Extracting interpolation-sequence");
        List<BooleanFormula> itpSequence = null;
        try (InterpolatingProverEnvironment<?> itpProver =
            solver.newProverEnvironmentWithInterpolation()) {
          itpSequence = getInterpolationSequence(itpProver, partitionedFormulas);
          logger.log(Level.ALL, "Interpolation sequence:", itpSequence);
        }

        logger.log(Level.FINE, "Updating reachability vector");
        reachVector = updateReachabilityVector(reachVector, itpSequence);
        logger.log(Level.ALL, "Updated reachability vector:", reachVector);

        logger.log(Level.FINE, "Checking fiexd-point");
        if (reachFixedPoint(reachVector)) {
          logger.log(Level.INFO, "Fixed point reached");
          removeUnreachableTargetStates(pReachedSet);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        logger.log(Level.FINE, "The overapproximation is unsafe, going back to BMC phase");
      }
      removeUnreachableTargetStates(pReachedSet);
    } while (adjustConditions());
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private List<BooleanFormula> collectFormulas(final ReachedSet pReachedSet) {
    FluentIterable<AbstractState> targetStatesAfterLoop = getTargetStatesAfterLoop(pReachedSet);
    List<ARGState> abstractionStates =
          getAbstractionStatesToRoot(targetStatesAfterLoop.get(0)).toList();
    
    List<BooleanFormula> formulas = new ArrayList<>();
    for (int i = 2; i < abstractionStates.size() - 1; ++i) {
      // TR(V_k, V_k+1)
      BooleanFormula transitionRelation = getPredicateAbstractionBlockFormula(abstractionStates.get(i)).getFormula();
      if (i == 2) {
        // INIT(V_0) ^ TR(V_0, V_1)
        BooleanFormula initialCondition = getPredicateAbstractionBlockFormula(abstractionStates.get(1)).getFormula();
        transitionRelation = bfmgr.and(initialCondition, transitionRelation);
      }
      formulas.add(transitionRelation);
    }

    // ~P
    formulas.add(createDisjunctionFromStates(targetStatesAfterLoop));

    return formulas;
  }

  private <T> List<BooleanFormula> getInterpolationSequence(InterpolatingProverEnvironment<T> itpProver, List<BooleanFormula> pFormulas)
      throws InterruptedException, SolverException {
    // TODO: consider using the methods that generates interpolation sequence in ImpactAlgorithm
    // should be someting like: imgr.buildCounterexampleTrace(formulas)
    List<BooleanFormula> itpSequence = new ArrayList<>();
    for (int i = 1; i < pFormulas.size(); ++i) {
      BooleanFormula booleanFormulaA = bfmgr.and(pFormulas.subList(0, i));
      BooleanFormula booleanFormulaB = bfmgr.and(pFormulas.subList(i, pFormulas.size()));
      
      List<T> formulaA = new ArrayList<>();
      List<T> formulaB = new ArrayList<>();
      formulaA.add(itpProver.push(booleanFormulaA));
      formulaB.add(itpProver.push(booleanFormulaB));

      if (!itpProver.isUnsat()) {
        throw new AssertionError("The formula must be UNSAT to retrieve the interpolant.");
      }
      BooleanFormula interpolant = getInterpolantFrom(itpProver, formulaA, formulaB);
      itpSequence.add(fmgr.uninstantiate(interpolant));  // uninstantiate the formula

      itpProver.pop();
      itpProver.pop();
    }
    
    return itpSequence;
  }

  private List<BooleanFormula> updateReachabilityVector(
      List<BooleanFormula> oldReachVector, List<BooleanFormula> itpSequence) {
    List<BooleanFormula> newReachVector = new ArrayList<>();
    assert oldReachVector.size() + 1 == itpSequence.size();

    for (int i = 0; i < oldReachVector.size(); ++i) {
      BooleanFormula image = oldReachVector.get(i);
      BooleanFormula itp = itpSequence.get(i);
      newReachVector.add(bfmgr.and(image, itp));
    }
    newReachVector.add(itpSequence.get(itpSequence.size() - 1));
    return newReachVector;
  }

  private boolean reachFixedPoint(List<BooleanFormula> reachVector)
      throws InterruptedException, SolverException {
    BooleanFormula currentImage = reachVector.get(0);
    for (int i = 1; i < reachVector.size(); ++i) {
      BooleanFormula imageAtI = reachVector.get(i);
      if (solver.implies(imageAtI, currentImage)) {
        return true;
      }
      currentImage = bfmgr.or(currentImage, imageAtI);
    }
    return false;
  }

  // note: an exact copy from IMCAlgorithm.java
  private void fallBackToBMC(final String pReason) {
    logger.log(Level.WARNING, pReason);
    logger.log(Level.WARNING, "Interpolation disabled: falling back to BMC");
    interpolation = false;
  }

  // note: an exact copy from IMCAlgorithm.java
  private void fallBackToBMCWithoutForwardCondition(final String pReason) {
    logger.log(Level.WARNING, pReason);
    logger.log(Level.WARNING, "Forward-condition disabled: falling back to plain BMC");
    interpolation = false;
    checkForwardConditions = false;
  }

  // note: an exact copy from IMCAlgorithm.java
  private static boolean hasCoveredStates(final ReachedSet pReachedSet) {
    return !from(pReachedSet).transformAndConcat(e -> ((ARGState) e).getCoveredByThis()).isEmpty();
  }

  // note: an exact copy from IMCAlgorithm.java
  private static void removeUnreachableTargetStates(ReachedSet pReachedSet) {
    if (pReachedSet.wasTargetReached()) {
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
  // note: an exact copy from IMCAlgorithm.java
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

  // note: an exact copy from IMCAlgorithm.java
  private List<AbstractState> getUnreachableStopStates(
      final FluentIterable<AbstractState> pStopStates)
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

  // note: an exact copy from IMCAlgorithm.java
  private static FluentIterable<ARGState> getAbstractionStatesToRoot(AbstractState pTargetState) {
    return from(ARGUtils.getOnePathTo((ARGState) pTargetState).asStatesList())
        .filter(PredicateAbstractState::containsAbstractionState);
  }

  // note: an exact copy from IMCAlgorithm.java
  private static boolean isTargetStateAfterLoopStart(AbstractState pTargetState) {
    return getAbstractionStatesToRoot(pTargetState).size() > 2;
  }

  // note: an exact copy from IMCAlgorithm.java
  private static FluentIterable<AbstractState> getTargetStatesAfterLoop(
      final ReachedSet pReachedSet) {
    return AbstractStates.getTargetStates(pReachedSet)
        .filter(ISMCAlgorithm::isTargetStateAfterLoopStart);
  }

  // note: an exact copy from IMCAlgorithm.java
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
  // note: an exact copy from IMCAlgorithm.java
  private <T> BooleanFormula getInterpolantFrom(
      InterpolatingProverEnvironment<T> itpProver, final List<T> pFormulaA, final List<T> pFormulaB)
      throws SolverException, InterruptedException {
    if (deriveInterpolantFromSuffix) {
      logger.log(Level.FINE, "Deriving the interpolant from suffix (formula B) and negate it");
      return bfmgr.not(itpProver.getInterpolant(pFormulaB));
    } else {
      logger.log(Level.FINE, "Deriving the interpolant from prefix and loop (formula A)");
      return itpProver.getInterpolant(pFormulaA);
    }
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    throw new AssertionError(
        "Class "
            + getClass().getSimpleName()
            + " does not support this function. It should not be called.");
  }

  // note: an exact copy from IMCAlgorithm.java
  private BooleanFormula createDisjunctionFromStates(final FluentIterable<AbstractState> pStates) {
    return pStates.transform(e -> getPredicateAbstractionBlockFormula(e).getFormula()).stream()
        .collect(bfmgr.toDisjunction());
  }

  // note: an exact copy from IMCAlgorithm.java
  private BooleanFormula buildReachFormulaForStates(
      final FluentIterable<AbstractState> pGoalStates) {
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

  // note: an exact copy from IMCAlgorithm.java
  private static FluentIterable<AbstractState> getStopStates(final ReachedSet pReachedSet) {
    return from(pReachedSet)
        .filter(AbstractBMCAlgorithm::isStopState)
        .filter(AbstractBMCAlgorithm::isRelevantForReachability);
  }

  // note: an exact copy from IMCAlgorithm.java
  private BooleanFormula buildReachTargetStateFormula(final ReachedSet pReachedSet) {
    return buildReachFormulaForStates(AbstractStates.getTargetStates(pReachedSet));
  }

  // note: an exact copy from IMCAlgorithm.java
  private BooleanFormula buildBoundingAssertionFormula(final ReachedSet pReachedSet) {
    return buildReachFormulaForStates(getStopStates(pReachedSet));
  }
}
