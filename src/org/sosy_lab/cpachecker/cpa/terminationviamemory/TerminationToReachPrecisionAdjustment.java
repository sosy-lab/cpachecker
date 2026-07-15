// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.ExpressionTreeLocationTransitionInvariant;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.terminationviamemory")
public class TerminationToReachPrecisionAdjustment implements PrecisionAdjustment {
  private final Solver solver;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaManagerView fmgr;
  private final InterpolationManager itpMgr;
  private final PathFormulaManager pthfmgr;
  private final TerminationToReachStatistics statistics;
  private final CFA cfa;
  private final LogManager logger;
  private final ImmutableSet<ExpressionTreeLocationInvariant> candidateInvariants;
  private final boolean validation;

  private final String PREV_KEYWORD = "__TransInv@1";
  private final String CURR_KEYWORD = "__TransInv@2";
  private final String CURR2_KEYWORD = "__TransInv@3";
  private final String DUMMY_PREFIX = "";
  private static final long MAX_INT = 4294967295L;
  private static final long MIN_INT = -4294967295L;

  private final ImmutableSet<String> UFs =
      ImmutableSet.of("Integer_/_", "_%_", "_>>_", "_<<_", "_&_", "_!!_", "_~_", "_^_");

  @Option(
      secure = true,
      description =
          "There might be programs with unsigned integer overflow, and"
              + "due to the overflow, they are non-terminating."
              + "This option enforces the transition invariants to "
              + "have constraints to limit the mathematical integers.")
  private boolean addConstraintsToPreventOverflows = false;

  @Option(
      secure = true,
      description =
          "Some bitwise operations or modulo are UFs in integer encoding, and"
              + "they might lead to unsound results. If integer encoding is set,"
              + "we also have to use this option.")
  private boolean checkUFsInIntegerEncoding = false;

  @Option(
      secure = true,
      description =
          "Disables checks for fix-point with transition invariants and performs" + "plain BMC.")
  private boolean performBMC = false;

  public TerminationToReachPrecisionAdjustment(
      Solver pSolver,
      TerminationToReachStatistics pStatistics,
      LogManager plogger,
      CFA pCFA,
      BooleanFormulaManagerView pBfmgr,
      FormulaManagerView pFmgr,
      InterpolationManager pItpMgr,
      PathFormulaManager pPfmgr,
      Configuration pConfiguration,
      boolean pValidation,
      ImmutableSet<ExpressionTreeLocationInvariant> pCandidateInvariants)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    solver = pSolver;
    statistics = pStatistics;
    cfa = pCFA;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    logger = plogger;
    itpMgr = pItpMgr;
    pthfmgr = pPfmgr;
    candidateInvariants = pCandidateInvariants;
    validation = pValidation;
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

