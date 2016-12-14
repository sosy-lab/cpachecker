/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AssumptionReportingState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Transfer relation and strengthening for the DumpInvariant CPA
 */
public class AssumptionStorageTransferRelation extends SingleEdgeTransferRelation {

  private final CtoFormulaConverter converter;
  private final FormulaManagerView formulaManager;

  private final Collection<AbstractState> topStateSet;

  public AssumptionStorageTransferRelation(CtoFormulaConverter pConverter,
      FormulaManagerView pFormulaManager, AbstractState pTopState) {
    converter = pConverter;
    formulaManager = pFormulaManager;
    topStateSet = Collections.singleton(pTopState);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge) {
    AssumptionStorageState element = (AssumptionStorageState)pElement;

    // If we must stop, then let's stop by returning an empty set
    if (element.isStop()) {
      return Collections.emptySet();
    }

    return topStateSet;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState el, List<AbstractState> others, CFAEdge edge, Precision p) throws CPATransferException, InterruptedException {
    AssumptionStorageState asmptStorageElem = (AssumptionStorageState)el;
    BooleanFormulaManagerView bfmgr = formulaManager.getBooleanFormulaManager();
    assert bfmgr.isTrue(asmptStorageElem.getAssumption());
    assert bfmgr.isTrue(asmptStorageElem.getStopFormula());
    final CFANode currentLocation =
        Iterables.getOnlyElement(AbstractStates.extractLocations(others));
    String function = currentLocation.getFunctionName();

    BooleanFormula assumption =  bfmgr.makeTrue();
    BooleanFormula stopFormula = bfmgr.makeFalse(); // initialize with false because we create a
    // disjunction

    // process stop flag
    boolean stop = false;

    for (AbstractState element : AbstractStates.asFlatIterable(others)) {
      if (element instanceof AssumptionReportingState) {
        List<CExpression> assumptions = ((AssumptionReportingState)element).getAssumptions();
        for (CExpression inv : assumptions) {
          BooleanFormula invFormula = converter.makePredicate(inv, edge, function, SSAMap.emptySSAMap().builder());
          assumption = bfmgr.and(assumption, formulaManager.uninstantiate(invFormula));
        }
      }

      if (element instanceof AvoidanceReportingState) {
        AvoidanceReportingState e = (AvoidanceReportingState)element;

        if (e.mustDumpAssumptionForAvoidance()) {
          stopFormula = bfmgr.or(stopFormula, e.getReasonFormula(formulaManager));
          stop = true;
        }
      }
    }
    Preconditions.checkState(!bfmgr.isTrue(stopFormula));

    if (!stop) {
      stopFormula = bfmgr.makeTrue();
    }

    if (bfmgr.isTrue(assumption) && bfmgr.isTrue(stopFormula)) {
      return Collections.singleton(el); // nothing has changed

    } else {
      return Collections.singleton(new AssumptionStorageState(formulaManager, assumption, stopFormula));
    }
  }
}