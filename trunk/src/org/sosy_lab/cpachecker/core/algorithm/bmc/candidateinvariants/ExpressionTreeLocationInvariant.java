// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class ExpressionTreeLocationInvariant extends SingleLocationFormulaInvariant
    implements ExpressionTreeCandidateInvariant {

  private final ExpressionTree<AExpression> expressionTree;

  private final CFANode location;

  private final String groupId;

  private final ConcurrentMap<ManagerKey, ToFormulaVisitor> visitorCache;

  public ExpressionTreeLocationInvariant(
      String pGroupId,
      CFANode pLocation,
      ExpressionTree<AExpression> pExpressionTree,
      ConcurrentMap<ManagerKey, ToFormulaVisitor> pVisitorCache) {
    super(pLocation);
    groupId = Objects.requireNonNull(pGroupId);
    location = Objects.requireNonNull(pLocation);
    expressionTree = Objects.requireNonNull(pExpressionTree);
    visitorCache = checkNotNull(pVisitorCache);
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
      throws CPATransferException, InterruptedException {
    PathFormula clearContext =
        pContext == null ? null : pPFMGR.makeEmptyPathFormulaWithContextFrom(pContext);
    ManagerKey key = new ManagerKey(pFMGR, pPFMGR, clearContext);
    ToFormulaVisitor toFormulaVisitor =
        visitorCache.computeIfAbsent(
            key,
            k -> new ToFormulaVisitor(k.formulaManagerView, k.pathFormulaManager, k.clearContext));
    try {
      return expressionTree.accept(toFormulaVisitor);
    } catch (ToFormulaException e) {
      Throwables.propagateIfPossible(
          e.getCause(), CPATransferException.class, InterruptedException.class);
      throw new UnexpectedCheckedException("expression tree to formula", e);
    }
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    if (expressionTree.equals(ExpressionTrees.getFalse())) {
      Iterable<AbstractState> infeasibleStates =
          ImmutableList.copyOf(AbstractStates.filterLocation(pReachedSet, getLocation()));
      pReachedSet.removeAll(infeasibleStates);
      for (ARGState s : from(infeasibleStates).filter(ARGState.class)) {
        s.removeFromARG();
      }
    }
  }

  @Override
  public ExpressionTree<Object> asExpressionTree() {
    return ExpressionTrees.cast(expressionTree);
  }

  public String getGroupId() {
    return groupId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, location, expressionTree, System.identityHashCode(visitorCache));
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof ExpressionTreeLocationInvariant) {
      ExpressionTreeLocationInvariant other = (ExpressionTreeLocationInvariant) pObj;
      return groupId.equals(other.groupId)
          && location.equals(other.location)
          && expressionTree.equals(other.expressionTree)
          && visitorCache == other.visitorCache;
    }
    return false;
  }

  @Override
  public String toString() {
    return groupId + " at " + location + ": " + expressionTree;
  }

  public static class ManagerKey {

    private final FormulaManagerView formulaManagerView;

    private final PathFormulaManager pathFormulaManager;

    private final PathFormula clearContext;

    public ManagerKey(
        FormulaManagerView pFormulaManagerView,
        PathFormulaManager pPathFormulaManager,
        PathFormula pClearContext) {
      formulaManagerView = Objects.requireNonNull(pFormulaManagerView);
      pathFormulaManager = Objects.requireNonNull(pPathFormulaManager);
      clearContext = pClearContext;
    }

    @Override
    public int hashCode() {
      return Objects.hash(formulaManagerView, pathFormulaManager);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof ManagerKey) {
        ManagerKey other = (ManagerKey) pObj;
        return formulaManagerView == other.formulaManagerView
            && pathFormulaManager == other.pathFormulaManager
            && Objects.equals(clearContext, other.clearContext);
      }
      return false;
    }
  }
}
