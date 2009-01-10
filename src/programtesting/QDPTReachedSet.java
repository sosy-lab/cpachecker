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
package programtesting;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import cfa.objectmodel.CFAEdge;

import common.Pair;

import cpa.common.CompositeElement;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;

/**
 * @author Andreas Holzer <holzer@forsyte.de>
 *
 */
public class QDPTReachedSet implements Collection<Pair<AbstractElementWithLocation,Precision>> {

  class QDPTReachedSetIterator implements Iterator<Pair<AbstractElementWithLocation,Precision>> {
    private Iterator<Pair<AbstractElementWithLocation,Precision>> lInnerIterator;

    private int lMapIndex;

    public QDPTReachedSetIterator() {
      lMapIndex = TOP_INDEX;
      lInnerIterator = null;
    }

    private void init() {
      if (lInnerIterator == null) {
        lInnerIterator = mMap.get(lMapIndex).iterator();
      }
    }

    @Override
    public boolean hasNext() {
      init();

      if (lInnerIterator.hasNext()) {
        return true;
      }

      if (lMapIndex > BOTTOM_INDEX) {
        lMapIndex--;
        lInnerIterator = mMap.get(lMapIndex).iterator();

        return hasNext();
      }

      return false;
    }

    @Override
    public Pair<AbstractElementWithLocation,Precision> next() {
      init();

      if (hasNext()) {
        return lInnerIterator.next();
      }

      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      init();

      lInnerIterator.remove();
    }

  }

  private Map<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> mMap;
  private AutomatonCPADomain<CFAEdge> mAutomatonDomain;
  private final int mTestGoalCPAIndex;
  
  private final int TOP_INDEX;
  private final int BOTTOM_INDEX;

  public QDPTReachedSet(AutomatonCPADomain<CFAEdge> pAutomatonDomain, int pTestGoalCPAIndex) {

    mMap = new HashMap<Integer, Set<Pair<AbstractElementWithLocation,Precision>>>();
    
    assert(pAutomatonDomain != null);
    mAutomatonDomain = pAutomatonDomain;

    mTestGoalCPAIndex = pTestGoalCPAIndex;
    
    // top
    assert(pAutomatonDomain.getAutomaton().getFinalStates().size() < Integer.MAX_VALUE);
    TOP_INDEX = pAutomatonDomain.getAutomaton().getFinalStates().size() + 1;
    mMap.put(TOP_INDEX, new HashSet<Pair<AbstractElementWithLocation,Precision>>());

    for (int i = 0; i <= pAutomatonDomain.getAutomaton().getFinalStates().size(); i++) {
      mMap.put(i, new HashSet<Pair<AbstractElementWithLocation,Precision>>());
    }

    // bottom
    BOTTOM_INDEX = -1;
    mMap.put(BOTTOM_INDEX, new HashSet<Pair<AbstractElementWithLocation,Precision>>());
  }

  @Override
  public boolean add(Pair<AbstractElementWithLocation,Precision> pE) {
    assert(pE != null);
    assert(pE.getFirst() != null);
    assert(pE.getFirst() instanceof CompositeElement);

    CompositeElement lCompositeElement = (CompositeElement)pE.getFirst();

    AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);

    if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(TOP_INDEX);

      assert(lSet != null);

