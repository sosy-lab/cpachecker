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
/**
 * 
 */
package org.sosy_lab.cpachecker.cpa.testgoal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.util.automaton.Automaton;
import org.sosy_lab.cpachecker.util.automaton.AutomatonCPADomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * @author holzera
 *
 */
public class TestGoalCPA implements ConfigurableProgramAnalysis {

  public class TestGoalPrecision implements Precision {
    Collection<Automaton<CFAEdge>.State> mRemainingFinalStates;
    
    public TestGoalPrecision(Collection<Automaton<CFAEdge>.State> pFinalStates) {
      assert(pFinalStates != null);
      
      mRemainingFinalStates = new HashSet<Automaton<CFAEdge>.State>();
      mRemainingFinalStates.addAll(pFinalStates);
    }
    
    // TODO Hack!!!
    public void setTestGoals(Collection<Automaton<CFAEdge>.State> pFinalStates) {
      assert(pFinalStates != null);
      
      mRemainingFinalStates.clear();
      mRemainingFinalStates.addAll(pFinalStates);
    }
    
    public boolean allTestGoalsCovered() {
      return mRemainingFinalStates.isEmpty();
    }
    
    public void removeAllFinalStates(AutomatonCPADomain<CFAEdge>.Element pElement) {
      assert(pElement != null);
      
      if (mDomain.getTopElement().equals(pElement)) {
        mRemainingFinalStates.clear();
        
        return;
      }
      
      if (mDomain.getBottomElement().equals(pElement)) {
        return;
      }
      
      AutomatonCPADomain<CFAEdge>.StateSetElement lStateSetElement = mDomain.castToStateSetElement(pElement);
      
      mRemainingFinalStates.removeAll(lStateSetElement.getStates());
    }
    
    public final Collection<Automaton<CFAEdge>.State> getRemainingFinalStates() {
      return mRemainingFinalStates;
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (pOther == null) {
        return false;
      }
      
      if (!(pOther instanceof TestGoalPrecision)) {
        return false;
      }
      
      TestGoalPrecision lPrecision = (TestGoalPrecision)pOther;
      
      return mRemainingFinalStates.equals(lPrecision.mRemainingFinalStates);
    }
    
    @Override
    public int hashCode() {
      return mRemainingFinalStates.hashCode();
    }
    
    @Override
    public String toString() {
      String lDescription = "Test goal precision: " + mRemainingFinalStates.toString();
      
      return lDescription;
    }
    
  }

  /*public class TestGoalMergeOperator implements MergeOperator {

    @Override
    public AbstractElement merge(AbstractElement pElement1,
                                 AbstractElement pElement2,
                                 Precision prec) throws CPAException {
      assert(pElement1 != null);
      assert(pElement2 != null);
      
      assert(pElement1 instanceof AutomatonCPADomain<?>.Element);
      assert(pElement2 instanceof AutomatonCPADomain<?>.Element);
      
      // no join if top or bottom element
      if (!(pElement1 instanceof AutomatonCPADomain<?>.StateSetElement) 
          || !(pElement2 instanceof AutomatonCPADomain<?>.StateSetElement)) {
        return pElement2;
      }
      
      AutomatonCPADomain<CFAEdge>.StateSetElement lElement1 = mDomain.castToStateSetElement(pElement1);
      AutomatonCPADomain<CFAEdge>.StateSetElement lElement2 = mDomain.castToStateSetElement(pElement2);
      
      for (Automaton<CFAEdge>.State lState : lElement1.getStates()) {
        if (!lState.isFinal()) {
          if (!lElement2.getStates().contains(lState)) {
            // no join
            return pElement2;
          }
        }
      }
      
      for (Automaton<CFAEdge>.State lState : lElement2.getStates()) {
        if (!lState.isFinal()) {
          if (!lElement1.getStates().contains(lState)) {
            // no join
            return pElement2;
          }
        }
      }
      
      return lElement1.createUnionElement(lElement2.getStates());
      
      // no join
      return pElement2;
    }

    public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                             AbstractElementWithLocation pElement2,
                                             Precision prec) throws CPAException {
      throw new CPAException ("Cannot return element with location information");
    }
  }*/

  public class TestGoalStopOperator implements StopOperator {

