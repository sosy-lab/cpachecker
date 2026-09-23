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

public class PredicateViolationConditionOperator
    implements MergeableViolationConditionOperator<PathFormula> {

  private final PathFormulaManagerImpl backwardManager;
  private final PredicateCPA cpa;
  private final @Nullable ExistentialProjection graphProjection;
  private final boolean hasRootAsPredecessor;

  /** Projects the local variables out of every condition, or {@code null} to keep them. */
  private final @Nullable ExistentialProjection projection;

  /** Rewrites every condition as cubes relative to the precondition, or {@code null} to not. */
  private final @Nullable ModelBasedGeneralization generalization;

  public PredicateViolationConditionOperator(
      PathFormulaManagerImpl pBackwardManager,
      PredicateCPA pCpa,
      boolean pHasRootAsPredecessor,
      @Nullable ExistentialProjection pProjection,
      boolean pProjectNestedDisjunctions,
      @Nullable ModelBasedGeneralization pGeneralization) {
    backwardManager = pBackwardManager;
    cpa = pCpa;
    graphProjection =
        pProjection == null
            ? null
            : new ExistentialProjection(cpa.getSolver(), pProjectNestedDisjunctions);
    hasRootAsPredecessor = pHasRootAsPredecessor;
    projection = pProjection;
    generalization = pGeneralization;
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
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    return finish(
        pARGPath.getFirstState(),
        prepend(
            initialCondition(pARGPath.getLastState(), pPreviousCondition), pARGPath.getFullPath()));
  }

  @Override
  public PathFormula initialCondition(ARGState pStart, Optional<ARGState> pPreviousCondition)
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
    return withPointerTargetSetOf(pStart, result);
  }

  @Override
  public PathFormula prepend(PathFormula formula, List<CFAEdge> edges)
      throws InterruptedException, CPATransferException {
    for (CFAEdge edge : edges.reversed()) {
      formula = backwardManager.makeAnd(formula, edge);
    }
    return formula;
  }

  @Override
  public PathFormula union(PathFormula first, PathFormula second) throws InterruptedException {
    return backwardManager.makeOr(first, second);
  }

  @Override
  public Optional<AbstractState> finishGraph(ARGState root, PathFormula result)
      throws InterruptedException, SolverException {
    return finish(
        root, graphProjection == null ? result : graphProjection.projectLocalVariables(result));
  }

  public Optional<AbstractState> finish(ARGState root, PathFormula result)
      throws InterruptedException, SolverException {
    if (generalization != null) {
      // The path starts in the precondition the block was explored from. Predecessors only ever
      // ask about states of their postconditions, which make up this precondition, so the
      // condition only has to be exact there.
      Optional<PathFormula> generalized = generalization.generalize(result, preconditionOf(root));
      if (generalized.isPresent()) {
        if (cpa.getSolver()
            .getFormulaManager()
            .getBooleanFormulaManager()
            .isFalse(generalized.orElseThrow().getFormula())) {
          // no state of the precondition takes this path to the violation
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
    if (hasRootAsPredecessor) {
      if (cpa.getSolver().isUnsat(result.getFormula())) {
        return Optional.empty();
      }
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