      return lSet.add(pE);
    }

    if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(BOTTOM_INDEX);

      assert(lSet != null);

      return lSet.add(pE);
    }

    AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = mAutomatonDomain.castToStateSetElement(lTmpElement);

    final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();

    int lNumberOfFinalStates = 0;

    for (Automaton<CFAEdge>.State lState : lStates) {
      if (lState.isFinal()) {
        lNumberOfFinalStates++;
      }
    }

    Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(lNumberOfFinalStates);

    assert(lSet != null);

    return lSet.add(pE);
  }

  @Override
  public boolean addAll(Collection<? extends Pair<AbstractElementWithLocation,Precision>> pC) {
    assert(pC != null);

    boolean lWasChanged = false;

    for (Pair<AbstractElementWithLocation,Precision> lElement : pC) {
      if (add(lElement)) {
        lWasChanged = true;
      }
    }

    return lWasChanged;
  }

  @Override
  public void clear() {
    for (Map.Entry<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> lEntry : mMap.entrySet()) {
      lEntry.getValue().clear();
    }
  }

  @Override
  public boolean contains(Object pO) {
    assert(pO != null);
    assert(pO instanceof Pair<?,?>);

    Pair<AbstractElementWithLocation,Precision> lPair = (Pair<AbstractElementWithLocation,Precision>)pO;

    assert(lPair.getFirst() instanceof CompositeElement);

    CompositeElement lCompositeElement = (CompositeElement)lPair.getFirst();

    AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);

    if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(TOP_INDEX);

      assert(lSet != null);

      return lSet.contains(pO);
    }

    if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(BOTTOM_INDEX);

      assert(lSet != null);

      return lSet.contains(pO);
    }

    AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = mAutomatonDomain.castToStateSetElement(lTmpElement);

    final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();

    int lNumberOfFinalStates = 0;

    for (Automaton<CFAEdge>.State lState : lStates) {
      if (lState.isFinal()) {
        lNumberOfFinalStates++;
      }
    }

    Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(lNumberOfFinalStates);

    assert(lSet != null);

    return lSet.contains(pO);
  }

  @Override
  public boolean containsAll(Collection<?> pC) {
    assert(pC != null);

    for (Object lObject : pC) {
      if (!contains(lObject)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean isEmpty() {
    for (Map.Entry<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> lEntry : mMap.entrySet()) {
      if (!lEntry.getValue().isEmpty()) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Iterator<Pair<AbstractElementWithLocation,Precision>> iterator() {
    return new QDPTReachedSetIterator();
  }

  @Override
  public boolean remove(Object pO) {
    assert(pO != null);

    assert(pO instanceof CompositeElement);

    CompositeElement lCompositeElement = (CompositeElement)pO;

    AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);

    if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(TOP_INDEX);

      assert(lSet != null);

      return lSet.remove(pO);
    }

    if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(BOTTOM_INDEX);

      assert(lSet != null);

      return lSet.remove(pO);
    }

    AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = mAutomatonDomain.castToStateSetElement(lTmpElement);

    final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();

    int lNumberOfFinalStates = 0;

    for (Automaton<CFAEdge>.State lState : lStates) {
      if (lState.isFinal()) {
        lNumberOfFinalStates++;
      }
    }

    Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(lNumberOfFinalStates);

    assert(lSet != null);

    return lSet.remove(pO);
  }

  @Override
  public boolean removeAll(Collection<?> pC) {
    assert(pC != null);

    boolean lWasChanged = false;

    for (Object lObject : pC) {
      if (remove(lObject)) {
        lWasChanged = true;
      }
    }

    return lWasChanged;
  }

  @Override
  public boolean retainAll(Collection<?> pC) {
    assert(pC != null);

    boolean lWasChanged = false;

    for (Pair<AbstractElementWithLocation,Precision> lElement : this) {
      if (!pC.contains(lElement)) {
        if (remove(lElement)) {
          lWasChanged = true;
        }
      }
    }

    return lWasChanged;
  }

  @Override
  public int size() {
    int lSize = 0;

    for (Map.Entry<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> lEntry : mMap.entrySet()) {
      lSize += lEntry.getValue().size();
    }

    return lSize;
  }

  @Override
  public Object[] toArray() {
    int lSize = size();

    if (lSize == 0) {
      return new Object[0];
    }

    Object[] lObjects = new Object[lSize];

    int lIndex = 0;

    for (Object lObject : this) {
      lObjects[lIndex] = lObject;
      lIndex++;
    }

    return lObjects;
  }

  @Override
  public <T> T[] toArray(T[] pA) {
    assert(pA != null);

    int lSize = size();

    if (lSize <= pA.length) {
      int lIndex = 0;

      for (Object lObject : this) {
        pA[lIndex] = (T)lObject;
      }

      for (int i = lSize; i < pA.length; i++) {
        pA[i] = null;
      }

      return pA;
    }
    else {
      return (T[])toArray();
    }
  }

  @Override
  public String toString() {
    return mMap.toString();
  }
}