    @Override
    public boolean stop(AbstractElement pElement, Collection<AbstractElement> pReached, Precision prec)
    throws CPAException {
      assert(pElement != null);
      assert(pReached != null);

      // exists lElement in pReached with stop(pElement, lElement)?
      for (AbstractElement lElement : pReached) {
        if (stop(pElement, lElement)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public boolean stop(AbstractElement pElement,
                        AbstractElement pReachedElement) throws CPAException {
        assert(pElement != null);
        assert(pReachedElement != null);
        
        if (mDomain.getBottomElement().equals(pElement)) {
            return true;
        }
        
        if (mDomain.getTopElement().equals(pReachedElement)) {
            return true;
        }
        
        if (mDomain.getTopElement().equals(pElement)) {
            return false;
        }
        
        if (mDomain.getBottomElement().equals(pReachedElement)) {
            return false;
        }
        
        assert(pElement instanceof AutomatonCPADomain<?>.Element);
        assert(pReachedElement instanceof AutomatonCPADomain<?>.Element);
        
        AutomatonCPADomain<CFAEdge>.StateSetElement lElement = mDomain.castToStateSetElement(pElement);
        AutomatonCPADomain<CFAEdge>.StateSetElement lReachedElement = mDomain.castToStateSetElement(pReachedElement);

        for (Automaton<CFAEdge>.State lState : lElement.getStates()) {
            if (!lState.isFinal()) {
                if (!lReachedElement.getStates().contains(lState)) {
                    return false;
                }
            }
        }
        
        return true;
      //return mDomain.getPartialOrder().satisfiesPartialOrder(pElement, pReachedElement);
    }

  }

  public class TestGoalTransferRelation implements TransferRelation {

    private AbstractElement getAbstractSuccessor(AbstractElement pElement,
                                                CFAEdge pCfaEdge, Precision prec)
    throws CPATransferException {
      assert(prec != null);
      assert(prec instanceof TestGoalPrecision);
      
      TestGoalPrecision lPrecision = (TestGoalPrecision)prec;
      
      if (lPrecision.allTestGoalsCovered()) {
        return mDomain.getBottomElement();
      }
      
      // TODO This is a hack for performance reasons
      AutomatonCPADomain<CFAEdge>.Element lElement = mDomain.getSuccessor(pElement, pCfaEdge);
      
      int lOldSize = lPrecision.getRemainingFinalStates().size();
      
      lPrecision.removeAllFinalStates(lElement);
      
      int lNewSize = lPrecision.getRemainingFinalStates().size();
      
      if (lOldSize != lNewSize) {
        CPAchecker.logger.log(Level.INFO, "Remaining #states = " + lNewSize);
      }
      
      return lElement;
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
  private TestGoalTransferRelation mTransferRelation;
  private MergeOperator mMergeOperator;
  private TestGoalStopOperator mStopOperator;
  private PrecisionAdjustment mPrecisionAdjustment;

  public TestGoalCPA(Automaton<CFAEdge> pTestGoalAutomaton) {
    // Check for invariant: Final states will not be left once they are reached.
    for (Automaton<CFAEdge>.State lState : pTestGoalAutomaton.getFinalStates()) {
      // we ensure the property by a very strict assumption:
      assert(lState.hasUnconditionalSelfLoop());
    }

    mDomain = new AutomatonCPADomain<CFAEdge>(pTestGoalAutomaton);
    mTransferRelation = new TestGoalTransferRelation();
    mMergeOperator = MergeSepOperator.getInstance();
    mStopOperator = new TestGoalStopOperator();
    
    // TODO remove all covered test goals from pPrecision
    // TODO This is a hack for performance reasons
    mPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getAbstractDomain()
   */
  @Override
  public AutomatonCPADomain<CFAEdge> getAbstractDomain() {
    return mDomain;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public TestGoalTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getMergeOperator()
   */
  @Override
  public MergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getStopOperator()
   */
  @Override
  public TestGoalStopOperator getStopOperator() {
    return mStopOperator;
  }

  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getInitialElement(org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  @Override
  public AutomatonCPADomain<CFAEdge>.Element getInitialElement(CFAFunctionDefinitionNode pNode) {
    return mDomain.getInitialElement();
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return new TestGoalPrecision(this.mDomain.getAutomaton().getFinalStates());
  }
}
