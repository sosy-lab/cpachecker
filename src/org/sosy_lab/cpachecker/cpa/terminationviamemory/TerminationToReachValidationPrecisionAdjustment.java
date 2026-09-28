// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.CURR_KEYWORD;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.TRANS_INV_KEYWORD;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.ExpressionTreeLocationTransitionInvariant;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Precision adjustment for the validation of termination witnesses. In addition to the transition
 * invariants found by {@link TerminationToReachPrecisionAdjustment}, it also uses the transition
 * invariants from the witness if they are inductive.
 */
public class TerminationToReachValidationPrecisionAdjustment
    extends TerminationToReachPrecisionAdjustment {

  private final PathFormulaManager pthfmgr;
  private final ImmutableSet<ExpressionTreeLocationInvariant> candidateInvariants;

  public TerminationToReachValidationPrecisionAdjustment(
      Solver pSolver,
      TerminationToReachStatistics pStatistics,
      LogManager plogger,
      CFA pCFA,
      BooleanFormulaManagerView pBfmgr,
      FormulaManagerView pFmgr,
      PathFormulaManager pPthfmgr,
      InterpolationManager pItpMgr,
      Configuration pConfiguration,
      ImmutableSet<Loop> pAllLoops,
      ImmutableSet<ExpressionTreeLocationInvariant> pCandidateInvariants)
      throws InvalidConfigurationException {
    super(pSolver, pStatistics, plogger, pCFA, pBfmgr, pFmgr, pItpMgr, pConfiguration, pAllLoops);
    pthfmgr = pPthfmgr;
    candidateInvariants = pCandidateInvariants;
  }

  /**
   * Collects the inductive transition invariants as {@link
   * TerminationToReachPrecisionAdjustment#collectInductiveTransitionInvariants} does, and adds the
   * transition invariant from the witness as well, if it is inductive.
   */
  @Override
  protected ImmutableSet.Builder<PartitionedRelationFormula> collectInductiveTransitionInvariants(
      TerminationToReachState terminationState,
      PartitionedRelationFormula iterationFormula,
      CFANode location,
      Pair<LocationState, CallstackState> keyPair)
      throws InterruptedException {
    ImmutableSet.Builder<PartitionedRelationFormula> builderTransitionInvariants =
        super.collectInductiveTransitionInvariants(
            terminationState, iterationFormula, location, keyPair);

    // Add the predicates from the witness
    PartitionedRelationFormula invariantFromWitness =
        new PartitionedRelationFormula(
            collectCandidateTransitionInvariants(
                location, terminationState.getPathFormulasForIteration().get(keyPair)),
            fmgr);
    if (isInductiveTransitionInvariant(invariantFromWitness, iterationFormula, location)) {
      builderTransitionInvariants.add(invariantFromWitness);
    }
    return builderTransitionInvariants;
  }

  /**
   * Conjoins all transition invariants from the witness at the given location into one formula. The
   * variables of the formula that do not belong to the previous state are renamed to variables of
   * the current state.
   */
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
    candidateTransitionInvariant =
        fmgr.substitute(
            candidateTransitionInvariant,
            ImmutableMap.copyOf(
                Maps.asMap(
                    fmgr.extractVariables(candidateTransitionInvariant).values().stream()
                        .filter(variable -> !variable.toString().contains(TRANS_INV_KEYWORD))
                        .collect(ImmutableSet.toImmutableSet()),
                    variable ->
                        fmgr.makeVariable(
                            fmgr.getFormulaType(variable),
                            TransitionInvariantUtils.removeKeyWordAfterTransInv(
                                    fmgr.uninstantiate(variable).toString())
                                + CURR_KEYWORD))));
    return candidateTransitionInvariant;
  }
}
