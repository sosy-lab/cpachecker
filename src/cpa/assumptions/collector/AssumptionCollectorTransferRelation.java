/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.assumptions.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import assumptions.AbstractWrappedElementVisitor;
import assumptions.AssumptionReportingElement;
import assumptions.AssumptionWithLocation;
import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

/**
 * Transfer relation and strengthening for the DumpInvariant CPA
 * @author g.theoduloz
 */
public class AssumptionCollectorTransferRelation implements TransferRelation {

  private final ConfigurableProgramAnalysis wrappedCPA;
  private final TransferRelation wrappedTransfer;
  private final AssumptionReportingVisitor reportingVisitor;

  public AssumptionCollectorTransferRelation(AssumptionCollectorCPA cpa)
  {
    wrappedCPA = cpa.getWrappedCPAs().iterator().next();
    wrappedTransfer = wrappedCPA.getTransferRelation();
    reportingVisitor = new AssumptionReportingVisitor();
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge cfaEdge)
      throws CPATransferException {
    
    AssumptionCollectorElement element = (AssumptionCollectorElement)pElement;
    AbstractElement wrappedElement = element.getWrappedElements().iterator().next();
    
    // Compute the inner-successor
    Collection<? extends AbstractElement> wrappedSuccessors = wrappedTransfer.getAbstractSuccessors(wrappedElement, pPrecision, cfaEdge);
    
    if (wrappedSuccessors.isEmpty())
      return Collections.emptyList();
    
    ArrayList<AssumptionCollectorElement> successors = new ArrayList<AssumptionCollectorElement>(wrappedSuccessors.size());
    
    // Handle all inner-successor, one after the other, and produce
    // a corresponding wrapped successor
    for (AbstractElement wrappedSuccessor : wrappedSuccessors) {
      AssumptionWithLocation innerAssumption = reportingVisitor.collectAssumptions(wrappedSuccessor);
      successors.add(new AssumptionCollectorElement(wrappedSuccessor, innerAssumption)); 
    }
    return successors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement el, List<AbstractElement> others, CFAEdge edge, Precision p)
  throws CPATransferException
  {
    return null;
  }

  private static final class AssumptionReportingVisitor
    extends AbstractWrappedElementVisitor
  {
    private AssumptionWithLocation assumptionResult;

    @Override
    public void process(AbstractElement element) {
      if (element instanceof AssumptionReportingElement) {
        AssumptionWithLocation otherInv = ((AssumptionReportingElement)element).getAssumptionWithLocation();
        if (otherInv != null) {
          if (assumptionResult == null)
            assumptionResult = otherInv;
          else
            assumptionResult = assumptionResult.and(otherInv);
        }
      }
    }
    
    public synchronized AssumptionWithLocation collectAssumptions(AbstractElement element)
    {
      assumptionResult = null;
      
      visit(element);
      
      AssumptionWithLocation result = assumptionResult;
      assumptionResult = null;
      return result;
    }
  }
  
}
