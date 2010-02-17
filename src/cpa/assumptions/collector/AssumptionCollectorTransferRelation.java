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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import symbpredabstraction.interfaces.SymbolicFormula;
import assumptions.AbstractWrappedElementVisitor;
import assumptions.Assumption;
import assumptions.AssumptionReportingElement;
import assumptions.AssumptionSymbolicFormulaManager;
import assumptions.AssumptionWithLocation;
import assumptions.AvoidanceReportingElement;
import assumptions.ReportingUtils;
import cfa.objectmodel.CFAEdge;

import common.Pair;

import cpa.common.CPAchecker;
import cpa.common.algorithm.CEGARAlgorithm;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.transferrelationmonitor.TransferRelationMonitorElement;
import exceptions.CPATransferException;

/**
 * Transfer relation and strengthening for the DumpInvariant CPA
 * @author g.theoduloz
 */
public class AssumptionCollectorTransferRelation implements TransferRelation {

  private final ConfigurableProgramAnalysis wrappedCPA;
  private final TransferRelation wrappedTransfer;
  private final AssumptionAndForceStopReportingVisitor reportingVisitor;
  private final AssumptionSymbolicFormulaManager manager;
  private final AbstractElement wrappedBottom;
  
  // variables for monitoring execution of a single transfer
  private TransferCallable tc = new TransferCallable();
  private long timeLimit = 0;
  private long timeLimitForPath = 0;

  public AssumptionCollectorTransferRelation(AssumptionCollectorCPA cpa)
  {
    wrappedCPA = cpa.getWrappedCPAs().iterator().next();
    wrappedTransfer = wrappedCPA.getTransferRelation();
    wrappedBottom = wrappedCPA.getAbstractDomain().getBottomElement();
    reportingVisitor = new AssumptionAndForceStopReportingVisitor();
    manager = cpa.getSymbolicFormulaManager();
    // time limit is given in milliseconds
    timeLimit = Integer.parseInt(CPAchecker.config.getPropertiesArray
        ("trackabstractioncomputation.limit")[0]);
    timeLimitForPath = Integer.parseInt(CPAchecker.config.getPropertiesArray
        ("trackabstractioncomputation.pathcomputationlimit")[0]);
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    
    AssumptionCollectorElement element = (AssumptionCollectorElement)pElement;
    
    // If we must stop, then let's stop by returning an empty set
    if (element.isStop())
      return Collections.emptySet();
    
    long timeOfExecution = 0;
    long start = 0;
    long end = 0;
    
    Collection<? extends AbstractElement> wrappedSuccessors = null;
    AbstractElement wrappedElement = element.getWrappedElements().iterator().next();
    // set the edge and element
    tc.setEdge(pCfaEdge);
    tc.setElement(wrappedElement);
    tc.setPrecision(pPrecision);
    
    // Compute the inner-successor
    Future<Collection<? extends AbstractElement>> future = CEGARAlgorithm.executor.submit(tc);
    try{
      start = System.currentTimeMillis();
      if(timeLimit == 0){
        wrappedSuccessors = future.get();
      }
      // here we get the result of the post computation but there is a time limit
      // given to complete the task specified by timeLimit
      else{
        assert(timeLimit > 0);
        wrappedSuccessors = future.get(timeLimit, TimeUnit.MILLISECONDS);
      }
      end = System.currentTimeMillis();
    } catch (TimeoutException exc){
      // we should return something here I think such as bottom
      return ??;
    } catch (InterruptedException exc) {
      exc.printStackTrace();
    } catch (ExecutionException exc) {
      exc.printStackTrace();
    }
    
    if (wrappedSuccessors.isEmpty())
      return Collections.emptyList();
    
    ArrayList<AssumptionCollectorElement> successors = new ArrayList<AssumptionCollectorElement>(wrappedSuccessors.size());
    
    // Handle all inner-successor, one after the other, and produce
    // a corresponding wrapped successor
    for (AbstractElement wrappedSuccessor : wrappedSuccessors) {
      Pair<AssumptionWithLocation,Boolean> pair = reportingVisitor.collect(wrappedSuccessor);
      AssumptionWithLocation assumption = pair.getFirst();
      
      boolean forceStop = pair.getSecond();
      if (forceStop) {
        SymbolicFormula reportedFormula = ReportingUtils.extractReportedFormulas(manager, wrappedElement);
        AssumptionWithLocation dataAssumption = (new Assumption(reportedFormula,false)).atLocation(pCfaEdge.getPredecessor());
        assumption = assumption.and(dataAssumption);
      }
      
      boolean isBottom = forceStop || wrappedBottom.equals(wrappedSuccessor); 

      successors.add(new AssumptionCollectorElement(wrappedSuccessor, assumption, isBottom));
    }
    return successors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement el, List<AbstractElement> others, CFAEdge edge, Precision p)
  throws CPATransferException
  {
    // TODO copied this from monitoringCPA to be tested -- we need the strengthening for 
    // error location analysis
    
    TransferRelationMonitorElement monitorElement = (TransferRelationMonitorElement)el;
    AbstractElement wrappedElement = monitorElement.getWrappedElements().iterator().next();
    List<AbstractElement> retList = new ArrayList<AbstractElement>();
    
    try {
       Collection<? extends AbstractElement> wrappedList = wrappedTransfer.strengthen(wrappedElement, others, edge, p);
       // if the returned list is null return null
       if(wrappedList == null)
         return null;
    // TODO we assume that only one element is returned or empty set to represent bottom
       assert(wrappedList.size() < 2);
       // if bottom return empty list
       if(wrappedList.size() == 0){
         return retList;
       }
       
       AbstractElement wrappedReturnElement = wrappedList.iterator().next();
       // fix this
       retList.add(new AssumptionCollectorElement(????));
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
      if (element instanceof AssumptionReportingElement) {
        AssumptionWithLocation otherInv = ((AssumptionReportingElement)element).getAssumptionWithLocation();
        if (otherInv != null) {
          if (assumptionResult == null)
            assumptionResult = otherInv;
          else
            assumptionResult = assumptionResult.and(otherInv);
        }
      }
      if (element instanceof AvoidanceReportingElement) {
        
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
  
  private class TransferCallable implements Callable<Collection<? extends AbstractElement>>{

    CFAEdge cfaEdge;
    AbstractElement abstractElement;
    Precision precision;

    public TransferCallable() {

    }

    @Override
    public Collection<? extends AbstractElement> call() throws Exception {
      return wrappedTransfer.getAbstractSuccessors(abstractElement, precision, cfaEdge);
    }

    public void setEdge(CFAEdge pCfaEdge){
      cfaEdge = pCfaEdge;
    }

    public void setElement(AbstractElement pAbstractElement){
      abstractElement = pAbstractElement;
    }

    public void setPrecision(Precision pPrecision){
      precision = pPrecision;
    }
  }
    
}
