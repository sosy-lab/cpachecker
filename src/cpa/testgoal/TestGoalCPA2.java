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
package cpa.testgoal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import common.Pair;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import exceptions.CPATransferException;
import cpa.common.automaton.Automaton2;
import cpa.common.automaton.AutomatonCPADomain2;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import java.util.Collections;
import java.util.Set;

/**
 * @author holzera
 *
 */
public class TestGoalCPA2 implements ConfigurableProgramAnalysis {

  public class TestGoalPrecision implements Precision {
    private Collection<Integer> mVisibleStates;
    
    public TestGoalPrecision(Collection<Integer> pVisibleStates) {
      assert(pVisibleStates != null);
      
      mVisibleStates = new HashSet<Integer>();
      mVisibleStates.addAll(pVisibleStates);
    }
    
    public TestGoalPrecision() {
      mVisibleStates = Collections.EMPTY_SET;
    }
    
    public boolean isEmpty() {
      return mVisibleStates.isEmpty();
    }

    public TestGoalPrecision getDifferencePrecision(AutomatonCPADomain2<CFAEdge>.BottomElement pElement) {
      assert(pElement != null);
      
      return this;
    }

    public TestGoalPrecision getDifferencePrecision(AutomatonCPADomain2<CFAEdge>.TopElement pElement) {
      assert(pElement != null);
      
      if (mVisibleStates.size() != 0) {
        return new TestGoalPrecision();
      }
      
      return this;
    }

    public TestGoalPrecision getDifferencePrecision(AutomatonCPADomain2<CFAEdge>.StateSetElement pElement) {
      assert(pElement != null);

      return getDifferencePrecision(pElement.getAcceptingStates());
    }

    public TestGoalPrecision getDifferencePrecision(Collection<Integer> pInvisibleStates) {
      assert(pInvisibleStates != null);

      Set<Integer> lNewRemainingTestGoals = new HashSet<Integer>(mVisibleStates);

      lNewRemainingTestGoals.removeAll(pInvisibleStates);

      return new TestGoalPrecision(lNewRemainingTestGoals);
    }

    public TestGoalPrecision getIntersection(Collection<Integer> pVisibleStates) {
      assert(pVisibleStates != null);

      Set<Integer> lNewVisibleStates = new HashSet<Integer>(mVisibleStates);

      lNewVisibleStates.retainAll(pVisibleStates);

      return new TestGoalPrecision(lNewVisibleStates);
    }

    public final Collection<Integer> getVisibleStates() {
      return mVisibleStates;
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
      
      return mVisibleStates.equals(lPrecision.mVisibleStates);
    }
    
    @Override
    public int hashCode() {
      // TODO Problem: when mVisibleStates gets changed the hashcode changes
      return mVisibleStates.hashCode();
    }
    
    @Override
    public String toString() {
      String lDescription = "Test goal precision: " + mVisibleStates.toString();
      
      return lDescription;
    }
    
  }

  public class TestGoalMergeOperator implements MergeOperator {

    @Override
    public AbstractElement merge(AbstractElement pElement1,
                                 AbstractElement pElement2,
                                 Precision prec) throws CPAException {
      assert(pElement1 != null);
      assert(pElement2 != null);
      
      assert(pElement1 instanceof AutomatonCPADomain2<?>.Element);
      assert(pElement2 instanceof AutomatonCPADomain2<?>.Element);
      
      // no join if top or bottom element
      if (!(pElement1 instanceof AutomatonCPADomain2<?>.StateSetElement) 
          || !(pElement2 instanceof AutomatonCPADomain2<?>.StateSetElement)) {
        return pElement2;
      }
      
      AutomatonCPADomain2<CFAEdge>.StateSetElement lElement1 = mDomain.castToStateSetElement(pElement1);
      AutomatonCPADomain2<CFAEdge>.StateSetElement lElement2 = mDomain.castToStateSetElement(pElement2);
      
      if (lElement1.equals(lElement2)) {
        return lElement2;
      }
      
      Set<Integer> lNonacceptingStates1 = lElement1.getNonacceptingStates();
      Set<Integer> lNonacceptingStates2 = lElement2.getNonacceptingStates();
      
      if (lNonacceptingStates1.equals(lNonacceptingStates2)) {
        return lElement1.projectToNonacceptingStates();
      }
      
      return lElement2;
    }

