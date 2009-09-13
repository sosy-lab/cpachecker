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
package compositeCPA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;
import cpa.common.CallElement;
import cpa.common.CallStack;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;

public class CompositeTransferRelation implements TransferRelation{

  private final CompositeDomain compositeDomain;
  private final List<TransferRelation> transferRelations;

  // private LocationTransferRelation locationTransferRelation;

  public CompositeTransferRelation (CompositeDomain compositeDomain, List<TransferRelation> transferRelations)
  {
    this.compositeDomain = compositeDomain;
    this.transferRelations = transferRelations;

    //TransferRelation first = transferRelations.get (0);
    //if (first instanceof LocationTransferRelation)
    //{
    //	locationTransferRelation = (LocationTransferRelation) first;
    //}
  }

  public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge, Precision precision) throws CPATransferException
  {
    CompositePrecision lCompositePrecision = null;
    if(precision != null){
      assert(precision instanceof CompositePrecision);
      lCompositePrecision = (CompositePrecision)precision;
    }

    CompositeElement compositeElement = (CompositeElement) element;
    List<AbstractElement> inputElements = compositeElement.getElements ();
    List<AbstractElement> resultingElements = new ArrayList<AbstractElement> ();

    CallStack updatedCallStack = compositeElement.getCallStack();

    // TODO add some check here for unbounded recursive calls
    if(cfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge)
    {
      String functionName = cfaEdge.getSuccessor().getFunctionName();
      CFANode callNode = cfaEdge.getPredecessor();
      CallElement ce = new CallElement(functionName, callNode, compositeElement);
      CallStack cs = compositeElement.getCallStack();
      updatedCallStack = cs.clone();
      updatedCallStack.push(ce);
    }

    // handling the return from a function
    else if(cfaEdge.getEdgeType() == CFAEdgeType.ReturnEdge)
    {
      CallElement topCallElement = compositeElement.getCallStack().peek();
      assert(cfaEdge.getPredecessor().getFunctionName().
          equals(topCallElement.getFunctionName()));
      CallElement returnElement = compositeElement.getCallStack().getSecondTopElement();

      if(! topCallElement.isConsistent(cfaEdge.getSuccessor()) ||
          ! returnElement.isConsistent(cfaEdge.getSuccessor().getFunctionName()) ){
        return compositeDomain.getBottomElement();
      }

      // TODO we are saving the abstract state on summary edge, that works for
      // now but this is a bad design practice. Add another method
      // getAbstractSuccessorOnReturn(subElement, prevElement, cfaEdge)
      // and implement it for all CPAs later.
      else{
        CallStack cs = compositeElement.getCallStack();
        updatedCallStack = cs.clone();
        CallElement ce = updatedCallStack.pop();
        CompositeElement compElemBeforeCall = ce.getState();
        // TODO use summary edge as a cache later
        CallToReturnEdge summaryEdge = cfaEdge.getSuccessor().getEnteringSummaryEdge();
        summaryEdge.setAbstractElement(compElemBeforeCall);
      }
    }

    for (int idx = 0; idx < transferRelations.size (); idx++)
    {
      TransferRelation transfer = transferRelations.get (idx);
      AbstractElement subElement = null;
      AbstractElement successor = null;
      subElement = inputElements.get (idx);
      // handling a call edge

      Precision lPresicion = null;
      if(lCompositePrecision != null){
        lPresicion = lCompositePrecision.get(idx);
      }

      successor = transfer.getAbstractSuccessor (subElement, cfaEdge, lPresicion);
      resultingElements.add (successor);
    }

    List<AbstractElement> resultingElementsRO = Collections.unmodifiableList(resultingElements);
    for (int idx = 0; idx < transferRelations.size(); idx++) {
      AbstractElement result = transferRelations.get(idx).strengthen(
          resultingElements.get(idx),
          resultingElementsRO, cfaEdge,
          (lCompositePrecision == null) ? null : lCompositePrecision.get(idx));
      if (result != null) {
        resultingElements.set(idx, result);
      }
    }

    CompositeElement successorState = new CompositeElement (resultingElements, updatedCallStack);
    return successorState;
  }

  public List<AbstractElementWithLocation> getAllAbstractSuccessors (AbstractElementWithLocation element, Precision precision) throws CPAException, CPATransferException
  {

    CompositeElement compositeElement = (CompositeElement) element;
    CFANode node = compositeElement.getLocationNode();

    List<AbstractElementWithLocation> results = new ArrayList<AbstractElementWithLocation> ();

    for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges (); edgeIdx++)
    {
      CFAEdge edge = node.getLeavingEdge (edgeIdx);
      results.add ((CompositeElement) getAbstractSuccessor (element, edge, precision));
    }

    return results;
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    // strengthen is only called by the composite CPA on its component CPAs
    return null;
  }
}
