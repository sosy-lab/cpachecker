/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.assumptions.AbstractWrappedElementVisitor;
import org.sosy_lab.cpachecker.util.assumptions.Assumption;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionReportingElement;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;
import org.sosy_lab.cpachecker.util.assumptions.AvoidanceReportingElement;
import org.sosy_lab.cpachecker.util.assumptions.ReportingUtils;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Transfer relation and strengthening for the DumpInvariant CPA
 * @author g.theoduloz
 */
public class AssumptionCollectorTransferRelation implements TransferRelation {

  private final ConfigurableProgramAnalysis wrappedCPA;
  private final TransferRelation wrappedTransfer;
  private final AssumptionAndForceStopReportingVisitor reportingVisitor;
  private final SymbolicFormulaManager manager;

  public AssumptionCollectorTransferRelation(AssumptionCollectorCPA cpa)
  {
    wrappedCPA = cpa.getWrappedCpa();
    wrappedTransfer = wrappedCPA.getTransferRelation();
    reportingVisitor = new AssumptionAndForceStopReportingVisitor();
    manager = cpa.getSymbolicFormulaManager();
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    AssumptionCollectorElement element = (AssumptionCollectorElement)pElement;
    AbstractElement wrappedElement = element.getWrappedElement();

    // If we must stop, then let's stop by returning an empty set
    if (element.isStop())
      return Collections.emptySet();

    // Compute the inner-successor
    Collection<? extends AbstractElement> unwrappedSuccessors = wrappedTransfer.getAbstractSuccessors(wrappedElement, pPrecision, pCfaEdge);

    if (unwrappedSuccessors.isEmpty())
      return Collections.emptyList();

    ArrayList<AssumptionCollectorElement> successors = new ArrayList<AssumptionCollectorElement>(unwrappedSuccessors.size());

    // Handle all inner-successors, one after the other, and produce
    // a corresponding wrapped successor
    for (AbstractElement unwrappedSuccessor : unwrappedSuccessors) {
      Pair<AssumptionWithLocation,Boolean> pair = reportingVisitor.collect(unwrappedSuccessor);
      AssumptionWithLocation assumption = pair.getFirst();

      boolean forceStop = pair.getSecond();
      if (forceStop) {
        SymbolicFormula reportedFormula = ReportingUtils.extractReportedFormulas(manager, wrappedElement);
        AssumptionWithLocation dataAssumption = (new Assumption(manager.makeNot(reportedFormula), false)).atLocation(pCfaEdge.getPredecessor());
        assumption = assumption.and(dataAssumption);
      }

      boolean isBottom = forceStop;

      successors.add(new AssumptionCollectorElement(element.getCpa(), unwrappedSuccessor, assumption, isBottom));
    }
    return successors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement el, List<AbstractElement> others, CFAEdge edge, Precision p)
    throws CPATransferException
  {
    // TODO copied this from monitoringCPA to be tested -- we need the strengthening for
    // error location analysis
    AssumptionCollectorElement collectorElement = (AssumptionCollectorElement)el;
    AbstractElement wrappedElement = collectorElement.getWrappedElement();

    try {
       Collection<? extends AbstractElement> wrappedList = wrappedTransfer.strengthen(wrappedElement, others, edge, p);
       // if the returned list is null return null
       if(wrappedList == null)
         return null;

       // if bottom return empty list
       if(wrappedList.size() == 0){
         return Collections.emptyList();
       }

       ArrayList<AssumptionCollectorElement> retList = new ArrayList<AssumptionCollectorElement>(wrappedList.size());
       AssumptionWithLocation assumption = collectorElement.getCollectedAssumptions();
       boolean stop = collectorElement.isStop();
       for (AbstractElement wrappedReturnElement : wrappedList)
         retList.add(new AssumptionCollectorElement(collectorElement.getCpa(), wrappedReturnElement, assumption, stop));
       return retList;
    } catch (CPATransferException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static final class AssumptionAndForceStopReportingVisitor
    extends AbstractWrappedElementVisitor
  {
    private AssumptionWithLocation assumptionResult;
    private boolean forceStop;

    @Override
    public void process(AbstractElement element) {
      // process reported assumptions
      if (element instanceof AssumptionReportingElement) {
        AssumptionWithLocation otherInv = ((AssumptionReportingElement)element).getAssumptionWithLocation();
        if (otherInv != null) {
          if (assumptionResult == null)
            assumptionResult = otherInv;
          else
            assumptionResult = assumptionResult.and(otherInv);
        }
      }

      // process stop flag
      if (element instanceof AvoidanceReportingElement) {
        boolean otherStop = ((AvoidanceReportingElement)element).mustDumpAssumptionForAvoidance();
        if (otherStop)
          forceStop = true;
      }
    }

    public synchronized Pair<AssumptionWithLocation, Boolean> collect(AbstractElement element)
    {
      assumptionResult = null;
      forceStop = false;

      visit(element);

      Pair<AssumptionWithLocation, Boolean> result = new Pair<AssumptionWithLocation, Boolean>(assumptionResult, forceStop);
      assumptionResult = null;
      forceStop = false;
      return result;
    }
  }

}
