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

package cpa.common.automaton;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author holzera
 */
public class Automaton2<E> {
  
  /*
   * Objects of type Transition represent a transition in an automaton. 
   */
  public class Transition {
    private Label<E> mLabel;
    private int mSuccessor;
    private int mPredecessor;
    
    private Transition(int pPredecessor, int pSuccessor, Label<E> pLabel) {
      assert(pLabel != null);
      
      mLabel = pLabel;
      
      mPredecessor = pPredecessor;
      mSuccessor = pSuccessor;
    }
    
    public int getSuccessor() {
      return mSuccessor;
    }
    
    public int getPredecessor() {
      return mPredecessor;
    }
    
    public boolean isFeasible(E pGuard) {
      return mLabel.matches(pGuard);
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (pOther == null) {
        return false;
      }
      
      try {
        Transition lTransition = (Transition)pOther;
        
        if (lTransition.mPredecessor != mPredecessor) {
          return false;
        }
        
        if (lTransition.mSuccessor != mSuccessor) {
          return false;
        }
        
        if (!lTransition.mLabel.equals(mLabel)) {
          return false;
        }
      }
      catch (ClassCastException lException) {
        return false;
      }
      
      return true;
    }

    @Override
    public int hashCode() {
      return (mLabel.hashCode() + mPredecessor + mSuccessor);
    }
    
    @Override
    public String toString() {
      return (mPredecessor + " -[" + mLabel + "]> " + mSuccessor);
    }
  }

  
  // members
  
  private HashMap<Integer, Set<Transition>> mTransitionRelation;
  private HashMap<Integer, Set<Transition>> mInverseTransitionRelation;
  private HashSet<Integer> mFinalStates;
  private static final int mInitialState = 0;
  private int mStateCounter;
  private final TrueLabel<E> mTrueLabel;
  
  
  // constructors
  
  public Automaton2() {
    mTransitionRelation = new HashMap<Integer, Set<Transition>>();
    mTransitionRelation.put(mInitialState, new HashSet<Transition>());
    
    mInverseTransitionRelation = new HashMap<Integer, Set<Transition>>();
    mInverseTransitionRelation.put(mInitialState, new HashSet<Transition>());
    
    mFinalStates = new HashSet<Integer>();
    
    mStateCounter = 1;
    
    mTrueLabel = new TrueLabel<E>();
  }
  
  public Automaton2(Automaton2<E> pOtherAutomaton) {
    assert(pOtherAutomaton != null);
    
    // copying pOtherAutomaton
    // TODO should we copy edges, too?
    mTransitionRelation = new HashMap<Integer, Set<Transition>>(pOtherAutomaton.mTransitionRelation);
    mInverseTransitionRelation = new HashMap<Integer, Set<Transition>>(pOtherAutomaton.mInverseTransitionRelation);
    mFinalStates = new HashSet<Integer>(pOtherAutomaton.mFinalStates);
    mStateCounter = pOtherAutomaton.mStateCounter;
    // TODO should we make a separate true label?
    mTrueLabel = pOtherAutomaton.mTrueLabel;
  }
  
  public Automaton2(Collection<Label<E>> pLabels) {
    this();
    
    assert(pLabels != null);
    
    for (Label<E> lLabel : pLabels) {
      int lSuccessorState = getNewState();
      
      setFinal(lSuccessorState);
      
      addTransition(getInitialState(), lSuccessorState, lLabel);
    }
  }
  
  
  // utility construction methods for automaton operations described in
  // VMCAI'09 paper
  
  public Automaton2<E> createUnion(Automaton2<E> pAutomaton) {
    assert(pAutomaton != null);
    
    
    // resulting automaton
    Automaton2<E> lAutomaton = new Automaton2<E>(this);
    
    
    // maps states in pAutomaton to states in lAutomaton
    HashMap<Integer, Integer> lStateMap = new HashMap<Integer, Integer>();
    
    for (Integer lState : pAutomaton.getStates()) {
      if (!pAutomaton.isInitialState(lState)) {
        // creates for every state (except the intial state) in pAutomaton a 
        // new state in lAutomaton
        int lNewState = lAutomaton.getNewState();
        
        lStateMap.put(lState, lNewState);
      }
      else {
        lStateMap.put(lState, lAutomaton.getInitialState());
      }
      
      assert(lStateMap.containsKey(lState));
      
      Integer lNewState = lStateMap.get(lState);
      
      if (pAutomaton.isFinalState(lState)) {
        lAutomaton.setFinal(lNewState);
      }
    }
    
    
    // add transitions of pAutomaton to lAutomaton
    for (Entry<Integer, Set<Transition>> lOutgoingTransitions : pAutomaton.mTransitionRelation.entrySet()) {
      Integer lPredecessorState = lStateMap.get(lOutgoingTransitions.getKey());
      
      for (Transition lTransition : lOutgoingTransitions.getValue()) {
        Integer lSuccessorState = lStateMap.get(lTransition.getSuccessor());
        
        lAutomaton.addTransition(lPredecessorState, lSuccessorState, lTransition.mLabel);
      }
    }

    
    return lAutomaton;
  }
  
