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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.util.AbstractWrappedElementVisitor;
import org.sosy_lab.cpachecker.util.assumptions.Assumption;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionReportingElement;
import org.sosy_lab.cpachecker.util.assumptions.AvoidanceReportingElement;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * Transfer relation and strengthening for the DumpInvariant CPA
 * @author g.theoduloz
 */
public class AssumptionCollectorTransferRelation implements TransferRelation {

  private static final Collection<AbstractElement> emptyElementSet
          = Collections.singleton(AssumptionCollectorElement.emptyElement);
  
  private final SymbolicFormulaManager symbolicFormulaManager;
  
  public AssumptionCollectorTransferRelation(SymbolicFormulaManager pManager) {
    symbolicFormulaManager = pManager;
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge) {
    AssumptionCollectorElement element = (AssumptionCollectorElement)pElement;

    // If we must stop, then let's stop by returning an empty set
    if (element.isStop()) {
      return Collections.emptySet();
    }
    
    return emptyElementSet;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement el, List<AbstractElement> others, CFAEdge edge, Precision p) {
    
    AssumptionAndForceStopReportingVisitor reportingVisitor = new AssumptionAndForceStopReportingVisitor();
    for (AbstractElement e : others) {
      reportingVisitor.visit(e);
    }
    
    boolean forceStop = reportingVisitor.forceStop;
    Assumption assumption = reportingVisitor.assumption;
    
    if (forceStop) {
//    TODO: write code that extracts the state formula from the predecessor element
//    for now, we just add the assumption "false" to the current location      
//      for (AbstractElement e : others) {
//        SymbolicFormula reportedFormula = ReportingUtils.extractReportedFormulas(manager, e);
//        Assumption dataAssumption = new Assumption(manager.makeNot(reportedFormula), false);
//        assumption.add(edge.getPredecessor(), dataAssumption);
//      }

      assumption = Assumption.FALSE;
    }
    
    return Collections.singleton(new AssumptionCollectorElement(assumption));
  }

  private final class AssumptionAndForceStopReportingVisitor extends AbstractWrappedElementVisitor {

    private Assumption assumption = Assumption.TRUE;
    private boolean forceStop = false;
    
    @Override
    public void process(AbstractElement element) {
      // process reported assumptions
      if (element instanceof AssumptionReportingElement) {
        Assumption inv = ((AssumptionReportingElement)element).getAssumption();
        assumption = Assumption.and(assumption, inv, symbolicFormulaManager);
      }

      // process stop flag
      if (element instanceof AvoidanceReportingElement) {
        boolean stop = ((AvoidanceReportingElement)element).mustDumpAssumptionForAvoidance();
        if (stop) {
          forceStop = true;
        }
      }
    }
  }

}
