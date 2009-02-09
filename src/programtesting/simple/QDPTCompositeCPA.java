/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2009  Dirk Beyer and Erkan Keremoglu.
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
package programtesting.simple;

import programtesting.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;

import common.Pair;
import compositeCPA.CompositeMergeOperator;
import compositeCPA.CompositePrecision;
import compositeCPA.CompositePrecisionAdjustment;
import compositeCPA.CompositeStopOperator;

import cpa.common.CallElement;
import cpa.common.CallStack;
import cpa.common.CompositeDomain;
import cpa.common.CompositeElement;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * @author Andreas Holzer <holzer@forsyte.de>
 *
 */
public class QDPTCompositeCPA implements ConfigurableProgramAnalysis {

  public interface HasSubpaths {
    public Iterable<List<Edge>> getSubpaths();
  }
  
  public abstract class Edge {
    private QDPTCompositeElement mParent;
    private QDPTCompositeElement mChild;
    private String mStringRepresentation;
    
    private int mHashCode;
    
    public Edge(QDPTCompositeElement pParent, QDPTCompositeElement pChild) {
      assert(pParent != null);
      assert(pChild != null);
      
      mParent = pParent;
      mChild = pChild;
      
      recalculateHashCode();
      recalculateStringRepresentation();
    }
    
    private void recalculateHashCode() {
      mHashCode = mParent.hashCode() + mChild.hashCode();
    }
    
    private void recalculateStringRepresentation() {
      mStringRepresentation = mParent + " -> " + mChild;
    }
    
    public QDPTCompositeElement getParent() {
      return mParent;
    }
    
    public QDPTCompositeElement getChild() {
      return mChild;
    }
    
    public void setParent(QDPTCompositeElement pParent) {
      assert(pParent != null);
      
      mParent.mChildren.remove(this);
      
      mChild.mParent = pParent;
      mParent = pParent;
      
      mParent.mChildren.add(this);
      
      recalculateHashCode();
      recalculateStringRepresentation();
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (pOther == null) {
        return false;
      }
      
      if (!(pOther instanceof Edge)) {
        return false;
      }
      
      Edge lOther = (Edge)pOther;
      
      if (mParent.equals(lOther.mParent) && mChild.equals(lOther.mChild)) {
        return true;
      }
      
      return false;
    }
    
    @Override
    public int hashCode() {
      return mHashCode;
    }
    
    @Override
    public String toString() {
      return mStringRepresentation;
    }
  }
  
  public class CFAEdgeEdge extends Edge {
    private CFAEdge mEdge;
    private int mHashCode;
    private String mStringRepresentation;
    
    public CFAEdgeEdge(QDPTCompositeElement pParent, QDPTCompositeElement pChild, CFAEdge pEdge) {
      super(pParent, pChild);
      
      assert(pEdge != null);
      
      assert(pEdge.getPredecessor().equals(pParent.getElementWithLocation().getLocationNode()));
      assert(pEdge.getSuccessor().equals(pChild.getElementWithLocation().getLocationNode()));
      
      mEdge = pEdge;

      recalculateHashCode();
      recalculateStringRepresentation();
    }
    
    private void recalculateHashCode() {
      mHashCode = super.hashCode() + mEdge.hashCode();
    }
    
    private void recalculateStringRepresentation() {
      mStringRepresentation = getParent() + " -[" + mEdge.toString() + "]> " + getChild();
    }
    
    public CFAEdge getCFAEdge() {
      return mEdge;
    }
    
    @Override
    public void setParent(QDPTCompositeElement pParent) {
      assert(pParent != null);

      assert(mEdge.getPredecessor().equals(pParent.getElementWithLocation().getLocationNode()));
      
      super.setParent(pParent);
      
      recalculateHashCode();
      recalculateStringRepresentation();
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (!super.equals(pOther)) {
        return false;
      }
      
      if (!(pOther instanceof CFAEdgeEdge)) {
        return false;
      }
      
      CFAEdgeEdge lOther = (CFAEdgeEdge)pOther;
      
      return lOther.mEdge.equals(mEdge);
    }

    @Override
    public int hashCode() {
      return mHashCode;
    }
    
    @Override
    public String toString() {
      return mStringRepresentation;
    }
  }
  
  public class CFAEdgeAndSubpathsEdge extends CFAEdgeEdge implements HasSubpaths {
    private HashSet<List<Edge>> mSubpaths;
    private int mHashCode;
    private String mStringRepresentation;
    