  public Automaton2<E> createSequence(Automaton2<E> pAutomaton, Label<E> pLabel) {
    assert(pAutomaton != null);
    assert(pLabel != null);
    
    Automaton2<E> lAutomaton = new Automaton2<E>(this);
    
    assert(lAutomaton.getFinalStates().equals(getFinalStates()));
    
    for (Integer lFinalState : getFinalStates()) {
      assert(lAutomaton.contains(lFinalState));
      assert(lAutomaton.isFinalState(lFinalState));
      
      // this state is not final anymore
      // this has to be done since lFinalState can become final
      // again during add
      lAutomaton.unsetFinal(lFinalState);
      
      // append pAutomaton to every final state
      lAutomaton.appendAutomaton(lFinalState, pAutomaton);
      
      lAutomaton.addSelfLoop(lFinalState, pLabel);
    }
    
    return lAutomaton;
  }
  
  public Automaton2<E> createSequence(Automaton2<E> pAutomaton) {
    return createSequence(pAutomaton, mTrueLabel);
  }
  
  public Automaton2<E> createSequence(Automaton2<E> pAutomaton, Collection<Label<E>> pLabels) {
    assert(pLabels != null);
    assert(!pLabels.isEmpty());
    
    Label<E> lLabel = null;
    
    boolean first = true;
      
    for (Label<E> lGivenLabel : pLabels) {
      assert (lGivenLabel != null);

      if (first) {
        lLabel = lGivenLabel;

        first = false;
      } else {
        lLabel = new OrLabel<E>(lLabel, lGivenLabel);
      }
    }
    
    return createSequence(pAutomaton, lLabel);
  }
  
  public Automaton2<E> createExactIteration(int k) {
    assert(k >= 0);
    
    if (k == 0) {
      Automaton2<E> lAutomaton = new Automaton2<E>();
      
      lAutomaton.setFinal(lAutomaton.getInitialState());
      
      return lAutomaton;
    }
    else if (k == 1) {
      return new Automaton2<E>(this);
    }
    else {
      Automaton2<E> lAutomatonRec = createExactIteration(k - 1);
      
      Automaton2<E> lAutomaton = new Automaton2<E>(this);
      
      HashSet<Integer> lFinalStates = new HashSet<Integer>(lAutomaton.getFinalStates());
      
      for (Integer lFinalState : lFinalStates) {
        lAutomaton.unsetFinal(lFinalState);
        
        lAutomaton.appendAutomaton(lFinalState, lAutomatonRec);
      }
      
      return lAutomaton;
    }
  }
  
  public Automaton2<E> createMaximumIteration(int k) {
    assert(k >= 0);
    
    if (k == 0) {
      return createExactIteration(0);
    }
    else if (k == 1) {
      Automaton2<E> lAutomaton = createExactIteration(1);
      
      lAutomaton.setFinal(lAutomaton.getInitialState());
      
      return lAutomaton;
    }
    else {
      Automaton2<E> lAutomatonRec = createMaximumIteration(k - 1);
      
      Automaton2<E> lAutomaton = new Automaton2<E>(this);
      
      for (Integer lFinalState : lAutomaton.getFinalStates()) {
        lAutomaton.appendAutomaton(lFinalState, lAutomatonRec);
      }
      
      lAutomaton.setFinal(lAutomaton.getInitialState());
      
      return lAutomaton;
    }
  }
  
  public Automaton2<E> createMinimumIteration(int k) {
    assert(k >= 0);
    
    if (k == 0) {
      return createExactIteration(0);
    }
    else if (k == 1) {
      return createExactIteration(1);
    }
    else {
      Automaton2<E> lAutomatonRec = createExactIteration(k - 1);
      
      Automaton2<E> lAutomaton = new Automaton2<E>(this);
      
      HashSet<Integer> lFinalStates = new HashSet<Integer>(lAutomaton.getFinalStates());
      
      for (Integer lFinalState : lFinalStates) {
        lAutomaton.unsetFinal(lFinalState);
        
        lAutomaton.appendAutomaton(lFinalState, lAutomatonRec);
      }
      
      return lAutomaton;
    }
  }
  
