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
package org.sosy_lab.cpachecker.util.automaton;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.util.automaton.Automaton;
//import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * @author holzera
 *
 */
public class AutomatonCPADomain<E> implements AbstractDomain {

  public abstract class Element implements AbstractElement {
    private AutomatonCPADomain<E> mDomain;

    protected Element(AutomatonCPADomain<E> pDomain) {
      assert(pDomain != null);

      mDomain = pDomain;
    }

    public AutomatonCPADomain<E> getDomain() {
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
    private Set<Automaton<E>.State> mStates;

    private StateSetElement(AutomatonCPADomain<E> pDomain, Set<Automaton<E>.State> pStates) {
      super(pDomain);

      assert(pStates != null);

      // we do not want bottom here
      assert(pStates.size() > 0);

      // TODO: check whether pStates are states from our current automaton
      mStates = new HashSet<Automaton<E>.State>(pStates);
    }

    private StateSetElement(AutomatonCPADomain<E> pDomain, Automaton<E>.State pState) {
      super(pDomain);

      assert(pState != null);

      // ensure correctness of domain
      assert(pState.getAutomaton().equals(getDomain().mAutomaton));

      mStates = new HashSet<Automaton<E>.State>();
      mStates.add(pState);
    }

    public StateSetElement createUnionElement(Set<Automaton<E>.State> pStates) {
      StateSetElement lElement = new StateSetElement(this.getDomain(), this.getStates());

      lElement.mStates.addAll(pStates);

      return lElement;
    }

    public final Set<Automaton<E>.State> getStates() {
      return mStates;
    }

    @Override
    public boolean subsumes(Element pOtherElement) {
      assert(pOtherElement != null);
      assert(pOtherElement.getDomain().equals(getDomain()));

      // bot == {}
      if (pOtherElement instanceof AutomatonCPADomain<?>.BottomElement) {
        return false;
      }

      if (pOtherElement instanceof AutomatonCPADomain<?>.TopElement) {
        return true;
      }

      assert(pOtherElement instanceof AutomatonCPADomain<?>.StateSetElement);

      StateSetElement lOtherElement = (StateSetElement)pOtherElement;

      if (mStates.size() < lOtherElement.mStates.size()) {
        return false;
      }

      for (Automaton<E>.State lState : lOtherElement.mStates) {
        if (!mStates.contains(lState)) {
          return false;
        }
      }

      return true;
    }

    @Override
    public boolean isError() {
      return false;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (!(o instanceof AutomatonCPADomain<?>.StateSetElement)) {
        return false;
      }

      StateSetElement lOtherElement = (StateSetElement)o;

      if (mStates.size() != lOtherElement.mStates.size()) {
        return false;
      }

      // since both sets have the same number of elements in its set it suffices
      // to check whether every element of one of the sets is contained in
      // the other set.
      for (Automaton<E>.State lState : mStates) {
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

      Set<Automaton<E>.State> lSuccessorStates = new HashSet<Automaton<E>.State>();

      for (Automaton<E>.State lState : mStates) {
        lSuccessorStates.addAll(lState.getSuccessors(pEdge));
      }

      if (lSuccessorStates.size() == 0) {
        return new BottomElement(getDomain());
      }

      return new StateSetElement(getDomain(), lSuccessorStates);
    }

    @Override
    public boolean isSingleton() {
      return (mStates.size() == 1);
    }

    @Override
    public String toString() {
      String lString = "[";

      boolean lFirst = true;

      for (Automaton<E>.State lState : mStates) {
        if (!lFirst) {
          lString += ",";
        }
        else {
          lFirst = false;
        }

        lString += "q" + lState.getIndex();
      }

      lString += "]";

      return lString;
    }
  }

  public class BottomElement extends Element {
    private BottomElement(AutomatonCPADomain<E> pDomain) {
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

      if (!(o instanceof AutomatonCPADomain<?>.BottomElement)) {
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
    public boolean isError() {
      return false;
    }

    @Override
    public String toString() {
      return "Bottom";
    }
  }

  public class TopElement extends Element {
    private TopElement(AutomatonCPADomain<E> pDomain) {
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

      if (!(o instanceof AutomatonCPADomain<?>.TopElement)) {
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
    public boolean isError() {
      return false;
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

  public class JoinOperator implements org.sosy_lab.cpachecker.core.interfaces.JoinOperator {

    @Override
    public Element join(AbstractElement pElement1,
                                AbstractElement pElement2) throws CPAException {
      assert(pElement1 != null);
      assert(pElement2 != null);

      assert(pElement1 instanceof AutomatonCPADomain<?>.Element);
      assert(pElement2 instanceof AutomatonCPADomain<?>.Element);

      // ensure same domain
      assert(((Element)pElement1).getDomain().equals(((Element)pElement2).getDomain()));

      if (pElement1 instanceof AutomatonCPADomain<?>.BottomElement) {
        return (Element)pElement2;
      }

      if (pElement1 instanceof AutomatonCPADomain<?>.TopElement) {
        return (Element)pElement1;
      }

      if (pElement2 instanceof AutomatonCPADomain<?>.BottomElement) {
        return (Element)pElement1;
      }

      if (pElement2 instanceof AutomatonCPADomain<?>.TopElement) {
        return (Element)pElement2;
      }

      assert(pElement1 instanceof AutomatonCPADomain<?>.StateSetElement);
      assert(pElement2 instanceof AutomatonCPADomain<?>.StateSetElement);

      StateSetElement lElement1 = (StateSetElement)pElement1;
      StateSetElement lElement2 = (StateSetElement)pElement2;

      // join is union
      Set<Automaton<E>.State> lAllStates = new HashSet<Automaton<E>.State>();

      lAllStates.addAll(lElement1.mStates);
      lAllStates.addAll(lElement2.mStates);

      assert(lAllStates.size() > 0);

      return new StateSetElement(lElement1.getDomain(), lAllStates);
    }

  }

  public class PartialOrder implements org.sosy_lab.cpachecker.core.interfaces.PartialOrder {

    @Override
    public boolean satisfiesPartialOrder(AbstractElement pElement1,
                                         AbstractElement pElement2)
                                                                   throws CPAException {
      assert(pElement1 != null);
      assert(pElement2 != null);

      assert(pElement1 instanceof AutomatonCPADomain<?>.Element);
      assert(pElement2 instanceof AutomatonCPADomain<?>.Element);

      // ensure same domain
      assert(((Element)pElement1).getDomain().equals(((Element)pElement2).getDomain()));

      if (pElement1 instanceof AutomatonCPADomain<?>.BottomElement) {
        return true;
      }

      if (pElement2 instanceof AutomatonCPADomain<?>.TopElement) {
        return true;
      }

      assert(pElement1 instanceof AutomatonCPADomain<?>.StateSetElement);
      assert(pElement2 instanceof AutomatonCPADomain<?>.StateSetElement);

      StateSetElement lElement1 = (StateSetElement)pElement1;
      StateSetElement lElement2 = (StateSetElement)pElement2;

      return lElement1.isSubsumedBy(lElement2);
    }

  }

  private Automaton<E> mAutomaton;
  private BottomElement mBottomElement;
  private TopElement mTopElement;
  private Element mInitialElement;
  private final JoinOperator mJoinOperator = new JoinOperator();
  private final PartialOrder mPartialOrder = new PartialOrder();

  public AutomatonCPADomain(Automaton<E> pAutomaton) {
    assert(pAutomaton != null);

    mAutomaton = pAutomaton;
    mBottomElement = new BottomElement(this);
    mTopElement = new TopElement(this);

    mInitialElement = new StateSetElement(this, mAutomaton.getInitialState());
  }

  public StateSetElement castToStateSetElement(AbstractElement pElement) {
    assert(pElement != null);
    assert(pElement instanceof AutomatonCPADomain<?>.StateSetElement);

    return (StateSetElement)pElement;
  }

  public Element castToStateElement(AbstractElement pElement) {
    assert(pElement != null);
    assert(pElement instanceof AutomatonCPADomain<?>.Element);

    return (Element)pElement;
  }

  public BottomElement castToBottomElement(AbstractElement pElement) {
    assert(pElement != null);
    assert(pElement instanceof AutomatonCPADomain<?>.BottomElement);

    return (BottomElement)pElement;
  }

  public TopElement castToTopElement(AbstractElement pElement) {
    assert(pElement != null);
    assert(pElement instanceof AutomatonCPADomain<?>.TopElement);

    return (TopElement)pElement;
  }

  public final Automaton<E> getAutomaton() {
    return mAutomaton;
  }

  public Element getInitialElement() {
    return mInitialElement;
  }

  public boolean contains(AbstractElement pElement) {
    if (pElement == null) {
      return false;
    }

    if (!(pElement instanceof AutomatonCPADomain<?>.Element)) {
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
   * @see org.sosy_lab.cpachecker.core.interfaces.AbstractDomain#getBottomElement()
   */
  @Override
  public BottomElement getBottomElement() {
    return mBottomElement;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.AbstractDomain#getTopElement()
   */
  @Override
  public TopElement getTopElement() {
    return mTopElement;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.AbstractDomain#getJoinOperator()
   */
  @Override
  public JoinOperator getJoinOperator() {
    return mJoinOperator;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.AbstractDomain#getPartialOrder()
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

    if (!(o instanceof AutomatonCPADomain<?>)) {
      return false;
    }

    AutomatonCPADomain<?> lOtherDomain = (AutomatonCPADomain<?>)o;

    return lOtherDomain.mAutomaton.equals(mAutomaton);
  }

  @Override
  public int hashCode() {
    return mAutomaton.hashCode();
  }
}