    if (terminationState.isLoopHead(location)
        && !terminationState.isLoopTerminating(location)
        && terminationState.getStoredValues().containsKey(keyPair)) {
      if (terminationState.getNumberOfIterationsAtLoopHead(keyPair) > 1) {
        boolean isTargetStateReachable;
        boolean isOverapproximating = false;
        PathFormula prefixPathFormula = terminationState.getPathFormulasForPrefix().orElseThrow();
        SSAMap largestIndices =
            terminationState.getPathFormulasForIteration().get(keyPair).getSsa();

        PartitionedRelationFormula iterationFormula =
            new PartitionedRelationFormula(
                terminationState.getPathFormulasForIteration().get(keyPair).getFormula(), fmgr);
        ImmutableMap<Integer, BooleanFormula> sameStateFormulas =
            buildCycleFormula(
                terminationState.getStoredValues().get(keyPair),
                largestIndices,
                terminationState.getNumberOfIterationsAtLoopHead(keyPair) - 1);

        // If the BMC queries are UNSAT, we try to compute transition invariant
        // We strengthen the transition invariant with the prefix formula
        PartitionedRelationFormula candidateTransInv =
            new PartitionedRelationFormula(bfmgr.makeFalse(), fmgr);
        while (true) {
          // First, check that the BMC check is UNSATs
          BooleanFormula latestSameStateFormula = bfmgr.makeTrue();
          for (Entry<Integer, BooleanFormula> sameStateFormula : sameStateFormulas.entrySet()) {
            try {
              // Construct formula:
              // T(x__PREV, x__CURR) and Tr(x__CURR, x__CURR2) and x__PREV = x_CURR2
              if (isOverapproximating) {
                // Construct formula instantiated to x__PREV = x__CURR2
                PartitionedRelationFormula sameStateFormulaRelation =
                    new PartitionedRelationFormula(sameStateFormula.getValue(), fmgr);
                sameStateFormulaRelation.extendPrevVarsWithPrefixSuffix(DUMMY_PREFIX, PREV_KEYWORD);
                sameStateFormulaRelation.extendCurrVarsWithPrefixSuffix(
                    DUMMY_PREFIX, CURR2_KEYWORD);
                latestSameStateFormula = sameStateFormulaRelation.getFormula();

                // Set the prev vars in T to match x__PREV and the curr cars to match x__CURR
                candidateTransInv.extendPrevVarsWithPrefixSuffix(DUMMY_PREFIX, PREV_KEYWORD);
                candidateTransInv.extendCurrVarsWithPrefixSuffix(DUMMY_PREFIX, CURR_KEYWORD);

                // Set the prev vars in Tr to match x__CURR and the curr cars to match x__CURR2
                iterationFormula.extendPrevVarsWithPrefixSuffix(DUMMY_PREFIX, CURR_KEYWORD);
                iterationFormula.extendCurrVarsWithPrefixSuffix(DUMMY_PREFIX, CURR2_KEYWORD);

                isTargetStateReachable =
                    !solver.isUnsat(
                        bfmgr.and(
                            candidateTransInv.getFormula(),
                            iterationFormula.getFormula(),
                            latestSameStateFormula));
              } else {
                isTargetStateReachable =
                    !solver.isUnsat(
                        bfmgr.and(
                            prefixPathFormula.getFormula(),
                            iterationFormula.getFormula(),
                            sameStateFormula.getValue()));
              }
            } catch (SolverException e) {
              logger.logDebugException(e);
              return Optional.of(result);
            }
            if (isTargetStateReachable) {
              if (!isOverapproximating && isSound(iterationFormula.getFormula())) {
                terminationState.makeTarget();
                terminationState.setNumberOfUnrollingsInTarget(sameStateFormula.getKey());
                result = result.withAbstractState(terminationState);
                statistics.setNonterminatingLoop(
                    cfa.getLoopStructure().orElseThrow().getLoopsForLoopHead(location));
                result = result.withAction(Action.BREAK);
              }
              return Optional.of(result);
            }
            latestSameStateFormula = sameStateFormula.getValue();
            if (isOverapproximating) {
              break;
            }
          }

          // If the user sets the algorithm to perform only BMC, then it does not try to reach the
          // fix-point
          if (performBMC) {
            break;
          }

          // Check the fix-point, i.e. check whether the new interpolant is a transition invariant
          if (isOverapproximating
              && isTransitionInvariant(candidateTransInv, iterationFormula, location)) {
            terminationState.setTerminatingIfAllNodesVisited(locationState.getLocationNode());
            terminationState.setCandidateTransitionInvariant(candidateTransInv);
            return Optional.of(result.withAbstractState(terminationState));
          }

          BooleanFormula interpolant;
          candidateTransInv.extendPrevVarsWithPrefixSuffix(DUMMY_PREFIX, PREV_KEYWORD);
          candidateTransInv.extendCurrVarsWithPrefixSuffix(DUMMY_PREFIX, CURR_KEYWORD);
          if (validation) {
            interpolant =
                collectCandidateTransitionInvariants(
                    location, terminationState.getPathFormulasForIteration().get(keyPair));
          } else {
            try {
              // If BMC check is UNSAT, try to overapproximate the transition invariant
              BooleanFormula firstStep =
                  isOverapproximating
                      ? candidateTransInv.getFormula()
                      : prefixPathFormula.getFormula();
              interpolant =
                  itpMgr
                      .interpolate(
                          ImmutableList.of(
                              bfmgr.and(firstStep, iterationFormula.getFormula()),
                              latestSameStateFormula))
                      .orElseThrow()
                      .getFirst();
            } catch (CPAException e) {
              break;
            }
          }

          // Instantiate the new interpolant to T(x__PREV, x__CURR)
          PartitionedRelationFormula newInterpolant =
              new PartitionedRelationFormula(interpolant, fmgr);
          newInterpolant.extendPrevVarsWithPrefixSuffix(DUMMY_PREFIX, PREV_KEYWORD);
          newInterpolant.extendCurrVarsWithPrefixSuffix(DUMMY_PREFIX, CURR_KEYWORD);
          try {
            if (containsOnlyIrrelevantVariables(interpolant, callstackState)
                || solver.implies(newInterpolant.getFormula(), candidateTransInv.getFormula())) {
              break;
            }
          } catch (SolverException | InterruptedException e) {
            break;
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

  private BooleanFormula collectCandidateTransitionInvariants(
      CFANode pLocation, PathFormula pIterationFormula) throws InterruptedException {
    BooleanFormula candidateTransitionInvariant = bfmgr.makeTrue();
    for (ExpressionTreeLocationInvariant invariant : candidateInvariants) {
      if (!(invariant instanceof ExpressionTreeLocationTransitionInvariant)) {
        continue;
      }

      if (invariant.getLocation().equals(pLocation)) {
        BooleanFormula invariantFormula;
        try {
          if (invariant.asExpressionTree().equals(ExpressionTrees.getTrue())) {
            invariantFormula = bfmgr.makeTrue();
          } else {
            invariantFormula = invariant.getFormula(fmgr, pthfmgr, pIterationFormula);
          }
        } catch (CPATransferException e) {
          invariantFormula = bfmgr.makeTrue();
        }
        candidateTransitionInvariant = bfmgr.and(candidateTransitionInvariant, invariantFormula);
      }
    }
    return candidateTransitionInvariant;
  }

  private boolean isSound(Formula pFormula) {
    if (!checkUFsInIntegerEncoding) {
      return true;
    }

    for (String function : fmgr.extractFunctionNames(pFormula)) {
      if (UFs.contains(function)) {
        return false;
      }
    }
    return true;
  }

  private BooleanFormula restrictFormulaVariablesWithIntRange(
      BooleanFormula pFormula, CFANode pLocation) {
    for (Formula variable : fmgr.extractVariables(pFormula).values()) {
      String pureVarName =
          TransitionInvariantUtils.removeTransInvKeyWord(
              TransitionInvariantUtils.removeFunctionFromVarsName(
                  fmgr.extractVariableNames(fmgr.uninstantiate(variable)).stream()
                      .findAny()
                      .orElseThrow()));
      for (AbstractSimpleDeclaration varDecl :
          cfa.getAstCfaRelation().getVariablesAndParametersInScope(pLocation).orElseThrow()) {
        if (varDecl.getName().equals(pureVarName)
            && (varDecl.getType() instanceof CSimpleType
                && !cfa.getMachineModel().isSigned(((CSimpleType) varDecl.getType())))) {
          pFormula =
              bfmgr.and(
                  pFormula,
                  fmgr.makeGreaterOrEqual(
                      fmgr.makeNumber(FormulaType.IntegerType, MAX_INT), variable, true));
          pFormula =
              bfmgr.and(
                  pFormula,
                  fmgr.makeLessOrEqual(
                      fmgr.makeNumber(FormulaType.IntegerType, MIN_INT), variable, true));
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
      CFANode pLocation) {
    boolean isTransitionInvariant;

    // The goal is to construct formula of the following form:
    // T(x__PREV, x__CURR) and Tr(x__CURR, x__CURR2) => T(x__PREV, x__CURR)

    // Construct T(x__PREV, x__CURR)
    candidateTransitionInvariant.extendPrevVarsWithPrefixSuffix(DUMMY_PREFIX, PREV_KEYWORD);
    candidateTransitionInvariant.extendCurrVarsWithPrefixSuffix(DUMMY_PREFIX, CURR_KEYWORD);
    BooleanFormula firstStepInTransInv = candidateTransitionInvariant.getFormula();

    // Construct T(x__PREV, x__CURR2)
    candidateTransitionInvariant.extendCurrVarsWithPrefixSuffix(DUMMY_PREFIX, CURR2_KEYWORD);
    BooleanFormula secondStepInTransInv = candidateTransitionInvariant.getFormula();

    if (addConstraintsToPreventOverflows) {
      firstStepInTransInv = restrictFormulaVariablesWithIntRange(firstStepInTransInv, pLocation);
      secondStepInTransInv = restrictFormulaVariablesWithIntRange(secondStepInTransInv, pLocation);
    }

    // Construct Tr(x__CURR, x__CURR2)
    iterationFormula.extendPrevVarsWithPrefixSuffix(DUMMY_PREFIX, CURR_KEYWORD);
    iterationFormula.extendCurrVarsWithPrefixSuffix(DUMMY_PREFIX, CURR2_KEYWORD);

    try {
      isTransitionInvariant =
          solver.implies(
              bfmgr.and(firstStepInTransInv, iterationFormula.getFormula()), secondStepInTransInv);

      // Check Init(x__CURR) and Tr(x__CURR, x__CURR2) => T(x__CURR, x__CURR2)
      candidateTransitionInvariant.extendPrevVarsWithPrefixSuffix(DUMMY_PREFIX, CURR_KEYWORD);

      isTransitionInvariant =
          isTransitionInvariant
              && solver.implies(
                  iterationFormula.getFormula(), candidateTransitionInvariant.getFormula());
    } catch (SolverException | InterruptedException e) {
      logger.logDebugException(e);
      return false;
    }
    if (isTransitionInvariant) {
      return true;
    }
    return false;
  }

  private ImmutableMap<Integer, BooleanFormula> buildCycleFormula(
      Map<Integer, ImmutableSet<Formula>> storedValues, SSAMap pLatestValues, int pMaxIndex) {
    return buildComparingFormulas(storedValues, pMaxIndex, pLatestValues);
  }

  private ImmutableMap<Integer, BooleanFormula> buildComparingFormulas(
      Map<Integer, ImmutableSet<Formula>> storedValues, int pMaxIndex, SSAMap pLatestValues) {
    ImmutableMap.Builder<Integer, BooleanFormula> comparingFormulas = ImmutableMap.builder();
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
      comparingFormulas.put(savedVariables.getKey(), comparingFormula);
    }
    return comparingFormulas.buildOrThrow();
  }
}
