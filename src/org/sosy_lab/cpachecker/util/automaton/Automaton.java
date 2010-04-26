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
package org.sosy_lab.cpachecker.util.automaton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.sosy_lab.common.Pair;

/**
 * @author holzera
 *
 */
public class Automaton<E> {
  public class State {
    private Automaton<E> mAutomaton;
    private int mIndex;
    private Set<Pair<Label<E>, State>> mOutgoingTransitions;
    private Set<Pair<Label<E>, State>> mIncomingTransitions;
    
    public State(Automaton<E> pAutomaton, int pIndex) {
      mAutomaton = pAutomaton;
      mIndex = pIndex;
      
      mOutgoingTransitions = new HashSet<Pair<Label<E>, State>>();
      mIncomingTransitions = new HashSet<Pair<Label<E>, State>>();
    }
    
    public Automaton<E> getAutomaton() {
      return mAutomaton;
    }
    
    public boolean isInitial() {
      return mAutomaton.mInitialState.equals(this);
    }
    
    public void setFinal() {
      mAutomaton.mFinalStates.add(this);
    }
    
    public void unsetFinal() {
      mAutomaton.mFinalStates.remove(this);
    }
    
    public boolean isFinal() {
      return mAutomaton.mFinalStates.contains(this);
    }
    
    public int getIndex() {
      return mIndex;
    }
    
    public void addTransition(Label<E> pLabel, State pState) {
      assert(pState != null);
      assert(pLabel != null);
      
      mOutgoingTransitions.add(new Pair<Label<E>, State>(pLabel, pState));
      pState.mIncomingTransitions.add(new Pair<Label<E>, State>(pLabel, this));
    }
    
    public void addTransition(Label<E> pLabel, int pIndex) {
      assert(pIndex >= 0 && pIndex < mAutomaton.mStates.size());      
      
      addTransition(pLabel, mAutomaton.mStates.get(pIndex));
    }
    
    public void addSelfLoop(Label<E> pLabel) {
      //assert(pLabel != null);
      
      addTransition(pLabel, this);
      //mOutgoingTransitions.add(new Pair<Label<E>, State>(pLabel, this));
    }
    
    public void addUnconditionalSelfLoop() {
      addSelfLoop(mAutomaton.mTrueLabel);
    }
    
    public void add(Automaton<E> pAutomaton) {
      assert(pAutomaton != null);
      
      int lOffset = mAutomaton.mStates.size() - 1;
      
      // copy states
      for (int i = 0; i < pAutomaton.mStates.size() - 1; i++) {
        mAutomaton.createState();
      }
      
      // copy transitions
      for (State lState : pAutomaton.mStates) {
        int lIndex1;
        
        if (lState.isInitial()) {
          lIndex1 = mIndex;
        }
        else {
          lIndex1 = lOffset + lState.getIndex();
        }
        
        State lLocalState = mStates.get(lIndex1);
        
        for (Pair<Label<E>, State> lEntry : lState.mOutgoingTransitions) {
          State lTmpState = lEntry.getSecond();
          
          int lIndex2;
          
          if (lTmpState.isInitial()) {
            lIndex2 = mIndex;
          }
          else {
            lIndex2 = lOffset + lTmpState.getIndex();
          }
          
          lLocalState.addTransition(lEntry.getFirst(), lIndex2);
        }
        
        if (lState.isFinal()) {
          lLocalState.setFinal();
        }
      }
    }
    
    // TODO: Provide bit vector implementation
    // because test goal automaton can become huge.
    public Collection<State> getSuccessors(E pE) {
      Collection<State> lSuccessors = new ArrayList<State>();
      
      for (Pair<Label<E>, State> lEntry : mOutgoingTransitions) {
        if (lEntry.getFirst().matches(pE)) {
          lSuccessors.add(lEntry.getSecond());
        }
      }
      
      return lSuccessors;
    }
    
