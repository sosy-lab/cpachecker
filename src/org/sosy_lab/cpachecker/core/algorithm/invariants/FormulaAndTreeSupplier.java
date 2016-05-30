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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractReportedFormulas;

import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.util.Objects;
import java.util.Set;

public class FormulaAndTreeSupplier implements InvariantSupplier, ExpressionTreeSupplier {

  private final InvariantSupplier invariantSupplier;

  private final ExpressionTreeSupplier expressionTreeSupplier;

  public FormulaAndTreeSupplier(InvariantSupplier pInvariantSupplier, ExpressionTreeSupplier pExpressionTreeSupplier) {
    this.invariantSupplier = pInvariantSupplier;
    this.expressionTreeSupplier = pExpressionTreeSupplier;
  }

  public FormulaAndTreeSupplier(LazyLocationMapping pLazyLocationMapping, CFA pCfa) {
    invariantSupplier = new ReachedSetBasedInvariantSupplier(pLazyLocationMapping);
    expressionTreeSupplier = new ReachedSetBasedExpressionTreeSupplier(pLazyLocationMapping, pCfa);
  }

  @Override
  public ExpressionTree<Object> getInvariantFor(CFANode pNode) {
    return expressionTreeSupplier.getInvariantFor(pNode);
  }

  @Override
  public BooleanFormula getInvariantFor(
      CFANode pNode, FormulaManagerView pFmgr, PathFormulaManager pPfmgr, PathFormula pContext) {
    return invariantSupplier.getInvariantFor(pNode, pFmgr, pPfmgr, pContext);
  }

  /**
   * {@link InvariantSupplier} that extracts invariants from a {@link ReachedSet}
   * with {@link FormulaReportingState}s.
   */
  public static class ReachedSetBasedInvariantSupplier implements InvariantSupplier {

    private final LazyLocationMapping lazyLocationMapping;

    public ReachedSetBasedInvariantSupplier(LazyLocationMapping pLazyLocationMapping) {
      lazyLocationMapping = Objects.requireNonNull(pLazyLocationMapping);
    }

    @Override
    public BooleanFormula getInvariantFor(
        CFANode pLocation,
        FormulaManagerView fmgr,
        PathFormulaManager pfmgr,
        PathFormula pContext) {
      BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
      BooleanFormula invariant = bfmgr.makeBoolean(false);

      for (AbstractState locState : lazyLocationMapping.get(pLocation)) {
        invariant = bfmgr.or(invariant, extractReportedFormulas(fmgr, locState, pfmgr));
      }
      return invariant;
    }
  }

  private static class ReachedSetBasedExpressionTreeSupplier implements ExpressionTreeSupplier {

    private final LazyLocationMapping lazyLocationMapping;
    private final CFA cfa;

    ReachedSetBasedExpressionTreeSupplier(LazyLocationMapping pLazyLocationMapping, CFA pCFA) {
      lazyLocationMapping = Objects.requireNonNull(pLazyLocationMapping);
      cfa = Objects.requireNonNull(pCFA);
    }

    @Override
    public ExpressionTree<Object> getInvariantFor(CFANode pLocation) {
      ExpressionTree<Object> locationInvariant = ExpressionTrees.getFalse();

      Set<InvariantsState> invStates = Sets.newHashSet();
      boolean otherReportingStates = false;

      for (AbstractState locState : lazyLocationMapping.get(pLocation)) {
        ExpressionTree<Object> stateInvariant = ExpressionTrees.getTrue();

        for (ExpressionTreeReportingState expressionTreeReportingState :
            AbstractStates.asIterable(locState).filter(ExpressionTreeReportingState.class)) {
          if (expressionTreeReportingState instanceof InvariantsState) {
            InvariantsState invState = (InvariantsState) expressionTreeReportingState;
            boolean skip = false;
            for (InvariantsState other : invStates) {
              if (invState.isLessOrEqual(other)) {
                skip = true;
                break;
              }
            }
            if (skip) {
              stateInvariant = ExpressionTrees.getFalse();
              continue;
            }
            invStates.add(invState);
          } else {
            otherReportingStates = true;
          }
          stateInvariant =
              And.of(
                  stateInvariant,
                  expressionTreeReportingState.getFormulaApproximation(
                      cfa.getFunctionHead(pLocation.getFunctionName()), pLocation));
        }

        locationInvariant = Or.of(locationInvariant, stateInvariant);
      }

      if (!otherReportingStates && invStates.size() > 1) {
        Set<InvariantsState> newInvStates = Sets.newHashSet();
        for (InvariantsState a : invStates) {
          boolean skip = false;
          for (InvariantsState b : invStates) {
            if (a != b && a.isLessOrEqual(b)) {
              skip = true;
              break;
            }
          }
          if (!skip) {
            newInvStates.add(a);
          }
        }
        if (newInvStates.size() < invStates.size()) {
          locationInvariant = ExpressionTrees.getFalse();
          for (InvariantsState state : newInvStates) {
            locationInvariant =
                Or.of(
                    locationInvariant,
                    state.getFormulaApproximation(
                        cfa.getFunctionHead(pLocation.getFunctionName()), pLocation));
          }
        }
      }

      return locationInvariant;
    }
  }
}