    public CFAEdgeAndSubpathsEdge(QDPTCompositeElement pParent, QDPTCompositeElement pChild, CFAEdge pEdge, Collection<List<Edge>> pSubpaths) {
      super(pParent, pChild, pEdge);
      
      for (List<Edge> lSubpath : pSubpaths) {
        assert(lSubpath != null);
        assert(lSubpath.size() == 1);
        
        Edge lEdge = lSubpath.get(0);
        
        assert(lEdge != null);
        assert(lEdge instanceof CFAEdgeEdge);
        
        CFAEdgeEdge lCFAEdgeEdge = (CFAEdgeEdge)lEdge;
        
        assert(getCFAEdge().equals(lCFAEdgeEdge.getCFAEdge()));
      }
      
      mSubpaths = new HashSet<List<Edge>>(pSubpaths);
      
      recalculateHashCode();
      recalculateStringRepresentation();
    }
    
    private void recalculateHashCode() {
      mHashCode = super.hashCode() + mSubpaths.hashCode();
    }
    
    private void recalculateStringRepresentation() {
      mStringRepresentation = super.toString();
    }
    
    public Iterable<List<QDPTCompositeCPA.Edge>> getSubpaths() {
      return mSubpaths;
    }
    
    @Override
    public void setParent(QDPTCompositeElement pParent) {
      super.setParent(pParent);
      
      recalculateHashCode();
      recalculateStringRepresentation();
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (!super.equals(pOther)) {
        return false;
      }
      
      if (!(pOther instanceof CFAEdgeAndSubpathsEdge)) {
        return false;
      }
      
      CFAEdgeAndSubpathsEdge lOther = (CFAEdgeAndSubpathsEdge)pOther;
      
      // TODO is this correct?
      return lOther.getSubpaths().equals(getSubpaths());
    }
    
    @Override
    public int hashCode() {
      return mHashCode;
    }
    
    @Override
    public String toString() {
      return mStringRepresentation;
    }
  }
  
  public class SubpathsEdge extends Edge implements HasSubpaths {
    private HashSet<List<Edge>> mSubpaths;
    private int mHashCode;
    private String mStringRepresentation;
    
    public SubpathsEdge(QDPTCompositeElement pParent, QDPTCompositeElement pChild, Collection<List<Edge>> pSubpaths) {
      super(pParent, pChild);
      
      assert(pSubpaths != null);
      
      for (List<Edge> lSubpath : pSubpaths) {
        assert(lSubpath != null);
        assert(lSubpath.size() > 0);
      }
      
      // TODO more consistency checks
      
      mSubpaths = new HashSet<List<Edge>>(pSubpaths);
      
      recalculateHashCode();
      recalculateStringRepresentation();
    }
    
    private void recalculateHashCode() {
      mHashCode = super.hashCode() + mSubpaths.hashCode();
    }
    
    private void recalculateStringRepresentation() {
      mStringRepresentation = super.toString();
    }
    
    public Iterable<List<Edge>> getSubpaths() {
      return mSubpaths;
    }
    
    @Override
    public void setParent(QDPTCompositeElement pParent) {
      assert(pParent != null);

      // TODO add consistency checks
      
      super.setParent(pParent);
      
      recalculateHashCode();
      recalculateStringRepresentation();
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (!super.equals(pOther)) {
        return false;
      }
      
      if (!(pOther instanceof SubpathsEdge)) {
        return false;
      }
      
      SubpathsEdge lOther = (SubpathsEdge)pOther;
      
      // TODO is this correct?
      return lOther.getSubpaths().equals(getSubpaths());
    }
    
    @Override
    public int hashCode() {
      return mHashCode;
    }
    
    @Override
    public String toString() {
      return mStringRepresentation;
    }
  }
  
  public class QDPTCompositeElement extends CompositeElement {
    private QDPTCompositeElement mParent;
    private HashSet<Edge> mChildren;
    private Edge mEdge;
    
    public QDPTCompositeElement(List<AbstractElement> pElements, CallStack pStack, QDPTCompositeElement pParent, CFAEdge pCFAEdge) {
      this(pElements, pStack, pParent);
      
      assert(pCFAEdge != null);
      
      mChildren = new HashSet<Edge>();
      
      mEdge = new CFAEdgeEdge(mParent, this, pCFAEdge);
      mParent.mChildren.add(mEdge);
    }
    
