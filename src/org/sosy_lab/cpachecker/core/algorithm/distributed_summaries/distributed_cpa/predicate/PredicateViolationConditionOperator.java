// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssARGPathGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.MergeableViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/** Computes violation conditions as backward path formulas and merges them by disjunction. */
public class PredicateViolationConditionOperator
    implements MergeableViolationConditionOperator<PathFormula> {

  private final PredicateCPA cpa;
  private final PathFormulaManagerImpl backwardManager;

  /**
   * Whether to drop unsatisfiable conditions. The root block has to, because it has no predecessor
   * that would refute them.
   */
  private final boolean checkSatisfiability;

  /** Rewrites every condition as equivalent cubes, or {@code null} to keep it. */
  private final @Nullable ModelBasedGeneralization generalization;

  /** Projects the local variables out of every condition, or {@code null} to keep them. */
  private final @Nullable ExistentialProjection projection;

  /**
   * Projects the local variables out of a condition merged over a graph before it is generalized,
   * or {@code null} to keep them.
   */
  private final @Nullable ExistentialProjection graphProjection;

  public PredicateViolationConditionOperator(
      PredicateCPA pCpa,
      PathFormulaManagerImpl pBackwardManager,
      boolean pCheckSatisfiability,
      @Nullable ModelBasedGeneralization pGeneralization,
      @Nullable ExistentialProjection pProjection,
      @Nullable ExistentialProjection pGraphProjection) {
    cpa = pCpa;
    backwardManager = pBackwardManager;
    checkSatisfiability = pCheckSatisfiability;
    generalization = pGeneralization;
    projection = pProjection;
    graphProjection = pGraphProjection;
  }

  @Override
  public PathFormula initialCondition(ARGState pTarget, Optional<ARGState> pPreviousCondition)
      throws InterruptedException {
    PathFormula result;
    if (pPreviousCondition.isEmpty()) {
      result = backwardManager.makeEmptyPathFormula();
    } else {
      PredicateAbstractState counterexampleState =
          Objects.requireNonNull(
              AbstractStates.extractStateByType(
                  pPreviousCondition.orElseThrow(), PredicateAbstractState.class));
      if (counterexampleState.isAbstractionState()) {
        result = counterexampleState.getAbstractionFormula().getBlockFormula();
      } else {
        result = counterexampleState.getPathFormula();
      }
    }
    // Walking backwards has to know the memory layout that the forward analysis of this block
    // discovered up to the state the walk starts from. Starting from an empty pointer-target set
    // loses the bases and the tracked fields, so a write through a pointer to a field of a
    // composite cannot be encoded and the condition comes out unsatisfiable even though the path
    // is feasible. The forward state carries that layout, so seed the walk with it.
    return withPointerTargetSetOf(pTarget, result);
  }

  /**
   * Adds the pointer-target set the forward analysis built up to {@code pStart} to the context of
   * {@code pFormula}. The forward state knows which bases exist and which fields are tracked; the
   * backward walk that starts there needs the same knowledge to encode accesses to them.
   */
  private PathFormula withPointerTargetSetOf(ARGState pStart, PathFormula pFormula)
      throws InterruptedException {
    PredicateAbstractState predicateState =
        AbstractStates.extractStateByType(pStart, PredicateAbstractState.class);
    if (predicateState == null) {
      return pFormula;
    }
    PointerTargetSet forward = predicateState.getPathFormula().getPointerTargetSet();
    SSAMapBuilder ssa = pFormula.getSsa().builder();
    PointerTargetSet merged =
        backwardManager.mergePts(pFormula.getPointerTargetSet(), forward, ssa);
    return pFormula.withContext(ssa.build(), merged);
  }

  @Override
  public Optional<PathFormula> prepend(PathFormula pCondition, List<CFAEdge> pEdges)
      throws InterruptedException, CPATransferException {
    PathFormula formula = pCondition;
    for (CFAEdge edge : pEdges.reversed()) {
      formula = backwardManager.makeAnd(formula, edge);
    }
    return Optional.of(formula);
  }

  @Override
  public Optional<PathFormula> merge(PathFormula pFirst, PathFormula pSecond)
      throws InterruptedException {
    return Optional.of(backwardManager.makeOr(pFirst, pSecond));
  }

  @Override
  public Optional<AbstractState> finish(
      ARGPath pPath, Optional<ARGState> pPreviousCondition, PathFormula pCondition)
      throws InterruptedException, SolverException {
    ARGState root = pPath.getFirstState();
    PathFormula result = pCondition;
    if (graphProjection != null && pPath instanceof DssARGPathGraph) {
      result = graphProjection.projectLocalVariables(result);
    }
    if (generalization != null) {
      // The precondition supplies a vocabulary only. Generalization must preserve every entry
      // state of the condition, including states outside the current precondition.
      Optional<PathFormula> generalized = generalization.generalize(result, preconditionOf(root));
      if (generalized.isPresent()) {
        if (cpa.getSolver()
            .getFormulaManager()
            .getBooleanFormulaManager()
            .isFalse(generalized.orElseThrow().getFormula())) {
          // The exact violation condition is unsatisfiable.
          return Optional.empty();
        }
        result = generalized.orElseThrow();
      }
    }
    if (projection != null) {
      // Only the variables at their latest SSA index describe the block entry. All others are
      // intermediate values of the path or of the previous condition, which a block using this
      // condition renames apart anyway. Removing them keeps the condition from growing with every
      // block it passes and lets conditions of different paths become equal.
      result = projection.projectLocalVariables(result);
    }
    if (checkSatisfiability && cpa.getSolver().isUnsat(result.getFormula())) {
      return Optional.empty();
    }
    return Optional.of(
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
            result,
            (PredicateAbstractState)
                cpa.getInitialState(
                    Objects.requireNonNull(AbstractStates.extractLocation(root)),
                    StateSpacePartition.getDefaultPartition())));
  }

  private BooleanFormula preconditionOf(ARGState pStart) {
    PredicateAbstractState start =
        Objects.requireNonNull(
            AbstractStates.extractStateByType(pStart, PredicateAbstractState.class));
    if (start.isAbstractionState()) {
      return start.getAbstractionFormula().asFormula();
    }
    return cpa.getSolver().getFormulaManager().getBooleanFormulaManager().makeTrue();
  }
}