    // only simple syntactical check
    public boolean hasUnconditionalSelfLoop() {
      for (Pair<Label<E>, State> lOutgoingTransition : mOutgoingTransitions) {
        if (lOutgoingTransition.getFirst().equals(mAutomaton.mTrueLabel) && 
            lOutgoingTransition.getSecond().equals(this)) {
          return true;
        }
      }
      
      return false;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      
      if (!(o instanceof Automaton<?>.State)) {
        return false;
      }
      
      State lState = (State)o;
      
      if (!mAutomaton.equals(lState.mAutomaton)) {
        return false;
      }
      
      if (mIndex != lState.mIndex) {
        return false;
      }
      
      return true;
    }
    
    @Override
    public int hashCode() {
      return mIndex;
    }
    
    @Override
    public String toString() {
      String result = "node [shape=";
      
      if (isInitial()) {
        if (isFinal()) {
          result += "doubleoctagon";
        }
        else {
          result += "octagon";
        }
      }
      else if (isFinal()) {
        result += "doublecircle";
      }
      else {
        result += "circle";
      }
      
      result += ", label=\"q";
      
      result += getIndex();
      
      result += "\"]; q";
      
      result += getIndex();
      
      result += ";";
      
      return result;
    }
  }
  
  private State mInitialState;
  private Vector<State> mStates;
  private Set<State> mFinalStates;
  private final TrueLabel<E> mTrueLabel;
  
  public Automaton() {
    mStates = new Vector<State>(); 
    mInitialState = new State(this, mStates.size());
    mFinalStates = new HashSet<State>();
    mTrueLabel = new TrueLabel<E>();
    
    mStates.add(mInitialState);
  }
  
  public Automaton(Collection<Label<E>> pLabels) {
    this();
    
    assert(pLabels != null);
    
    for (Label<E> lLabel : pLabels) {
      State lSuccessor = createState();
      lSuccessor.setFinal();
      
      mInitialState.addTransition(lLabel, lSuccessor);
    }
  }
  
  public Automaton(Automaton<E> pAutomaton) {
    this();
    
    assert(pAutomaton != null);
    
    // copy states
    for (int i = 0; i < pAutomaton.mStates.size() - 1; i++) {
      createState();
    }
    
    // copy transitions
    for (State lState : pAutomaton.mStates) {
      State lLocalState = mStates.get(lState.getIndex());
      
      for (Pair<Label<E>, State> lEntry : lState.mOutgoingTransitions) {
        lLocalState.addTransition(lEntry.getFirst(), lEntry.getSecond().getIndex());
      }
      
      if (lState.isFinal()) {
        lLocalState.setFinal();
      }
    }
  }
  
  public Automaton<E> createUnion(Automaton<E> pAutomaton) {
    assert(pAutomaton != null);
    
    Automaton<E> lAutomaton = new Automaton<E>(this);
    
    int lOffset = lAutomaton.mStates.size() - 1;
    
    // copy states
    for (int i = 0; i < pAutomaton.mStates.size() - 1; i++) {
      lAutomaton.createState();
    }
    
    // copy transitions
    for (State lState : pAutomaton.mStates) {
      int lIndex1;
      
      if (lState.isInitial()) {
        lIndex1 = 0;
      }
      else {
        lIndex1 = lOffset + lState.getIndex();
      }
      
      State lLocalState = lAutomaton.mStates.get(lIndex1);
      
      for (Pair<Label<E>, State> lEntry : lState.mOutgoingTransitions) {
        State lTmpState = lEntry.getSecond();
        
        int lIndex2;
        
        if (lTmpState.isInitial()) {
          lIndex2 = 0;
        }
        else {
          lIndex2 = lOffset + lTmpState.getIndex();
        }
        
        lLocalState.addTransition(lEntry.getFirst(), lIndex2);
      }
      
      if (lState.isFinal()) {
        lLocalState.setFinal();
      }
    }
    
    return lAutomaton;
  }
  
  public Automaton<E> createSequence(Automaton<E> pAutomaton) {
    Collection<Label<E>> lLabels = new ArrayList<Label<E>>();
    
    return createRestrictedSequence(pAutomaton, lLabels);
  }
  
