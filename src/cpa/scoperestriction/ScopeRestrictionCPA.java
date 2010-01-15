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
/**
 * 
 */
package cpa.scoperestriction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

/**
 * @author holzera
 *
 */
public class ScopeRestrictionCPA implements ConfigurableProgramAnalysis {
  
  public class ScopeRestrictionTransferRelation implements TransferRelation {

    private AbstractElement getAbstractSuccessor(AbstractElement pElement,
                                                CFAEdge pCfaEdge, Precision prec)
                                                                 throws CPATransferException {
      AutomatonCPADomain<CFAEdge>.Element lSuccessor = mDomain.getSuccessor(pElement, pCfaEdge);
      
      // we want a deterministic behavior
      assert(lSuccessor.isSingleton() || lSuccessor.equals(mDomain.getBottomElement()) || lSuccessor.equals(mDomain.getTopElement()));
      
      return lSuccessor;
    }

    @Override
    public Collection<AbstractElement> getAbstractSuccessors(AbstractElement pElement, Precision prec, CFAEdge cfaEdge)
                                                                                   throws CPATransferException {
      return Collections.singleton(getAbstractSuccessor(pElement, cfaEdge, prec));
    }

    @Override
    public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                           List<AbstractElement> otherElements, CFAEdge cfaEdge,
                           Precision precision) {    
      return null;
    }
  }
  
  private AutomatonCPADomain<CFAEdge> mDomain;
  private ScopeRestrictionTransferRelation mTransferRelation;
  private MergeOperator mMergeOperator;
  private StopOperator mStopOperator;
  private PrecisionAdjustment mPrecisionAdjustment;
  
  public ScopeRestrictionCPA(Automaton<CFAEdge> pTestGoalAutomaton) {
    // Check for invariant: No final states
    assert(pTestGoalAutomaton.getFinalStates().isEmpty());
    
    mDomain = new AutomatonCPADomain<CFAEdge>(pTestGoalAutomaton);
    mTransferRelation = new ScopeRestrictionTransferRelation();
    mMergeOperator = MergeSepOperator.getInstance();
    mStopOperator = new StopSepOperator(mDomain.getPartialOrder());
    mPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getAbstractDomain()
   */
  @Override
  public AutomatonCPADomain<CFAEdge> getAbstractDomain() {
    return mDomain;
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public ScopeRestrictionTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getMergeOperator()
   */
  @Override
  public MergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getStopOperator()
   */
  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }
  
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  @Override
  public AutomatonCPADomain<CFAEdge>.Element getInitialElement(CFAFunctionDefinitionNode pNode) {
    return mDomain.getInitialElement();
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return null;
  }
}
