/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.collect.FluentIterable.from;

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

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;


public class ExpressionTreeLocationInvariant extends AbstractLocationFormulaInvariant
    implements ExpressionTreeCandidateInvariant {

  private final ExpressionTree<AExpression> expressionTree;

  private final CFANode location;

  private final String groupId;

  private final @Nullable Map<ManagerKey, ToFormulaVisitor> visitorCache;

  public ExpressionTreeLocationInvariant(
      String pGroupId, CFANode pLocation, ExpressionTree<AExpression> pExpressionTree) {
    this(pGroupId, pLocation, pExpressionTree, null);
  }

  public ExpressionTreeLocationInvariant(
      String pGroupId,
      CFANode pLocation,
      ExpressionTree<AExpression> pExpressionTree,
      @Nullable Map<ManagerKey, ToFormulaVisitor> pVisitorCache) {
    super(pLocation);
    groupId = Objects.requireNonNull(pGroupId);
    location = Objects.requireNonNull(pLocation);
    expressionTree = Objects.requireNonNull(pExpressionTree);
    visitorCache = pVisitorCache;
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
      throws CPATransferException, InterruptedException {
    ManagerKey key = null;
    PathFormula clearContext = pContext == null ? null : pPFMGR.makeEmptyPathFormula(pContext);
    ToFormulaVisitor toFormulaVisitor = null;
    if (visitorCache != null) {
      key = new ManagerKey(pFMGR, pPFMGR, clearContext);
      toFormulaVisitor = visitorCache.get(key);
    }
    if (toFormulaVisitor == null) {
      toFormulaVisitor = new ToFormulaVisitor(pFMGR, pPFMGR, clearContext);
      if (visitorCache != null) {
        visitorCache.put(key, toFormulaVisitor);
      }
    }
    try {
      return expressionTree.accept(toFormulaVisitor);
    } catch (ToFormulaException e) {
      if (e.isInterruptedException()) {
        throw e.asInterruptedException();
      }
      throw e.asTransferException();
    }
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    if (expressionTree.equals(ExpressionTrees.getFalse())) {
      Iterable<AbstractState> infeasibleStates = AbstractStates.filterLocations(pReachedSet, getLocations()).toList();
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
    return groupId + " at " + location + ": " + expressionTree.toString();
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

  public CFANode getLocation() {
    return location;
  }

}
