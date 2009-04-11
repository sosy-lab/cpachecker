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
package cpa.common.automaton;

import java.util.HashSet;
import java.util.Set;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import exceptions.CPAException;
import java.util.Collection;

/**
 * @author holzera
 *
 */
public class AutomatonCPADomain2<E> implements AbstractDomain {

  public abstract class Element implements AbstractElement {
    private AutomatonCPADomain2<E> mDomain;
    
    protected Element(AutomatonCPADomain2<E> pDomain) {
      assert(pDomain != null);
      
      mDomain = pDomain;
    }
    
    public AutomatonCPADomain2<E> getDomain() {
      return mDomain;
    }
    
    public abstract boolean subsumes(Element pOtherElement);
    
    public boolean isSubsumedBy(Element pOtherElement) {
      assert(pOtherElement != null);
      
      return pOtherElement.subsumes(this);
    }
    
    public abstract Element getSuccessor(E pEdge);
    public abstract boolean isSingleton();
  }
  
  public class StateSetElement extends Element {
    private Set<Integer> mStates;
    
    private StateSetElement(AutomatonCPADomain2<E> pDomain, Set<Integer> pStates) {
      super(pDomain);
      
      assert(pStates != null);
      
      // we do not want bottom here
      assert(pStates.size() > 0);
      
      // TODO: check whether pStates are states from our current automaton
      mStates = new HashSet<Integer>(pStates);
    }
    
    private StateSetElement(AutomatonCPADomain2<E> pDomain, Integer pState) {
      super(pDomain);
      
      assert(pState != null);
      
      // ensure correctness of domain
      assert(getDomain().getAutomaton().contains(pState));
      
      mStates = new HashSet<Integer>();
      mStates.add(pState);
    }
    
    public StateSetElement createUnionElement(Set<Integer> pStates) {
      StateSetElement lElement = new StateSetElement(this.getDomain(), this.getStates());
      
      lElement.mStates.addAll(pStates);
      
      return lElement;
    }
    
    public final Set<Integer> getStates() {
      return mStates;
    }
    
    public Set<Integer> getNonacceptingStates() {
      Set<Integer> lNonacceptingStates = new HashSet<Integer>();
      
      AutomatonCPADomain2<E> lDomain = getDomain();
      Automaton2<E> lAutomaton = lDomain.getAutomaton();
      
      for (Integer lState : mStates) {
        if (!lAutomaton.isFinalState(lState)) {
          lNonacceptingStates.add(lState);
        }
      }
      
      return lNonacceptingStates;
    }
    
    public Set<Integer> getAcceptingStates() {
      Set<Integer> lAcceptingStates = new HashSet<Integer>();
      
      AutomatonCPADomain2<E> lDomain = getDomain();
      Automaton2<E> lAutomaton = lDomain.getAutomaton();
      
      for (Integer lState : mStates) {
        if (lAutomaton.isFinalState(lState)) {
          lAcceptingStates.add(lState);
        }
      }
      
      return lAcceptingStates;
    }

    public Element projectToVisibleStates(Collection<Integer> pVisibleStates) {
      Set<Integer> lVisibleStates = new HashSet<Integer>(mStates);

      lVisibleStates.retainAll(pVisibleStates);

      AutomatonCPADomain2<E> lDomain = getDomain();

      if (lVisibleStates.size() == 0) {
        return lDomain.getBottomElement();
      }

      if (lVisibleStates.size() == mStates.size()) {
        return this;
      }

      return new StateSetElement(lDomain, lVisibleStates);
    }

    public Element projectToNonacceptingStates() {
      Set<Integer> lNonacceptingStates = getNonacceptingStates();
      
      AutomatonCPADomain2<E> lDomain = getDomain();
      
      if (lNonacceptingStates.size() == 0) {
        return lDomain.getBottomElement();
      }
      
      if (lNonacceptingStates.size() == mStates.size()) {
        return this;
      }
      
      return new StateSetElement(lDomain, lNonacceptingStates);
    }
    
