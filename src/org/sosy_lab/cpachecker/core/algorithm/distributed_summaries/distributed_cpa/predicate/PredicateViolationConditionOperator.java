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

  /** Rewrites every condition as equivalent cubes, or {@code null} to keep it. */
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

  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    return finish(
        pARGPath.getFirstState(),
        prepend(initialCondition(pPreviousCondition), pARGPath.getFullPath()));
  }

  @Override
  public PathFormula initialCondition(Optional<ARGState> pPreviousCondition) {
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
    return result;
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
