// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import static org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelationComponents.computeIntegratedInvariantFormula;
import static org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelationComponents.createComponentsFromSSAMap;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelationComponents;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class RankingRelationInvariantGenerator extends AbstractInvariantGenerator {

  private ReachedSet reachedSet;
  private final AggregatedReachedSets aggregatedReachedSets;
  private final CFA cfa;

  public RankingRelationInvariantGenerator(AggregatedReachedSets pAggregatedReachedSets, CFA pCFA) {
    this.aggregatedReachedSets = pAggregatedReachedSets;
    this.cfa = pCFA;
  }

  public void setReachedSet(ReachedSet pReachedSet) {
    this.reachedSet = pReachedSet;
  }

  protected Iterable<AbstractState> getReachedStates() {
    if (reachedSet == null) {
      return ImmutableList.of();
    } else {
      return reachedSet.asCollection();
    }
  }

  @Override
  protected void startImpl(CFANode pInitialLocation) {}

  @Override
  public boolean isProgramSafe() {
    return false;
  }

  @Override
  public void cancel() {}

  @Override
  public InvariantSupplier getSupplier() throws CPAException, InterruptedException {
    return new InvariantSupplier() {

      @Override
      public BooleanFormula getInvariantFor(
          CFANode node,
          Optional<CallstackStateEqualsWrapper> callstackInformation,
          FormulaManagerView fmgr,
          PathFormulaManager pfmgr,
          PathFormula pContext)
          throws InterruptedException {

        BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
        BooleanFormula reachedSetInvariant = bfmgr.makeTrue();
        BooleanFormula rankingRelationInvariant = bfmgr.makeTrue();

        Iterable<AbstractState> nodeStates =
            AbstractStates.filterLocation(getReachedStates(), node);

        for (AbstractState state : nodeStates) {

          PredicateAbstractState pas =
              AbstractStates.extractStateByType(state, PredicateAbstractState.class);
          if (pas == null) {
            continue;
          }

          BooleanFormula extractedState = AbstractStates.extractReportedFormulas(fmgr, state);
          reachedSetInvariant = bfmgr.and(reachedSetInvariant, extractedState);

          PathFormula currentPf = pas.getPathFormula();
          SSAMap ssaMap = currentPf.getSsa();

          if (ssaMap != null) {
            IntegerFormulaManagerView intgerFormulaManager = fmgr.getIntegerFormulaManager();
            IntegerFormula zero2 = intgerFormulaManager.makeNumber(0L);

            RankingRelationComponents componentsFromState =
                createComponentsFromSSAMap(ssaMap, fmgr, zero2);
            BooleanFormula computedInvariant =
                computeIntegratedInvariantFormula(componentsFromState, fmgr, zero2);

            rankingRelationInvariant = bfmgr.and(rankingRelationInvariant, computedInvariant);
          } else {
            rankingRelationInvariant = bfmgr.makeTrue();
          }
        }

        BooleanFormula finalInvariant = bfmgr.and(reachedSetInvariant, rankingRelationInvariant);

        return finalInvariant;
      }
    };
  }

  @Override
  public ExpressionTreeSupplier getExpressionTreeSupplier()
      throws CPAException, InterruptedException {
    return new ExpressionTreeInvariantSupplier(aggregatedReachedSets, cfa);
  }
}
