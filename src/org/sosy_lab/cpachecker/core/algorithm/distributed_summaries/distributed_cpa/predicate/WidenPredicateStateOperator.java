// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen.WidenOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class WidenPredicateStateOperator implements WidenOperator {

  private final PredicateCPA predicateCPA;

  public WidenPredicateStateOperator(PredicateCPA pCPA) {
    predicateCPA = pCPA;
  }

  @Override
  public AbstractState combine(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    PredicateAbstractState predicateState1 = (PredicateAbstractState) state1;
    PredicateAbstractState predicateState2 = (PredicateAbstractState) state2;
    Preconditions.checkArgument(predicateState1.isAbstractionState());
    Preconditions.checkArgument(predicateState2.isAbstractionState());
    PathFormula pathFormula =
        predicateCPA
            .getPathFormulaManager()
            .makeOr(predicateState1.getPathFormula(), predicateState2.getPathFormula());
    BooleanFormulaManagerView bmgr =
        predicateCPA.getSolver().getFormulaManager().getBooleanFormulaManager();
    BooleanFormula abstraction =
        bmgr.or(
            predicateState1.getAbstractionFormula().asInstantiatedFormula(),
            predicateState2.getAbstractionFormula().asInstantiatedFormula());
    return PredicateAbstractState.mkAbstractionState(
        pathFormula,
        new AbstractionFormula(
            predicateCPA.getSolver().getFormulaManager(),
            predicateState2.getAbstractionFormula().asRegion(),
            abstraction,
            predicateCPA.getSolver().getFormulaManager().uninstantiate(abstraction),
            pathFormula,
            Sets.union(
                predicateState1.getAbstractionFormula().getIdsOfStoredAbstractionReused(),
                predicateState2.getAbstractionFormula().getIdsOfStoredAbstractionReused())),
        PathCopyingPersistentTreeMap.copyOf(
            ImmutableMap.<CFANode, Integer>builder()
                .putAll(predicateState1.getAbstractionLocationsOnPath())
                .putAll(predicateState2.getAbstractionLocationsOnPath())
                .buildKeepingLast()));
  }
}