    public QDPTCompositeElement(List<AbstractElement> pElements, CallStack pStack, QDPTCompositeElement pParent, Collection<List<Edge>> pEdgeSet) {
      this(pElements, pStack, pParent);
      
      assert(pEdgeSet != null);
      assert(pEdgeSet.size() > 1);
      
      // do all paths have length one?
      
      boolean lIsCFAEdgeEdge = true;
      
      CFAEdge lCFAEdge = null;
      
      for (List<Edge> lSubpath : pEdgeSet) {
        assert(lSubpath != null);
        assert(lSubpath.size() > 0);
        
        if (lSubpath.size() > 1) {
          lIsCFAEdgeEdge = false;
          
          break;
        }
        else {
          Edge lEdge = lSubpath.get(0);
          
          if (lEdge instanceof CFAEdgeEdge) {
            CFAEdgeEdge lCFAEdgeEdge = (CFAEdgeEdge)lEdge;
            
            CFAEdge lCurrentCFAEdge = lCFAEdgeEdge.getCFAEdge();
            
            if (lCFAEdge == null) {
              lCFAEdge = lCurrentCFAEdge;
            }
            else {
              if (!lCFAEdge.equals(lCurrentCFAEdge)) {
                lIsCFAEdgeEdge = false;
                
                break;
              }
            }
          }
          else {
            lIsCFAEdgeEdge = false;
            
            break;
          }
        }
      }
      
      if (lIsCFAEdgeEdge) {
        // do all paths have the same cfa edge?
        assert(lCFAEdge != null);
        
        mEdge = new CFAEdgeAndSubpathsEdge(pParent, this, lCFAEdge, pEdgeSet);
      }
      else {
        mEdge = new SubpathsEdge(pParent, this, pEdgeSet);        
      }
      
      mParent.mChildren.add(mEdge);
    }
    
    private QDPTCompositeElement(List<AbstractElement> pElements, CallStack pStack, QDPTCompositeElement pParent) {
      this(pElements, pStack);
      
      assert(pParent != null);
      
      mParent = pParent;
    }
    
    public QDPTCompositeElement(List<AbstractElement> pElements, CallStack pStack) {
      super(pElements, pStack);
      
      mParent = null;
      mChildren = new HashSet<Edge>();
      mEdge = null;
    }
    
    @Override
    public boolean equals(Object o) {
      return (this == o);
    }
    
    @Override
    public int hashCode() {
      return super.hashCode();
    }
    
    @Override
    public String toString() {
      return super.toString();
    }
    
    public boolean isRoot() {
      return !hasParent();
    }
    
    public boolean hasParent() {
      return (mParent != null);
    }
    
    public QDPTCompositeElement getParent() {
      assert(mParent != null);
      
      return mParent;
    }
    
    public Edge getEdgeToParent() {
      return mEdge;
    }
    
    public boolean hasChildren() {
      return (mChildren.size() > 0);
    }
    
    public Iterable<Edge> getChildren() {
      return mChildren;
    }
    
    public int getNumberOfChildren() {
      return mChildren.size();
    }
    
    // removes this node from its parent, i.e., cutting itself away from the tree
    public void remove() {
      if (hasParent()) {
        QDPTCompositeElement lParent = getParent();
        
        lParent.mChildren.remove(mEdge);
        
        mEdge = null;
        mParent = null;
      }
    }
    
    public void hideChildren() {
      mChildren.clear();
    }
    
    public int getDepth() {
      if (isRoot()) {
        return 0;
      }
      else {
        return getParent().getDepth() + 1;
      }
    }
    
    public List<Edge> getPathToRoot() {
      LinkedList<Edge> lPath = new LinkedList<Edge>();
      
      QDPTCompositeElement lElement = this;
      
      while (lElement.hasParent()) {
        lPath.addFirst(lElement.mEdge);
        
        lElement = lElement.getParent();
      }
      
      return lPath;
    }
    
    public boolean isSuccessor(QDPTCompositeElement lOtherElement) {
      assert(lOtherElement != null);
      
      List<Edge> lPath = getPathToRoot();
      
      for (Edge lEdge : lPath) {
        if (lEdge.getParent().equals(lOtherElement)) {
          return true;
        }
      }
      
      return false;
    }
  }
  
  public class QDPTCompositeTransferRelation implements TransferRelation{

    private final CompositeDomain compositeDomain;
    private final List<TransferRelation> transferRelations;

    private QDPTCompositeElement mRoot;

    public QDPTCompositeTransferRelation (CompositeDomain compositeDomain, List<TransferRelation> transferRelations)
    {
      this.compositeDomain = compositeDomain;
      this.transferRelations = transferRelations;

      mRoot = null;
    }
    
    public AbstractDomain getAbstractDomain ()
    {
      return compositeDomain;
    }
    
    public boolean hasRoot() {
      return (mRoot != null);
    }
    
    public QDPTCompositeElement getRoot() {
      return mRoot;
    }

    public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge, Precision precision) throws CPATransferException
    {
      assert(precision instanceof CompositePrecision);
      CompositePrecision lCompositePrecision = (CompositePrecision)precision;

      QDPTCompositeElement compositeElement = (QDPTCompositeElement) element;

      // initialize root of ART
      if (!hasRoot()) {
        mRoot = compositeElement;
      }

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
        // now but this is a terrible design practice. Add another method
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

        Precision lPresicion = lCompositePrecision.get(idx);

        successor = transfer.getAbstractSuccessor (subElement, cfaEdge, lPresicion);

        // as soon as a component returns bottom we return composite bottom
        if (compositeDomain.getDomains().get(idx).getBottomElement().equals(successor)) {
          return compositeDomain.getBottomElement();
        }

        resultingElements.add (successor);
      }

