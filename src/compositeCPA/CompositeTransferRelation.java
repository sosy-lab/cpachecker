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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;
import cpa.common.CallElement;
import cpa.common.CallStack;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

public class CompositeTransferRelation implements TransferRelation{

  private final CompositeDomain compositeDomain;
  private final List<TransferRelation> transferRelations;

  // private LocationTransferRelation locationTransferRelation;

  public CompositeTransferRelation (CompositeDomain compositeDomain, List<TransferRelation> transferRelations)
  {
    this.compositeDomain = compositeDomain;
    this.transferRelations = transferRelations;
  }

  @Override
  public Collection<CompositeElement> getAbstractSuccessors(AbstractElement element, Precision precision, CFAEdge cfaEdge) throws CPATransferException {
    CompositeElement compositeElement = (CompositeElement) element;
    Collection<CompositeElement> results;
    
    if (cfaEdge == null) {
      CFANode node = compositeElement.getLocationNode();
      results = new ArrayList<CompositeElement>(node.getNumLeavingEdges());
      
      for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges(); edgeIdx++) {
        CFAEdge edge = node.getLeavingEdge(edgeIdx);
        getAbstractSuccessorForEdge(compositeElement, precision, edge, results);
      }
    
    } else {
      results = new ArrayList<CompositeElement>(1);
      getAbstractSuccessorForEdge(compositeElement, precision, cfaEdge, results);

    }

    return results;
  }
  
  private void getAbstractSuccessorForEdge(CompositeElement compositeElement, Precision precision, CFAEdge cfaEdge,
      Collection<CompositeElement> compositeSuccessors) throws CPATransferException {
    assert cfaEdge != null;
    
    CompositePrecision lCompositePrecision = null;
    if(precision != null){
      assert(precision instanceof CompositePrecision);
      lCompositePrecision = (CompositePrecision)precision;
    }

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
          ! returnElement.isConsistent(cfaEdge.getSuccessor().getFunctionName()) ) {
        compositeSuccessors.add(compositeDomain.getBottomElement());
        return;
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

    int resultCount = 1;
    List<AbstractElement> componentElements = compositeElement.getElements();
    List<Collection<? extends AbstractElement>> allComponentsSuccessors = new ArrayList<Collection<? extends AbstractElement>>(transferRelations.size());

    for (int idx = 0; idx < transferRelations.size (); idx++) {
      TransferRelation transfer = transferRelations.get(idx);
      AbstractElement componentElement = componentElements.get(idx);

      Precision lPrecision = null;
      if (lCompositePrecision != null) {
        lPrecision = lCompositePrecision.get(idx);
      }

      Collection<? extends AbstractElement> componentSuccessors = transfer.getAbstractSuccessors(componentElement, lPrecision, cfaEdge);
      resultCount *= componentSuccessors.size();
      allComponentsSuccessors.add(componentSuccessors);
    }
    assert resultCount != 0;
    
    Collection<List<AbstractElement>> allResultingElements;
    
    if (resultCount == 1) {
      List<AbstractElement> resultingElements = new ArrayList<AbstractElement>(allComponentsSuccessors.size());
      for (Collection<? extends AbstractElement> componentSuccessors : allComponentsSuccessors) {
        assert componentSuccessors.size() == 1;
        resultingElements.add(componentSuccessors.toArray(new AbstractElement[1])[0]);
      }
      allResultingElements = Collections.singleton(resultingElements);
      
    } else {
      // create cartesian product of all componentSuccessors and store the result in allResultingElements
      List<AbstractElement> initialPrefix = Collections.emptyList();
      allResultingElements = new ArrayList<List<AbstractElement>>(resultCount);
      createCartesianProduct(allComponentsSuccessors, initialPrefix, allResultingElements);
    }
    
    assert resultCount == allResultingElements.size();
    
    for (List<AbstractElement> resultingElements : allResultingElements) {
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
      
      CompositeElement compositeSuccessor = new CompositeElement(resultingElements, updatedCallStack);
      compositeSuccessors.add(compositeSuccessor);
    }

    return;
  }
  
  private static void createCartesianProduct(List<Collection<? extends AbstractElement>> allComponentsSuccessors,
      List<AbstractElement> prefix, Collection<List<AbstractElement>> allResultingElements) {
    
    if (prefix.size() == allComponentsSuccessors.size()) {
      allResultingElements.add(prefix);

    } else {
      int depth = prefix.size();
      Collection<? extends AbstractElement> myComponentsSuccessors = allComponentsSuccessors.get(depth);
      
      for (AbstractElement currentComponent : myComponentsSuccessors) {
        List<AbstractElement> newPrefix = new ArrayList<AbstractElement>(prefix);
        newPrefix.add(currentComponent);
        
        createCartesianProduct(allComponentsSuccessors, newPrefix, allResultingElements);
      }
    }
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    // strengthen is only called by the composite CPA on its component CPAs
    return null;
  }
}
