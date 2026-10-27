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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.ExpressionTreeLocationTransitionInvariant;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Precision adjustment for the validation of termination witnesses. In addition to the transition
 * invariants found by {@link TerminationToReachPrecisionAdjustment}, it also uses the transition
 * invariants from the witness if they are inductive.
 */
public class TerminationToReachValidationPrecisionAdjustment
    extends TerminationToReachPrecisionAdjustment {

  private final PathFormulaManager pthfmgr;
  private final ImmutableSet<ExpressionTreeLocationInvariant> candidateInvariants;
  private final LogManager logger;

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
      throws InvalidConfigurationException, InterruptedException {
    super(
        pSolver,
        pStatistics,
        plogger,
        pCFA,
        pBfmgr,
        pFmgr,
        pItpMgr,
        pConfiguration,
        pAllLoops,
        collectSupportingInvariants(pCandidateInvariants, pFmgr, pPthfmgr, plogger));
    logger = plogger;
    pthfmgr = pPthfmgr;
    candidateInvariants = pCandidateInvariants;
  }

  /**
   * Conjoins the (supporting) invariants from the witness at each location. Their variables are
   * renamed to the variables of the state where the iteration starts. The witness is only used if
   * these invariants were proven to hold, see {@link TerminationToReachCPA}.
   */
  private static ImmutableMap<CFANode, BooleanFormula> collectSupportingInvariants(
      ImmutableSet<ExpressionTreeLocationInvariant> pCandidateInvariants,
      FormulaManagerView pFmgr,
      PathFormulaManager pPthfmgr,
      LogManager pLogger)
      throws InterruptedException {
    Map<CFANode, BooleanFormula> invariantsAtLocations = new HashMap<>();
    for (ExpressionTreeLocationInvariant invariant : pCandidateInvariants) {
      if (invariant instanceof ExpressionTreeLocationTransitionInvariant
          || invariant.asExpressionTree().equals(ExpressionTrees.getTrue())) {
        continue;
      }
      try {
        invariantsAtLocations.merge(
            invariant.getLocation(),
            invariant.getFormula(pFmgr, pPthfmgr, pPthfmgr.makeEmptyPathFormula()),
            pFmgr.getBooleanFormulaManager()::and);
      } catch (CPATransferException e) {
        // Ignoring an invariant only weakens the checks
        pLogger.logDebugException(e, "Could not convert the supporting invariant " + invariant);
      }
    }
    return ImmutableMap.copyOf(
        Maps.transformValues(
            invariantsAtLocations,
            invariant ->
                pFmgr.substitute(
                    invariant,
                    ImmutableMap.copyOf(
                        Maps.asMap(
                            ImmutableSet.copyOf(pFmgr.extractVariables(invariant).values()),
                            variable ->
                                pFmgr.makeVariable(
                                    pFmgr.getFormulaType(variable),
                                    pFmgr.uninstantiate(variable).toString() + CURR_KEYWORD))))));
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
      CFANode location)
      throws InterruptedException {
    ImmutableSet.Builder<PartitionedRelationFormula> builderTransitionInvariants =
        super.collectInductiveTransitionInvariants(terminationState, iterationFormula, location);

    // Add the predicates from the witness
    PartitionedRelationFormula invariantFromWitness =
        new PartitionedRelationFormula(collectCandidateTransitionInvariants(location), fmgr);
    if (isInductiveTransitionInvariant(invariantFromWitness, iterationFormula, location)) {
      builderTransitionInvariants.add(invariantFromWitness);
    }
    return builderTransitionInvariants;
  }

  /**
   * Returns no transition invariants. The inductive transition invariants may contain the one from
   * the witness, which is not irreflexive by construction. Without a fix-point, their conjunction
   * did not pass the check for a lasso in the overapproximation, so a state with them could be
   * covered before the unrolling finds a lasso.
   */
  @Override
  protected ImmutableSet<PartitionedRelationFormula> getTransitionInvariantsWithoutFixPoint(
      ImmutableSet<PartitionedRelationFormula> pInductiveTransitionInvariants) {
    return ImmutableSet.of();
  }

  /**
   * The transition invariants from the witness that are inductive are known before computing new
   * transition invariants. If their conjunction excludes a lasso, the fix-point is reached.
   */
  @Override
  protected Optional<PrecisionAdjustmentResult> checkFixPointWithKnownTransitionInvariants(
      ImmutableList<BooleanFormula> sameStateFormulas,
      PartitionedRelationFormula iterationFormula,
      PathFormula prefixPathFormula,
      CFANode location,
      TerminationToReachState terminationState,
      ImmutableSet.Builder<PartitionedRelationFormula> builderTransitionPredicates,
      ImmutableSet.Builder<PartitionedRelationFormula> builderTransitionInvariants,
      PrecisionAdjustmentResult result)
      throws InterruptedException {
    ImmutableSet<PartitionedRelationFormula> knownTransitionInvariants =
        builderTransitionInvariants.build();
    if (knownTransitionInvariants.isEmpty()) {
      return Optional.empty();
    }
    // Each of the transition invariants is inductive, so their conjunction is inductive as well
    PartitionedRelationFormula conjunction =
        new PartitionedRelationFormula(
            bfmgr.and(
                FluentIterable.from(knownTransitionInvariants)
                    .transform(PartitionedRelationFormula::getFormula)
                    .toList()),
            fmgr);
    try {
      if (findNonterminatingLoop(
              sameStateFormulas,
              true,
              Optional.of(conjunction),
              iterationFormula,
              prefixPathFormula,
              location)
          .isPresent()) {
        return Optional.empty();
      }
    } catch (SolverException e) {
      logger.logDebugException(e);
      return Optional.empty();
    }
    return checkFixPoint(
        true,
        conjunction,
        iterationFormula,
        location,
        terminationState,
        builderTransitionPredicates,
        builderTransitionInvariants,
        result);
  }

  /**
   * Conjoins all transition invariants from the witness at the given location into one formula. The
   * variables of the formula that do not belong to the previous state are renamed to variables of
   * the current state.
   */
  private BooleanFormula collectCandidateTransitionInvariants(CFANode pLocation)
      throws InterruptedException {
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
            invariantFormula = invariant.getFormula(fmgr, pthfmgr, pthfmgr.makeEmptyPathFormula());
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