      QDPTCompositeElement successorState = new QDPTCompositeElement (resultingElements, updatedCallStack, compositeElement, cfaEdge);

      return successorState;
    }

    public List<AbstractElementWithLocation> getAllAbstractSuccessors(AbstractElementWithLocation element, Precision precision) throws CPAException, CPATransferException
    {

      //TODO CPACheckerStatistics.noOfTransferRelations++;

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
  }

  private final CompositeDomain mDomain;
  private final QDPTCompositeTransferRelation mTransferRelation;
  private final CompositeMergeOperator mMergeOperator;
  private final CompositeStopOperator mStopOperator;
  private final CompositePrecisionAdjustment mPrecisionAdjustment;
  private final QDPTCompositeElement mInitialElement;
  private final CompositePrecision mInitialPrecision;
  
  private final AutomatonCPADomain<CFAEdge> mAutomatonDomain;
  private final int mTestGoalCPAIndex;

  public QDPTCompositeCPA(List<ConfigurableProgramAnalysis> cpas, CFAFunctionDefinitionNode node,
                          AutomatonCPADomain<CFAEdge> pAutomatonDomain, int pTestGoalCPAIndex) {
    List<AbstractDomain> domains = new ArrayList<AbstractDomain> ();
    List<TransferRelation> transferRelations = new ArrayList<TransferRelation> ();
    List<MergeOperator> mergeOperators = new ArrayList<MergeOperator> ();
    List<StopOperator> stopOperators = new ArrayList<StopOperator> ();
    List<PrecisionAdjustment> precisionAdjustments = new ArrayList<PrecisionAdjustment> ();
    List<AbstractElement> initialElements = new ArrayList<AbstractElement> ();
    List<Precision> initialPrecisions = new ArrayList<Precision> ();

    for(ConfigurableProgramAnalysis sp : cpas) {
      domains.add(sp.getAbstractDomain());
      transferRelations.add(sp.getTransferRelation());
      mergeOperators.add(sp.getMergeOperator());
      stopOperators.add(sp.getStopOperator());
      precisionAdjustments.add(sp.getPrecisionAdjustment());
      initialElements.add(sp.getInitialElement(node));
      initialPrecisions.add(sp.getInitialPrecision(node));
    }

    mDomain = new CompositeDomain(domains);
    mTransferRelation = new QDPTCompositeTransferRelation(mDomain, transferRelations);
    mMergeOperator = new CompositeMergeOperator(mDomain, mergeOperators);
    mStopOperator = new CompositeStopOperator(mDomain, stopOperators);
    mPrecisionAdjustment = new CompositePrecisionAdjustment(precisionAdjustments);
    mInitialElement = new QDPTCompositeElement(initialElements, null);
    mInitialPrecision = new CompositePrecision(initialPrecisions);
    
    assert (pAutomatonDomain != null);
    mAutomatonDomain = pAutomatonDomain;
    assert (0 <= pTestGoalCPAIndex && pTestGoalCPAIndex < cpas.size());
    mTestGoalCPAIndex = pTestGoalCPAIndex;

    // set call stack
    CallStack initialCallStack = new CallStack();
    CallElement initialCallElement = new CallElement(node.getFunctionName(), node, mInitialElement);
    initialCallStack.push(initialCallElement);
    mInitialElement.setCallStack(initialCallStack);
  }

  // TODO: Move newReachedSet into interface of ConfigurableProgramAnalysis and
  // provide an abstract ConfigurableProgramAnalysisImpl-Class that implements
  // it by default by creating a hash set?
  // TODO: During ART creation establish an order
  // that allows efficient querying for test goals
  public Collection<Pair<AbstractElementWithLocation,Precision>> newReachedSet() {
    return new QDPTReachedSet(mAutomatonDomain,mTestGoalCPAIndex);
  }
  
  @Override
  public CompositeDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public QDPTCompositeElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    // TODO create an initial element
    return mInitialElement;
  }

  @Override
  public CompositePrecision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return mInitialPrecision;
  }

  @Override
  public CompositeMergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  @Override
  public CompositePrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  @Override
  public CompositeStopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public QDPTCompositeTransferRelation getTransferRelation() {
    return mTransferRelation;
  }
  
  public QDPTCompositeElement createElement(List<AbstractElement> pElements, CallStack pStack, QDPTCompositeElement pParent, Collection<List<Edge>> pPathSet) {
    return new QDPTCompositeElement(pElements, pStack, pParent, pPathSet);
  }

}
