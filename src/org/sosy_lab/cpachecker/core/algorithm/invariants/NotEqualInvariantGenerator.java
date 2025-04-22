// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class NotEqualInvariantGenerator extends AbstractInvariantGenerator
    implements ReachedSetNotEqual {

  private ReachedSet reachedSet;

  private final AggregatedReachedSets aggregatedReachedSets;
  private final CFA cfa;

  public NotEqualInvariantGenerator(AggregatedReachedSets pAggregatedReachedSets, CFA pCFA) {
    this.aggregatedReachedSets = pAggregatedReachedSets;
    this.cfa = pCFA;

  }

  @Override
  public void setReachedSet(ReachedSet pReachedSet) {
    this.reachedSet = pReachedSet;

  }

  protected Iterable<AbstractState> getReachedStates() {
    if (reachedSet == null) {

      return java.util.Collections.emptyList();
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
        Map<String, Integer> lastIndices = new HashMap<>();
        List<BooleanFormula> inequalityConstraints = new ArrayList<>();

        Iterable<AbstractState> nodeState = AbstractStates.filterLocation(getReachedStates(), node);

        for (AbstractState state : nodeState) {
          PredicateAbstractState pas =
              AbstractStates.extractStateByType(state, PredicateAbstractState.class);
          if (pas == null) {
            continue;
          }
          PathFormula currentPf = pas.getPathFormula();
          SSAMap currentSSAMap = currentPf.getSsa();

          for (String var : currentSSAMap.allVariables()) {
            int currentIndex = currentSSAMap.getIndex(var);

            if (lastIndices.containsKey(var)) {
              int prevIndex = lastIndices.get(var);

              if (prevIndex != currentIndex) {

                String oldVarName = var + "____" + prevIndex;
                String curVarName = var + "____" + currentIndex;

                BooleanFormula oldVarFormula = bfmgr.makeVariable(oldVarName);
                BooleanFormula curVarFormula = bfmgr.makeVariable(curVarName);

                BooleanFormula inequality =
                    fmgr.makeNot(fmgr.makeEqual(oldVarFormula, curVarFormula));
                inequalityConstraints.add(inequality);
              }
            }

            lastIndices.put(var, currentIndex);
          }
        }

        BooleanFormula notEaulInvariant =
            inequalityConstraints.isEmpty() ? bfmgr.makeTrue() : bfmgr.and(inequalityConstraints);

        return notEaulInvariant;
      }
    };
  }

  @Override
  public ExpressionTreeSupplier getExpressionTreeSupplier()
      throws CPAException, InterruptedException {
    return new ExpressionTreeInvariantSupplier(aggregatedReachedSets, cfa);
  }
}