  public void appendAutomaton(int pState, Automaton2<E> pAutomaton) {
    assert(contains(pState));
    assert(pAutomaton != null);
    
    // maps states in pAutomaton to states in lAutomaton
    HashMap<Integer, Integer> lStateMap = new HashMap<Integer, Integer>();
    
    // for all but the intial state of pAutomaton create a new state    
    for (Integer lState : pAutomaton.getStates()) {
      if (!pAutomaton.isInitialState(lState)) {
        // creates for every state (except the intial state) in pAutomaton a 
        // new state
        int lNewState = getNewState();
        
        lStateMap.put(lState, lNewState);
      }
      else {
        lStateMap.put(lState, pState);
      }
      
      assert(lStateMap.containsKey(lState));
      
      Integer lNewState = lStateMap.get(lState);
      
      if (pAutomaton.isFinalState(lState)) {
        setFinal(lNewState);
      }
    }
    
    
    // add transitions of pAutomaton
    for (Entry<Integer, Set<Transition>> lOutgoingTransitions : pAutomaton.mTransitionRelation.entrySet()) {
      Integer lPredecessorState = lStateMap.get(lOutgoingTransitions.getKey());
      
      for (Transition lTransition : lOutgoingTransitions.getValue()) {
        Integer lSuccessorState = lStateMap.get(lTransition.getSuccessor());
        
        addTransition(lPredecessorState, lSuccessorState, lTransition.mLabel);
      }
    }
  }
  
  
  // states
  
  public Set<Integer> getStates() {
    return Collections.unmodifiableSet(mTransitionRelation.keySet());
  }
  
  public int getNumberOfStates() {
    return mTransitionRelation.size();
  }
  
  public int getNewState() {
    assert(mStateCounter < Integer.MAX_VALUE);
    assert(!contains(mStateCounter));
    
    mTransitionRelation.put(mStateCounter, new HashSet<Transition>());
    mInverseTransitionRelation.put(mStateCounter, new HashSet<Transition>());
    
    return mStateCounter++;
  }
  
  public boolean contains(int pState) {
    return mTransitionRelation.containsKey(pState);
  }
  
  
  // intial states
  
  public int getInitialState() {
    return mInitialState;
  }
  
  public boolean isInitialState(int pState) {
    assert(contains(pState));
    
    return (pState == mInitialState);
  }
  
  
  // final states
  
  public boolean isFinalState(int pState) {
    assert(contains(pState));
    
    return mFinalStates.contains(pState);
  }
  
  public Set<Integer> getFinalStates() {
    return Collections.unmodifiableSet(mFinalStates);
  }

  public boolean hasFinalStates() {
    return !(mFinalStates.isEmpty());
  }
  
  public boolean setFinal(int pState) {
    assert(contains(pState));
    
    return mFinalStates.add(pState);
  }
  
  public boolean unsetFinal(int pState) {
    assert(contains(pState));
    
    return mFinalStates.remove(pState);
  }

  public boolean unsetFinal(Collection<Integer> pStates) {
    assert(pStates != null);

    boolean lReturnValue = false;

    for (Integer lState : pStates) {
      lReturnValue |= unsetFinal(lState);
    }

    return lReturnValue;
  }
  
  // transitions
  
  public boolean addTransition(int pPredecessor, int pSuccessor, Label<E> pLabel) {
    assert(contains(pPredecessor));
    assert(contains(pSuccessor));
    
    Transition lTransition = new Transition(pPredecessor, pSuccessor, pLabel);
    
    Set<Transition> lTransitions = mTransitionRelation.get(pPredecessor);
    
    Set<Transition> lInverseTransitions = mInverseTransitionRelation.get(pSuccessor);
    lInverseTransitions.add(lTransition);
    
    return lTransitions.add(lTransition);
  }
  
  public boolean removeTransition(Transition pTransition) {
    assert(pTransition != null);
    
    Set<Transition> lInverseTransitions = mTransitionRelation.get(pTransition.getSuccessor());
    lInverseTransitions.remove(pTransition);
    
    Set<Transition> lTransitions = mInverseTransitionRelation.get(pTransition.getPredecessor());
    
    return lTransitions.remove(pTransition);
  }
  
  public boolean addSelfLoop(int pState, Label<E> pLabel) {
    return addTransition(pState, pState, pLabel);
  }
  
  public boolean addUnconditionalSelfLoop(int pState) {
    return addSelfLoop(pState, mTrueLabel);
  }
  
  public Set<Transition> getOutgoingTransitions(int pState) {
    assert(contains(pState));
    
    return mTransitionRelation.get(pState);
  }
  
  public Set<Transition> getIncomingTransitions(int pState) {
    assert(contains(pState));
    
    return mInverseTransitionRelation.get(pState);
  }
  