  public Automaton<E> createRestrictedSequence(Automaton<E> pAutomaton, Collection<Label<E>> pLabels) {
    assert(pAutomaton != null);
    assert(pLabels != null);
    
    Label<E> lLabel = null;
    
    if (pLabels.isEmpty()) {
      lLabel = mTrueLabel;
    }
    else {
      boolean first = true;
      
      for (Label<E> lGivenLabel : pLabels) {
        assert(lGivenLabel != null);
        
        if (first) {
          lLabel = lGivenLabel;
          
          first = false;
        }
        else {
          lLabel = new OrLabel<E>(lLabel, lGivenLabel);
        }
      }
    }
    
    Automaton<E> lAutomaton = new Automaton<E>(this);
    
    Set<State> lFinalStates = new HashSet<State>(lAutomaton.getFinalStates());
    
    for (State lFinalState : lFinalStates) {
      // this state is not final anymore
      // this has to be done since lFinalState can become final
      // again during add
      lFinalState.unsetFinal();
      
      // append pAutomaton to every final state
      lFinalState.add(pAutomaton);
      
      // add self-loop
      lFinalState.addSelfLoop(lLabel);
    }
    
    return lAutomaton;
  }
  
  public Automaton<E> createMaximumIteration(int k) {
    assert(k >= 0);
    
    if (k == 0) {
      Automaton<E> lAutomaton = new Automaton<E>();
      
      lAutomaton.mInitialState.setFinal();
      
      return lAutomaton;
    }
    else if (k == 1) {
      return new Automaton<E>(this);
    }
    else {
      Automaton<E> lAutomatonRec = createMaximumIteration(k - 1);
      
      Automaton<E> lAutomaton = new Automaton<E>(this);
      
      for (State lFinalState : lAutomaton.mFinalStates) {
        lFinalState.add(lAutomatonRec);
      }
      
      lAutomaton.mInitialState.setFinal();
      
      return lAutomaton;
    }
  }
  
  public Automaton<E> createExactIteration(int k) {
    assert(k >= 0);
    
    if (k == 0) {
      Automaton<E> lAutomaton = new Automaton<E>();
      
      lAutomaton.mInitialState.setFinal();
      
      return lAutomaton;
    }
    else if (k == 1) {
      return new Automaton<E>(this);
    }
    else {
      Automaton<E> lAutomatonRec = createExactIteration(k - 1);
      
      Automaton<E> lAutomaton = new Automaton<E>(this);
      
      Set<State> lFinalStates = new HashSet<State>(lAutomaton.mFinalStates);
      
      for (State lFinalState : lFinalStates) {
        lFinalState.unsetFinal();
        
        lFinalState.add(lAutomatonRec);
      }
      
      return lAutomaton;
    }
  }
  
  public Automaton<E> createMinimumIteration(int k) {
    assert(k >= 0);
    
    if (k == 0) {
      Automaton<E> lAutomaton = new Automaton<E>();
      
      lAutomaton.mInitialState.setFinal();
      
      return lAutomaton;
    }
    else if (k == 1) {
      return new Automaton<E>(this);
    }
    else {
      Automaton<E> lAutomatonK = createExactIteration(k);
      
      Automaton<E> lModifiedAutomaton = new Automaton<E>();
      
      // build new automaton
      Map<Integer, Integer> lIndicesMap = new HashMap<Integer, Integer>();

      lIndicesMap.put(0, 0);
      
      for (State lState : mStates) {
        State lModifiedAutomatonState;
        
        if (lState.isFinal()) {
          lModifiedAutomatonState = lModifiedAutomaton.getInitialState();
        }
        else {
          if (lIndicesMap.containsKey(lState.getIndex())) {
            lModifiedAutomatonState = lModifiedAutomaton.mStates.get(lIndicesMap.get(lState.getIndex()));
          }
          else {
            lModifiedAutomatonState = lModifiedAutomaton.createState();
            lIndicesMap.put(lState.getIndex(), lModifiedAutomatonState.getIndex());
          }          
        }
        
        for (Pair<Label<E>, State> lEntry : lState.mOutgoingTransitions) {
          State lTmpState = lEntry.getSecond();
          
          State lCurrentState;
          
          if (lTmpState.isFinal()) {
            lCurrentState = lModifiedAutomaton.getInitialState();
          }
          else {
            if (lIndicesMap.containsKey(lTmpState.getIndex())) {
              lCurrentState = lModifiedAutomaton.mStates.get(lIndicesMap.get(lTmpState.getIndex()));
            }
            else {
              lCurrentState = lModifiedAutomaton.createState();
              lIndicesMap.put(lTmpState.getIndex(), lCurrentState.getIndex());
            }
          }
          
          lModifiedAutomatonState.addTransition(lEntry.getFirst(), lCurrentState);
        }
      }
      
      // we do not have any final state in lModifiedAutomaton
      // so, we do not change the set of final states of 
      // lAutomatonK while adding lModifiedAutomaton to its
      // final states
      assert(lModifiedAutomaton.getFinalStates().isEmpty());
      
      for (State lFinalState : lAutomatonK.getFinalStates()) {
        lFinalState.add(lModifiedAutomaton);
      }
      
      return lAutomatonK;
    }
  }
  