    @Override
    public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                             AbstractElementWithLocation pElement2,
                                             Precision prec) throws CPAException {
      throw new CPAException ("Cannot return element with location information");
    }
  }

  public class TestGoalStopOperator implements StopOperator {

    @Override
    public <AE extends AbstractElement> boolean stop(AE pElement,
                                                     Collection<AE> pReached, Precision prec)
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
      assert (pElement != null);
      assert (pReachedElement != null);

      if (mDomain.isBottomElement(pElement)) {
        return true;
      }

      if (mDomain.getTopElement().equals(pReachedElement)) {
        return true;
      }

      if (mDomain.getTopElement().equals(pElement)) {
        return false;
      }

      if (mDomain.isBottomElement(pReachedElement)) {
        return false;
      }

      assert (pElement instanceof AutomatonCPADomain2<?>.Element);
      assert (pReachedElement instanceof AutomatonCPADomain2<?>.Element);

      AutomatonCPADomain2<CFAEdge>.StateSetElement lElement = mDomain.castToStateSetElement(pElement);
      AutomatonCPADomain2<CFAEdge>.StateSetElement lReachedElement = mDomain.castToStateSetElement(pReachedElement);

      Automaton2<CFAEdge> lAutomaton = mDomain.getAutomaton();

      for (Integer lState : lElement.getStates()) {
        if (!lAutomaton.isFinalState(lState)) {
          if (!lReachedElement.getStates().contains(lState)) {
            return false;
          }
        }
      }

      return true;
    }

  }

  public class TestGoalPrecisionAdjustment implements PrecisionAdjustment {

    @Override
    public <AE extends AbstractElement> Pair<AE, Precision> prec(
        AE pElement,
        Precision pPrecision,
        Collection<Pair<AE, Precision>> pElements) {
      // TODO remove all covered test goals from pPrecision
      // TODO This is a hack for performance reasons
      return new Pair<AE,Precision> (pElement, pPrecision);
    }

  }

  public class TestGoalTransferRelation implements TransferRelation {
    
    @Override
    public AbstractElement getAbstractSuccessor(AbstractElement pElement,
                                                CFAEdge pCfaEdge, Precision prec)
    throws CPATransferException {
      assert(prec != null);
      assert(prec instanceof TestGoalPrecision);
      
      // TODO use lPrecision for something
      TestGoalPrecision lPrecision = (TestGoalPrecision)prec;

      //AutomatonCPADomain2<CFAEdge>.Element lElement = mDomain.getSuccessor(pElement, pCfaEdge);

      // TODO implement other cases than StateSetElement
      AutomatonCPADomain2<CFAEdge>.StateSetElement lStateSetElement = mDomain.castToStateSetElement(pElement);

      AutomatonCPADomain2<CFAEdge>.Element lElement = mDomain.getSuccessor(lStateSetElement.projectToVisibleStates(lPrecision.getVisibleStates()), pCfaEdge);
      
      // TODO implement other cases
      AutomatonCPADomain2<CFAEdge>.StateSetElement lResultElement = mDomain.castToStateSetElement(lElement);

      return lResultElement.projectToVisibleStates(lPrecision.getVisibleStates());

      //return lElement;
    }

    @Override
    public List<AbstractElementWithLocation> getAllAbstractSuccessors(AbstractElementWithLocation pElement, Precision prec)
    throws CPAException,
    CPATransferException {
      // this method may not be called!
      assert(false);

      return null;
    }

  }
  
  private AutomatonCPADomain2<CFAEdge> mDomain;
  private TestGoalTransferRelation mTransferRelation;
  private TestGoalMergeOperator mMergeOperator;
  private TestGoalStopOperator mStopOperator;
  private PrecisionAdjustment mPrecisionAdjustment;

  public TestGoalCPA2(Automaton2<CFAEdge> pTestGoalAutomaton) {
    mDomain = new AutomatonCPADomain2<CFAEdge>(pTestGoalAutomaton);
    mTransferRelation = new TestGoalTransferRelation();
    mMergeOperator = new TestGoalMergeOperator();
    mStopOperator = new TestGoalStopOperator();
    mPrecisionAdjustment = new TestGoalPrecisionAdjustment();
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getAbstractDomain()
   */
  @Override
  public AutomatonCPADomain2<CFAEdge> getAbstractDomain() {
    return mDomain;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public TestGoalTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getMergeOperator()
   */
  @Override
  public TestGoalMergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getStopOperator()
   */
  @Override
  public TestGoalStopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  @Override
  public AutomatonCPADomain2<CFAEdge>.Element getInitialElement(CFAFunctionDefinitionNode pNode) {
    return mDomain.getInitialElement();
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    //return new TestGoalPrecision(this.mDomain.getAutomaton().getFinalStates());
    return new TestGoalPrecision(this.mDomain.getAutomaton().getStates());
  }
}