    @Override
    public boolean subsumes(Element pOtherElement) {
      assert(pOtherElement != null);
      assert(pOtherElement.getDomain().equals(getDomain()));
      
      // bot == {}
      if (pOtherElement instanceof AutomatonCPADomain2<?>.BottomElement) {
        return false;
      }
      
      if (pOtherElement instanceof AutomatonCPADomain2<?>.TopElement) {
        return true;
      }
            
      StateSetElement lOtherElement = (StateSetElement)pOtherElement;
      
      if (mStates.size() < lOtherElement.mStates.size()) {
        return false;
      }
      
      for (Integer lState : lOtherElement.mStates) {
        if (!mStates.contains(lState)) {
          return false;
        }
      }
      
      return true;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      
      if (!(o instanceof AutomatonCPADomain2<?>.StateSetElement)) {
        return false;
      }
      
      StateSetElement lOtherElement = (StateSetElement)o;
      
      if (mStates.size() != lOtherElement.mStates.size()) {
        return false;
      }
      
      // since both sets have the same number of elements in its set it suffices
      // to check whether every element of one of the sets is contained in 
      // the other set.
      for (Integer lState : mStates) {
        if (!lOtherElement.mStates.contains(lState)) {
          return false;
        }
      }
      
      return true;
    }
    
    @Override
    public int hashCode() {
      return mStates.hashCode();
    }

    @Override
    public Element getSuccessor(E pEdge) {
      assert(pEdge != null);
      
      Set<Integer> lSuccessorStates = new HashSet<Integer>();
      
      AutomatonCPADomain2<E> lDomain = getDomain();
      Automaton2<E> lAutomaton = lDomain.getAutomaton();
      
      for (Integer lState : mStates) {
        lSuccessorStates.addAll(lAutomaton.getFeasibleSuccessors(lState, pEdge));
      }
      
      if (lSuccessorStates.size() == 0) {
        return lDomain.getBottomElement();
      }
      
      if (lSuccessorStates.equals(this.mStates)) {
        return this;
      }
      
      return new StateSetElement(lDomain, lSuccessorStates);
    }

    @Override
    public boolean isSingleton() {
      return (mStates.size() == 1);
    }
    
    @Override
    public String toString() {
      String lString = "[";
      
      boolean lFirst = true;
      
      for (Integer lState : mStates) {
        if (!lFirst) {
          lString += ",";
        }
        else {
          lFirst = false;
        }
        
        lString += "q" + lState;
      }
      
      lString += "]";
      
      return lString;
    }
  }
  
  public class BottomElement extends Element {
    private BottomElement(AutomatonCPADomain2<E> pDomain) {
      super(pDomain);
    }
    
    @Override
    public boolean subsumes(Element pOtherElement) {
      // we assume bottom == {}, so we only subsume bottom.
      return this.equals(pOtherElement);
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      
      if (!(o instanceof AutomatonCPADomain2<?>.BottomElement)) {
        return false;
      }
      
      BottomElement lBottomElement = (BottomElement)o;
      
      return (lBottomElement.getDomain().equals(getDomain()));
    }
    
    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }

    @Override
    public Element getSuccessor(E pEdge) {
      return this;
    }

    @Override
    public boolean isSingleton() {
      return false;
    }
    
