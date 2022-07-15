// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AssumptionReportingState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** Transfer relation and strengthening for the DumpInvariant CPA */
public class AssumptionStorageTransferRelation extends SingleEdgeTransferRelation {

  private final CtoFormulaConverter converter;
  private final FormulaManagerView formulaManager;

  private final Collection<AbstractState> topStateSet;

  public AssumptionStorageTransferRelation(
      CtoFormulaConverter pConverter, FormulaManagerView pFormulaManager, AbstractState pTopState) {
    converter = pConverter;
    formulaManager = pFormulaManager;
    topStateSet = Collections.singleton(pTopState);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge) {
    AssumptionStorageState element = (AssumptionStorageState) pElement;

    // If we must stop, then let's stop by returning an empty set
    if (element.isStop()) {
      return ImmutableSet.of();
    }

    return topStateSet;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState el, Iterable<AbstractState> others, CFAEdge edge, Precision p)
      throws CPATransferException, InterruptedException {
    AssumptionStorageState asmptStorageElem = (AssumptionStorageState) el;
    return Collections.singleton(strengthen(asmptStorageElem, others, edge));
  }

  AssumptionStorageState strengthen(
      AssumptionStorageState pAsmptStorageElem, Iterable<AbstractState> pOthers, CFAEdge pEdge)
      throws UnrecognizedCodeException, InterruptedException {
    BooleanFormulaManagerView bfmgr = formulaManager.getBooleanFormulaManager();

    String function = pEdge.getSuccessor().getFunctionName();

    BooleanFormula assumption = pAsmptStorageElem.getAssumption();
    BooleanFormula stopFormula = pAsmptStorageElem.getStopFormula();
    if (bfmgr.isTrue(stopFormula)) {
      // if there is no avoidance condition,
      // initialize with false because we create a disjunction over possible
      // new conditions below
      stopFormula = bfmgr.makeFalse();
    }

    // process stop flag
    boolean stop = false;

    for (AbstractState element : AbstractStates.asFlatIterable(pOthers)) {
      if (element instanceof AssumptionReportingState) {
        List<CExpression> assumptions = ((AssumptionReportingState) element).getAssumptions();
        for (CExpression inv : assumptions) {
          BooleanFormula invFormula =
              converter.makePredicate(inv, pEdge, function, SSAMap.emptySSAMap().builder());
          assumption = bfmgr.and(assumption, formulaManager.uninstantiate(invFormula));
        }
      }

      if (element instanceof AvoidanceReportingState) {
        AvoidanceReportingState e = (AvoidanceReportingState) element;

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
      return pAsmptStorageElem; // nothing has changed
    }
    return new AssumptionStorageState(formulaManager, assumption, stopFormula);
  }
}
