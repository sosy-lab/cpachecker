// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.CURR2_KEYWORD;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.CURR_KEYWORD;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.PREV_KEYWORD;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.terminationviamemory")
public class TerminationToReachPrecisionAdjustment implements PrecisionAdjustment {
  private final Solver solver;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaManagerView fmgr;
  private final InterpolationManager itpMgr;
  private final TerminationToReachStatistics statistics;
  private final CFA cfa;
  private final LogManager logger;
  private final ImmutableSet<Loop> allLoops;

  @Option(
      secure = true,
      description =
          "There might be programs with unsigned integer overflow, and "
              + "due to the overflow, they are non-terminating. "
              + "This option enforces the transition invariants to "
              + "have constraints to limit the mathematical integers.")
  private boolean addConstraintsToPreventOverflows = false;

  @Option(
      secure = true,
      description =
          "Some bitwise operations or modulo are UFs in integer encoding, and "
              + "they might lead to unsound results. If integer encoding is set, "
              + "we also have to use this option.")
  private boolean checkUFsInIntegerEncoding = false;

  @Option(
      secure = true,
      description =
          "Disables checks for fix-point with transition invariants and performs " + "plain BMC.")
  private boolean performBMC = false;

  public TerminationToReachPrecisionAdjustment(
      Solver pSolver,
      TerminationToReachStatistics pStatistics,
      LogManager plogger,
      CFA pCFA,
      BooleanFormulaManagerView pBfmgr,
      FormulaManagerView pFmgr,
      InterpolationManager pItpMgr,
      Configuration pConfiguration,
      ImmutableSet<Loop> pAllLoops)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    solver = pSolver;
    statistics = pStatistics;
    cfa = pCFA;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    logger = plogger;
    itpMgr = pItpMgr;
    allLoops = pAllLoops;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState)
      throws CPAException, InterruptedException {
    TerminationToReachState terminationState = (TerminationToReachState) state;
    LocationState locationState = AbstractStates.extractStateByType(fullState, LocationState.class);
    CallstackState callstackState =
        AbstractStates.extractStateByType(fullState, CallstackState.class);
    CFANode location = AbstractStates.extractLocation(locationState);

    PrecisionAdjustmentResult result =
        new PrecisionAdjustmentResult(state, precision, Action.CONTINUE);
    Pair<LocationState, CallstackState> keyPair = Pair.of(locationState, callstackState);

    if (TransitionInvariantUtils.isLoopHead(location, allLoops)
        && terminationState.getStoredValues().containsKey(keyPair)) {
      if (terminationState.getNumberOfIterationsAtLoopHead(keyPair) > 1) {
        boolean isOverapproximating = false;
        PathFormula prefixPathFormula = terminationState.getPathFormulasForPrefix().orElseThrow();
        SSAMap largestIndices =
            terminationState.getPathFormulasForIteration().get(keyPair).getSsa();

        PartitionedRelationFormula iterationFormula =
            new PartitionedRelationFormula(
                terminationState.getPathFormulasForIteration().get(keyPair).getFormula(), fmgr);
        ImmutableList<BooleanFormula> sameStateFormulas =
            buildCycleFormula(
                terminationState.getStoredValues().get(keyPair),
                largestIndices,
                terminationState.getNumberOfIterationsAtLoopHead(keyPair) - 1);

        // Compute all the transition predicates that hold for the current state
        ImmutableSet.Builder<PartitionedRelationFormula> builderTransitionPredicates =
            ImmutableSet.builder();
        ImmutableSet.Builder<PartitionedRelationFormula> builderTransitionInvariants =
            ImmutableSet.builder();
        for (PartitionedRelationFormula transitionPredicate :
            terminationState.getTransitionPredicates()) {
          if (isTransitionInvariant(transitionPredicate, iterationFormula, location)) {
            builderTransitionInvariants.add(transitionPredicate);
          }
        }

        // If the BMC queries are UNSAT, we try to compute transition invariant
        // We strengthen the transition invariant with the prefix formula
        PartitionedRelationFormula candidateTransInv =
            new PartitionedRelationFormula(bfmgr.makeFalse(), fmgr);
        while (true) {
          // Check for a lasso in the current unrolling
          try {
            if (isNonterminatingLoop(
                sameStateFormulas,
                isOverapproximating,
                isOverapproximating ? Optional.of(candidateTransInv) : Optional.empty(),
                iterationFormula,
                prefixPathFormula)) {
              if (!isOverapproximating && isSound(iterationFormula.getFormula())) {
                terminationState.makeTarget();
                result = result.withAbstractState(terminationState);
                statistics.setNonterminatingLoop(
                    cfa.getLoopStructure().orElseThrow().getLoopsForLoopHead(location));
                result = result.withAction(Action.BREAK);
                return Optional.of(result);
              }
              if (isOverapproximating) {
                return Optional.of(result);
              }
            }
          } catch (SolverException e) {
            logger.logDebugException(e);
            return Optional.of(result);
          }

          // Get the latest formula checking for the same states
          BooleanFormula latestSameStateFormula =
              getTheLatestSameStateFormula(sameStateFormulas, isOverapproximating);

          // If the user sets the algorithm to perform only BMC, then it does not try to reach the
          // fix-point
          if (performBMC) {
            break;
          }

          // Check the fix-point, i.e. check whether the new interpolant is a transition invariant
          if (isOverapproximating
              && isTransitionInvariant(candidateTransInv, iterationFormula, location)) {
            // Set the computed candidateTransInv to the terminationState
            builderTransitionPredicates.add(candidateTransInv);
            builderTransitionPredicates.addAll(terminationState.getTransitionPredicates());
            builderTransitionInvariants.add(candidateTransInv);

            TerminationToReachState newTerminationState =
                new TerminationToReachState(
                    terminationState.getStoredValues(),
                    terminationState.getNumberOfIterations(),
                    terminationState.getPathFormulasForIteration(),
                    terminationState.getPathFormulasForPrefix(),
                    terminationState.getPathFormulaFull(),
                    builderTransitionInvariants.build(),
                    builderTransitionPredicates.build());
            return Optional.of(result.withAbstractState(newTerminationState));
          }

          candidateTransInv = candidateTransInv.withPrevVarsSuffixed(PREV_KEYWORD);
          candidateTransInv = candidateTransInv.withCurrVarsSuffixed(CURR_KEYWORD);

          PartitionedRelationFormula newInterpolant;
          try {
            newInterpolant =
                computeNewRelationalInterpolant(
                    isOverapproximating,
                    candidateTransInv,
                    iterationFormula,
                    prefixPathFormula,
                    latestSameStateFormula,
                    callstackState);
          } catch (NoSuchElementException e) {
            logger.logDebugException(e);
            return Optional.of(result.withAction(Action.BREAK));
          }

          try {
            if (solver.implies(newInterpolant.getFormula(), candidateTransInv.getFormula())) {
              return Optional.of(result);
            }
          } catch (SolverException e) {
            logger.logDebugException(e);
            return Optional.of(result);
          }

          // Trying to reach the fix-point
          // We can also strengthen the candidate transition invariant with the prefix formula
          candidateTransInv =
              new PartitionedRelationFormula(
                  bfmgr.or(candidateTransInv.getFormula(), newInterpolant.getFormula()), fmgr);
          isOverapproximating = true;
        }
      }
    }
    return Optional.of(result);
  }

  /**
   * This function goes over all the saved states at the current loop head in the previous
   * iterations. It checks whether with the current path formula, the program can reach the same
   * state twice.
   *
   * @return false if there is no lasso in the current unrollings of the loops, true if the program
   *     is nonterminating and the algorithm found a lasso
   */
  private boolean isNonterminatingLoop(
      ImmutableList<BooleanFormula> sameStateFormulas,
      // tells us whether we are already computing a fix-point with abstraction
      boolean isOverapproximating,
      Optional<PartitionedRelationFormula> pCandidateTransInv,
      PartitionedRelationFormula iterationFormula,
      PathFormula prefixPathFormula)
      throws InterruptedException, SolverException {

    for (BooleanFormula sameStateFormula : sameStateFormulas) {
      boolean isTargetStateReachable;
      // Construct formula:
      // T(x__PREV, x__CURR) and Tr(x__CURR, x__CURR2) and x__PREV = x_CURR2
      if (isOverapproximating) {
        PartitionedRelationFormula candidateTransInv = pCandidateTransInv.orElseThrow();

        // Construct formula instantiated to x__PREV = x__CURR2
        PartitionedRelationFormula sameStateFormulaRelation =
            new PartitionedRelationFormula(sameStateFormula, fmgr);
        sameStateFormulaRelation = sameStateFormulaRelation.withPrevVarsSuffixed(PREV_KEYWORD);
        sameStateFormulaRelation = sameStateFormulaRelation.withCurrVarsSuffixed(CURR2_KEYWORD);

        // Set the prev vars in T to match x__PREV and the curr cars to match x__CURR
        candidateTransInv = candidateTransInv.withPrevVarsSuffixed(PREV_KEYWORD);
        candidateTransInv = candidateTransInv.withCurrVarsSuffixed(CURR_KEYWORD);

        // Set the prev vars in Tr to match x__CURR and the curr cars to match x__CURR2
        iterationFormula = iterationFormula.withPrevVarsSuffixed(CURR_KEYWORD);
        iterationFormula = iterationFormula.withCurrVarsSuffixed(CURR2_KEYWORD);

        isTargetStateReachable =
            !solver.isUnsat(
                bfmgr.and(
                    candidateTransInv.getFormula(),
                    iterationFormula.getFormula(),
                    sameStateFormulaRelation.getFormula()));
      } else {
        isTargetStateReachable =
            !solver.isUnsat(
                bfmgr.and(
                    prefixPathFormula.getFormula(),
                    iterationFormula.getFormula(),
                    sameStateFormula));
      }
      if (isTargetStateReachable) {
        return true;
      }
    }
    return false;
  }

  /**
   * This method gets the latest same state formula instantiated for the fix-point check if we are
   * abstracting already.
   */
  private BooleanFormula getTheLatestSameStateFormula(
      ImmutableList<BooleanFormula> sameStateFormulas, boolean isOverapproximating) {
    BooleanFormula latestSameStateFormula = sameStateFormulas.getLast();
    if (isOverapproximating) {
      // Construct formula instantiated to x__PREV = x__CURR2
      PartitionedRelationFormula sameStateFormulaRelation =
          new PartitionedRelationFormula(latestSameStateFormula, fmgr);
      sameStateFormulaRelation = sameStateFormulaRelation.withPrevVarsSuffixed(PREV_KEYWORD);
      sameStateFormulaRelation = sameStateFormulaRelation.withCurrVarsSuffixed(CURR2_KEYWORD);
      latestSameStateFormula = sameStateFormulaRelation.getFormula();
    }
    return latestSameStateFormula;
  }

  /**
   * This method computes a new relational interpolant overapproximating paths with one more step of
   * the concrete transition system.
   */
  private PartitionedRelationFormula computeNewRelationalInterpolant(
      boolean isOverapproximating,
      PartitionedRelationFormula candidateTransInv,
      PartitionedRelationFormula iterationFormula,
      PathFormula prefixPathFormula,
      BooleanFormula latestSameStateFormula,
      CallstackState callstackState)
      throws CPAException, InterruptedException, NoSuchElementException {

    BooleanFormula firstStep = prefixPathFormula.getFormula();
    if (isOverapproximating) {
      // If this is more then first unrolling, we replace the prefix formula with
      // the previously computed candidate transition invariant
      firstStep = candidateTransInv.getFormula();
      // Set the prev vars in Tr to match x__CURR and the curr cars to match x__CURR2
      iterationFormula = iterationFormula.withPrevVarsSuffixed(CURR_KEYWORD);
      iterationFormula = iterationFormula.withCurrVarsSuffixed(CURR2_KEYWORD);
    }
    BooleanFormula interpolant;

    interpolant =
        itpMgr
            .interpolate(
                ImmutableList.of(
                    bfmgr.and(firstStep, iterationFormula.getFormula()), latestSameStateFormula))
            .orElseThrow()
            .getFirst();
    if (containsOnlyIrrelevantVariables(interpolant, callstackState)) {
      return new PartitionedRelationFormula(bfmgr.makeFalse(), fmgr);
    }

    // Instantiate the new interpolant to T(x__PREV, x__CURR)
    PartitionedRelationFormula newInterpolant = new PartitionedRelationFormula(interpolant, fmgr);
    newInterpolant = newInterpolant.withPrevVarsSuffixed(PREV_KEYWORD);
    newInterpolant = newInterpolant.withCurrVarsSuffixed(CURR_KEYWORD);
    return newInterpolant;
  }

  private boolean isSound(Formula pFormula) {
    return !checkUFsInIntegerEncoding || !fmgr.hasUninterpretedFunction(pFormula);
  }

  private BooleanFormula restrictFormulaVariablesWithIntRange(
      BooleanFormula pFormula, CFANode pLocation) {
    for (Formula variable : fmgr.extractVariables(pFormula).values()) {
      String pureVarName =
          TransitionInvariantUtils.removeTransInvKeyWord(
              TransitionInvariantUtils.removeFunctionFromVarsName(
                  Iterables.getOnlyElement(
                      fmgr.extractVariableNames(fmgr.uninstantiate(variable)))));
      for (AbstractSimpleDeclaration varDecl :
          cfa.getAstCfaRelation().getVariablesAndParametersInScope(pLocation).orElseThrow()) {
        if (varDecl.getName().equals(pureVarName)
            && (varDecl.getType() instanceof CSimpleType sType
                && !cfa.getMachineModel().isSigned(sType))) {
          pFormula =
              bfmgr.and(
                  pFormula,
                  CtoFormulaTypeUtils.makeRangeConstraint(
                      fmgr, variable, sType, cfa.getMachineModel()));
        }
      }
    }
    return pFormula;
  }

  /**
   * It can happen that the transition invariant contains only variables outside the loop or
   * function. In that case, we have to not use the invariant as it might be potentially unsound.
   */
  private boolean containsOnlyIrrelevantVariables(
      BooleanFormula pInvariant, CallstackState pCallstackState) {
    for (Entry<String, Formula> varNames : fmgr.extractVariables(pInvariant).entrySet()) {
      if (varNames.getKey().startsWith(pCallstackState.getCurrentFunction())
          || fmgr.getFormulaType(varNames.getValue()).isArrayType()) {
        return false;
      }
    }
    return true;
  }

  private boolean isTransitionInvariant(
      PartitionedRelationFormula candidateTransitionInvariant,
      PartitionedRelationFormula iterationFormula,
      CFANode pLocation)
      throws InterruptedException {
    // The goal is to construct formula of the following form:
    // T(x__PREV, x__CURR) and Tr(x__CURR, x__CURR2) => T(x__PREV, x__CURR2)

    // Construct T(x__PREV, x__CURR)
    candidateTransitionInvariant = candidateTransitionInvariant.withPrevVarsSuffixed(PREV_KEYWORD);
    candidateTransitionInvariant = candidateTransitionInvariant.withCurrVarsSuffixed(CURR_KEYWORD);
    BooleanFormula firstStepInTransInv = candidateTransitionInvariant.getFormula();

    // Construct T(x__PREV, x__CURR2)
    candidateTransitionInvariant = candidateTransitionInvariant.withCurrVarsSuffixed(CURR2_KEYWORD);
    BooleanFormula secondStepInTransInv = candidateTransitionInvariant.getFormula();

    if (addConstraintsToPreventOverflows) {
      firstStepInTransInv = restrictFormulaVariablesWithIntRange(firstStepInTransInv, pLocation);
      secondStepInTransInv = restrictFormulaVariablesWithIntRange(secondStepInTransInv, pLocation);
    }

    // Construct Tr(x__CURR, x__CURR2)
    iterationFormula = iterationFormula.withPrevVarsSuffixed(CURR_KEYWORD);
    iterationFormula = iterationFormula.withCurrVarsSuffixed(CURR2_KEYWORD);

    boolean isTransitionInvariant;
    try {
      isTransitionInvariant =
          solver.implies(
              bfmgr.and(firstStepInTransInv, iterationFormula.getFormula()), secondStepInTransInv);

      // Check Tr(x__CURR, x__CURR2) => T(x__CURR, x__CURR2)
      candidateTransitionInvariant =
          candidateTransitionInvariant.withPrevVarsSuffixed(CURR_KEYWORD);

      isTransitionInvariant =
          isTransitionInvariant
              && solver.implies(
                  iterationFormula.getFormula(), candidateTransitionInvariant.getFormula());
    } catch (SolverException e) {
      logger.logDebugException(e);
      return false;
    }
    return isTransitionInvariant;
  }

  private ImmutableList<BooleanFormula> buildCycleFormula(
      Map<Integer, ImmutableSet<Formula>> storedValues, SSAMap pLatestValues, int pMaxIndex) {
    return buildComparingFormulas(storedValues, pMaxIndex, pLatestValues);
  }

  private ImmutableList<BooleanFormula> buildComparingFormulas(
      Map<Integer, ImmutableSet<Formula>> storedValues, int pMaxIndex, SSAMap pLatestValues) {
    ImmutableList.Builder<BooleanFormula> comparingFormulas = ImmutableList.builder();
    for (Entry<Integer, ImmutableSet<Formula>> savedVariables : storedValues.entrySet()) {
      if (savedVariables.getKey().intValue() >= pMaxIndex) {
        continue;
      }
      BooleanFormula comparingFormula = bfmgr.makeTrue();
      for (Formula oldVariable : savedVariables.getValue()) {
        comparingFormula =
            bfmgr.and(
                comparingFormula,
                fmgr.assignment(
                    fmgr.instantiate(fmgr.uninstantiate(oldVariable), pLatestValues), oldVariable));
      }
      comparingFormulas.add(comparingFormula);
    }
    return comparingFormulas.build();
  }
}