  public Set<Integer> getFeasibleSuccessors(int pState, E pGuard) {
    Set<Transition> lTransitions = getOutgoingTransitions(pState);
    
    Set<Integer> lSuccessors = new HashSet<Integer>();
    
    for (Transition lTransition : lTransitions) {
      if (lTransition.isFeasible(pGuard)) {
        lSuccessors.add(lTransition.getSuccessor());
      }
    }
    
    return lSuccessors;
  }
  
  
  // output utilities
  
  public String getDotRepresentation() {
    String lDotRepresentation = "digraph automaton {\n";
    
    
    // dot representation of states
    
    for (Integer lState : getStates()) {
      String lStateDotRepresentation = "node [shape=";
      
      if (isInitialState(lState)) {
        if (isFinalState(lState)) {
          lStateDotRepresentation += "doubleoctagon";
        }
        else {
          lStateDotRepresentation += "octagon";
        }
      }
      else if (isFinalState(lState)) {
        lStateDotRepresentation += "doublecircle";
      }
      else {
        lStateDotRepresentation += "circle";
      }
      
      lStateDotRepresentation += ", label=\"q" + lState + "\"]; q" + lState + ";";
      
      lDotRepresentation += lStateDotRepresentation + "\n";
    }
    
    lDotRepresentation += "\n";
    
    
    // dot representation of transitions
    
    for (Set<Transition> lTransitions : mTransitionRelation.values()) {
      for (Transition lTransition : lTransitions) {
        lDotRepresentation += "q" + lTransition.getPredecessor() + " -> q" + lTransition.getSuccessor() + " [label=\"" + lTransition.mLabel + "\"];\n";
      }
    }
        
    lDotRepresentation += "}\n";

    
    return lDotRepresentation;
  }

  public Set<Integer> getReachableStates(int lStartState) {
    Set<Integer> lReachableStates = new HashSet<Integer>();

    lReachableStates.add(lStartState);

    LinkedList<Integer> lWorklist = new LinkedList<Integer>();

    lWorklist.add(lStartState);

    while (!lWorklist.isEmpty()) {
      Integer lCurrentState = lWorklist.removeFirst();

      for (Transition lTransition : this.getOutgoingTransitions(lCurrentState)) {
        int lSuccessor = lTransition.getSuccessor();

        if (lReachableStates.add(lSuccessor)) {
          lWorklist.add(lSuccessor);
        }
      }
    }

    return lReachableStates;
  }
  
  // simplification of automaton
  
  public Automaton2<E> getSimplifiedAutomaton() {
    HashSet<Integer> lVisitedStates = new HashSet<Integer>(getNumberOfStates());
    
    LinkedList<Integer> lWorklist = new LinkedList<Integer>();
    
    lWorklist.addAll(getFinalStates());
    
    while (!lWorklist.isEmpty()) {
      Integer lState = lWorklist.removeFirst();
      
      if (lVisitedStates.contains(lState)) {
        continue;
      }
      
      lVisitedStates.add(lState);
      
      for (Transition lTransition : getIncomingTransitions(lState)) {
        Integer lReachedState = lTransition.getPredecessor();
        
        lWorklist.add(lReachedState);
      }
    }
    
    Automaton2<E> lAutomaton = new Automaton2<E>();
    
    if (!lVisitedStates.contains(getInitialState())) {
      return lAutomaton;
    }
    
    for (Integer lState : getStates()) {
      if (!isInitialState(lState)) {
        lAutomaton.getNewState();
      }
    }
    
    HashSet<Integer> lVisitedStates2 = new HashSet<Integer>();
    
    assert(lWorklist.isEmpty());
    
    lWorklist.add(getInitialState());
    
    while (!lWorklist.isEmpty()) {
      Integer lState = lWorklist.removeFirst();
      
      if (lVisitedStates2.contains(lState)) {
        continue;
      }
      
      lVisitedStates2.add(lState);
      
      for (Transition lTransition : getOutgoingTransitions(lState)) {
        Integer lSuccessorState = lTransition.getSuccessor();
        
        if (lVisitedStates.contains(lSuccessorState)) {
          lAutomaton.addTransition(lState, lSuccessorState, lTransition.mLabel);
          
          lWorklist.add(lSuccessorState);
        }
      }
    }
    
    for (Integer lState : getFinalStates()) {
      lAutomaton.setFinal(lState);
    }
    
    return lAutomaton;
  }
  
  @Override
  public String toString() {
    String lStatesString = "States: " +getStates() + "\n";
    
    String lFinalStatesString = "Final States: " + getFinalStates() + "\n";
    
    String lTransitionsString = "Transitions:\n";
    
    for (Set<Transition> lTransitions : mTransitionRelation.values()) {
      for (Transition lTransition : lTransitions) {
        lTransitionsString += lTransition + "\n";
      }
    }
    
    return lStatesString + lFinalStatesString + lTransitionsString;
  }
}