  public State createState() {
    State lState = new State(this, mStates.size());
    
    mStates.add(lState);
    
    return lState;
  }
  
  public int getNumberOfStates() {
    return mStates.size();
  }
  
  public State getInitialState() {
    return mInitialState;
  }
  
  public Set<State> getFinalStates() {
    return mFinalStates;
  }
  
  @Override
  public String toString() {
    String result = "digraph automaton {\n";
    
    for (State lState : mStates) {
      result += lState.toString();
      result += "\n";
    }
    
    result += "\n";
    
    for (State lState : mStates) {
      for (Pair<Label<E>, State> lEntry : lState.mOutgoingTransitions) {
        result += "q" + lState.getIndex() + " -> q" + lEntry.getSecond().getIndex() + " [label=\"" + lEntry.getFirst().toString() + "\"];\n";
      }
    }
    
    result += "}\n";
    
    return result;
  }
  
  public Automaton<E> getSimplifiedAutomaton() {
    Stack<State> lStack = new Stack<State>();
    lStack.addAll(getFinalStates());
    
    Set<State> lVisitedStates = new HashSet<State>();
    
    while (!lStack.isEmpty()) {
      State lState = lStack.pop();
      
      lVisitedStates.add(lState);
      
      for (Pair<Label<E>, State> lPair : lState.mIncomingTransitions) {
        State lSourceState = lPair.getSecond();
        
        // this state is already marked as reachable
        if (lVisitedStates.contains(lSourceState)) {
          continue;
        }
        
        // this state will be marked as reachable
        if (lStack.contains(lSourceState)) {
          continue;
        }
        
        lStack.add(lSourceState);
      }
    }
    
    if (!lVisitedStates.contains(getInitialState())) {
      // return dummy automaton since no final state is reachable
      return new Automaton<E>();
    }
    
    // there is a final state reachable from the initial state
    // create an automaton based on this that only involves 
    // states from lVisitedStates
    Map<State, State> lStateMap = new HashMap<State, State>();
    
    Automaton<E> lAutomaton = new Automaton<E>();
    
    lStateMap.put(getInitialState(), lAutomaton.getInitialState());
    
    for (State lState : lVisitedStates) {
      State lNewState;
      
      if (!lStateMap.containsKey(lState)) {
        lNewState = lAutomaton.createState();
        lStateMap.put(lState, lNewState);
      }
      else {
        lNewState = lStateMap.get(lState);
      }
      
      if (lState.isFinal()) {
        lNewState.setFinal();
      }
      
      for (Pair<Label<E>, State> lTransition : lState.mOutgoingTransitions) {
        State lTargetState = lTransition.getSecond();
        
        if (lVisitedStates.contains(lTargetState)) {
          State lNewTargetState;
          
          if (!lStateMap.containsKey(lTargetState)) {
            lNewTargetState = lAutomaton.createState();
            lStateMap.put(lTargetState, lNewTargetState);
          }
          else {
            lNewTargetState = lStateMap.get(lTargetState);
          }
          
          lNewState.addTransition(lTransition.getFirst(), lNewTargetState);
        }
      }
    }
    
    return lAutomaton;
  }
}