    @Override
    public String toString() {
      return "Bottom";
    }
  }
  
  public class TopElement extends Element {
    private TopElement(AutomatonCPADomain2<E> pDomain) {
      super(pDomain);
    }
    
    @Override
    public boolean subsumes(Element pOtherElement) {
      return true;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      
      if (!(o instanceof AutomatonCPADomain2<?>.TopElement)) {
        return false;
      }
      
      TopElement lTopElement = (TopElement)o;
      
      return (lTopElement.getDomain().equals(getDomain()));
    }
    
    @Override
    public int hashCode() {
      return Integer.MAX_VALUE;
    }

    @Override
    public Element getSuccessor(E pEdge) {
      return this;
    }

    @Override
    public boolean isSingleton() {
      // we define top as not being a singleton element
      return false;
    }
    
    @Override
    public String toString() {
      return "Top";
    }
  }
  
  public class JoinOperator implements cpa.common.interfaces.JoinOperator {

    @Override
    public Element join(AbstractElement pElement1,
                                AbstractElement pElement2) throws CPAException {
      assert(pElement1 != null);
      assert(pElement2 != null);      
      // ensure same domain
      assert(((Element)pElement1).getDomain().equals(((Element)pElement2).getDomain()));
      
      if (pElement1 instanceof AutomatonCPADomain2<?>.BottomElement) {
        return (Element)pElement2;
      }
      
      if (pElement1 instanceof AutomatonCPADomain2<?>.TopElement) {
        return (Element)pElement1;
      }
      
      if (pElement2 instanceof AutomatonCPADomain2<?>.BottomElement) {
        return (Element)pElement1;
      }
      
      if (pElement2 instanceof AutomatonCPADomain2<?>.TopElement) {
        return (Element)pElement2;
      }      
      StateSetElement lElement1 = (StateSetElement)pElement1;
      StateSetElement lElement2 = (StateSetElement)pElement2;

      // join is union
      Set<Integer> lAllStates = new HashSet<Integer>();
      
      lAllStates.addAll(lElement1.mStates);
      lAllStates.addAll(lElement2.mStates);
      
      assert(lAllStates.size() > 0);
      
      return new StateSetElement(lElement1.getDomain(), lAllStates);
    }
    
  }
  
  public class PartialOrder implements cpa.common.interfaces.PartialOrder {

    @Override
    public boolean satisfiesPartialOrder(AbstractElement pElement1,
                                         AbstractElement pElement2)
                                                                   throws CPAException {
      assert(pElement1 != null);
      assert(pElement2 != null);      
      // ensure same domain
      assert(((Element)pElement1).getDomain().equals(((Element)pElement2).getDomain()));

      if (pElement1 instanceof AutomatonCPADomain2<?>.BottomElement) {
        return true;
      }
      
      if (pElement2 instanceof AutomatonCPADomain2<?>.TopElement) {
        return true;
      }      
      StateSetElement lElement1 = (StateSetElement)pElement1;
      StateSetElement lElement2 = (StateSetElement)pElement2;

      return lElement1.isSubsumedBy(lElement2);
    }
    
  }
  
  private Automaton2<E> mAutomaton;
  private BottomElement mBottomElement;
  private TopElement mTopElement;
  private Element mInitialElement;
  private final JoinOperator mJoinOperator = new JoinOperator();
  private final PartialOrder mPartialOrder = new PartialOrder();
  
  public AutomatonCPADomain2(Automaton2<E> pAutomaton) {
    assert(pAutomaton != null);
    
    mAutomaton = pAutomaton;
    mBottomElement = new BottomElement(this);
    mTopElement = new TopElement(this);
    
    mInitialElement = new StateSetElement(this, mAutomaton.getInitialState());
  }
  
  public StateSetElement createStateSetElement(AutomatonCPADomain2<E> pDomain, Set<Integer> pStates) {
    return new StateSetElement(pDomain, pStates);
  }
  
  public StateSetElement castToStateSetElement(AbstractElement pElement) {
    assert(pElement != null);    
    return (StateSetElement)pElement;
  }
  
  public final Automaton2<E> getAutomaton() {
    return mAutomaton;
  }
  
  public Element getInitialElement() {
    return mInitialElement;
  }
  
  public boolean contains(AbstractElement pElement) {
    if (pElement == null) {
      return false;
    }
    
    if (!(pElement instanceof AutomatonCPADomain2<?>.Element)) {
      return false;
    }
    
    Element lElement = (Element)pElement;
    
    return equals(lElement.mDomain);
  }
  
  public Element getSuccessor(AbstractElement pElement, E pEdge) {
    assert(contains(pElement));
    
    Element lElement = (Element)pElement;
    
    return lElement.getSuccessor(pEdge);
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getBottomElement()
   */
  @Override
  public BottomElement getBottomElement() {
    return mBottomElement;
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getTopElement()
   */
  @Override
  public TopElement getTopElement() {
    return mTopElement;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getJoinOperator()
   */
  @Override
  public JoinOperator getJoinOperator() {
    return mJoinOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getPartialOrder()
   */
  @Override
  public PartialOrder getPartialOrder() {
    return mPartialOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    
    if (!(o instanceof AutomatonCPADomain2<?>)) {
      return false;
    }
    
    AutomatonCPADomain2<?> lOtherDomain = (AutomatonCPADomain2<?>)o;
    
    return lOtherDomain.mAutomaton.equals(mAutomaton);
  }
  
  @Override
  public int hashCode() {
    return mAutomaton.hashCode();
  }

  @Override
  public boolean isBottomElement(AbstractElement pElement) {
    return mBottomElement.equals(pElement);
  }
}
