/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.automaton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NondeterministicFiniteAutomaton<T> {

  public static class State {

    public final int ID;

    private State(int pId) {
      ID = pId;
    }

  }

  private static class StatePool {

    private static class StateIterable implements Iterable<State> {

      private final NondeterministicFiniteAutomaton<?> mAutomaton;

      public StateIterable(NondeterministicFiniteAutomaton<?> pAutomaton) {
        mAutomaton = pAutomaton;
      }

      @Override
      public Iterator<State> iterator() {
        return new StateSetIterator(mAutomaton.mStatesCounter);
      }

    }

    private static class StateSetIterator implements Iterator<State> {

      private final int mSize;
      private int mCounter;

      public StateSetIterator(int pSize) {
        mSize = pSize;
        mCounter = 0;
      }

      @Override
      public boolean hasNext() {
        return (mCounter < mSize);
      }

      @Override
      public State next() {
        State lState = STATE_POOL.mPool.get(mCounter);
        mCounter++;

        return lState;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

    }

    private static final StatePool STATE_POOL = new StatePool();

    private ArrayList<State> mPool;
    private int mNextStateId;

    private StatePool() {
      mNextStateId = 0;
      mPool = new ArrayList<>();
    }

    public State get(NondeterministicFiniteAutomaton<?> pAutomaton) {
      State lState;

      if (pAutomaton.mStatesCounter == mPool.size()) {
        // we have to create a new state
        lState = new State(mNextStateId++);
        mPool.add(lState);
      } else {
        // the pool contains more states than the automaton
        // so return the next state not used in the automaton
        lState = mPool.get(pAutomaton.mStatesCounter);
      }

      // we add the state to the set of states of the automaton
      pAutomaton.mStatesCounter++;
      return lState;
    }
  }

  public class Edge {
    private State mSource;
    private State mTarget;
    private T mLabel;

    private Edge(State pSource, State pTarget, T pLabel) {
      mSource = pSource;
      mTarget = pTarget;
      mLabel = pLabel;
    }

    public State getSource() {
      return mSource;
    }

    public State getTarget() {
      return mTarget;
    }

    public T getLabel() {
      return mLabel;
    }

    @Override
    public int hashCode() {
      return 31 * mSource.ID + mTarget.ID;
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }

      if (pOther == null) {
        return false;
      }

      if (!pOther.getClass().equals(getClass())) {
        return false;
      }

      @SuppressWarnings("unchecked")
      Edge lEdge = (Edge) pOther;

      if (lEdge.mSource.equals(mSource) && lEdge.mTarget.equals(mTarget)) {
        if (mLabel == null) {
          return (lEdge.mLabel == null);
        } else {
          return mLabel.equals(lEdge.mLabel);
        }
      }

      return false;
    }
  }

  private State mInitialState;
  private HashSet<State> mFinalStates;

  private final ArrayList<Set<Edge>> mOutgoingEdges;
  private final ArrayList<Set<Edge>> mIncomingEdges;
  private final ArrayList<Edge> mEdges;

  private int mStatesCounter;
  private final StatePool.StateIterable mStateIterable;

  public NondeterministicFiniteAutomaton() {
    mStateIterable = new StatePool.StateIterable(this);

    mFinalStates = new HashSet<>();

    mOutgoingEdges = new ArrayList<>();
    mIncomingEdges = new ArrayList<>();

    mEdges = new ArrayList<>();

    setInitialState(createState());
  }

  public State getInitialState() {
    return mInitialState;
  }

  public Set<State> getFinalStates() {
    return mFinalStates;
  }

  public void setInitialState(State pInitialState) {
    mInitialState = pInitialState;
  }

  public void setFinalStates(Set<State> pFinalStates) {
    mFinalStates.clear();
    mFinalStates.addAll(pFinalStates);
  }

  public void addToFinalStates(State pFinalStates) {
    mFinalStates.add(pFinalStates);
  }

  public boolean isFinalState(State pState) {
    return mFinalStates.contains(pState);
  }

  public Iterable<State> getStates() {
    return mStateIterable;
  }

  public State createState() {
    State lState = StatePool.STATE_POOL.get(this);

    mOutgoingEdges.add(new HashSet<>());
    mIncomingEdges.add(new HashSet<>());

    return lState;
  }

  /*
   * if pLabel is null it is treated as lambda.
   */
  public Edge createEdge(State pSource, State pTarget, T pLabel) {
    Edge lEdge = new Edge(pSource, pTarget, pLabel);
    addEdge(pSource, pTarget, lEdge);

    return lEdge;
  }

  private void addEdge(State pSource, State pTarget, Edge pEdge) {
    mOutgoingEdges.get(pSource.ID).add(pEdge);
    mIncomingEdges.get(pTarget.ID).add(pEdge);
    mEdges.add(pEdge);
  }

  public Iterable<Edge> getEdges() {
    return mEdges;
  }

  public Collection<Edge> getOutgoingEdges(State pState) {
    return mOutgoingEdges.get(pState.ID);
  }

  public Collection<Edge> getIncomingEdges(State pState) {
    return mIncomingEdges.get(pState.ID);
  }

  public NondeterministicFiniteAutomaton<T> getLambdaFreeAutomaton() {
    NondeterministicFiniteAutomaton<T> lLambdaFreeAutomaton = new NondeterministicFiniteAutomaton<>();

    // mStatesCounter - 1 because the initial state of lLambdaFreeAutomaton
    // exists already
    for (int lIndex = 0; lIndex < mStatesCounter - 1; lIndex++) {
      lLambdaFreeAutomaton.createState();
    }

    // we use the same states for lLambdaFreeAutomaton so we can use our
    // final states as its final states
    Set<State> lFinalStates = getFinalStates();
    lLambdaFreeAutomaton.setFinalStates(getFinalStates());

    State lInitialState = getInitialState();

    for (State lState : getLambdaClosure(lInitialState)) {
      if (lFinalStates.contains(lState)) {
        lLambdaFreeAutomaton.addToFinalStates(lInitialState);
      }
    }

    for (State lState : getStates()) {
      for (State lClosureElement : getLambdaClosure(lState)) {
        for (Edge lOutgoingEdge : getOutgoingEdges(lClosureElement)) {
          if (lOutgoingEdge.getLabel() != null) {
            // not a lambda edge
            State lTarget = lOutgoingEdge.getTarget();

            for (State lTargetClosureElement : getLambdaClosure(lTarget)) {
              // again, we use the same states in lLambdaFreeAutomaton
              if (lOutgoingEdge.getSource().equals(lState) && lOutgoingEdge.getTarget().equals(lTargetClosureElement)) {
                lLambdaFreeAutomaton.addEdge(lState, lTargetClosureElement, lOutgoingEdge);
              } else {
                lLambdaFreeAutomaton.createEdge(lState, lTargetClosureElement, lOutgoingEdge.getLabel());
              }
            }
          }
        }
      }
    }

    return lLambdaFreeAutomaton;
  }

  private Set<State> getLambdaClosure(State pState) {
    Set<State> lClosure = new HashSet<>();
    Set<State> lWorkset = new HashSet<>();

    lWorkset.add(pState);

    while (!lWorkset.isEmpty()) {
      State lCurrentState = lWorkset.iterator().next();
      lWorkset.remove(lCurrentState);

      if (lClosure.contains(lCurrentState)) {
        continue;
      }

      lClosure.add(lCurrentState);

      for (Edge lOutgoingEdge : getOutgoingEdges(lCurrentState)) {
        if (lOutgoingEdge.getLabel() == null) {
          // lambda edge
          State lTarget = lOutgoingEdge.getTarget();

          lWorkset.add(lTarget);
        }
      }

    }

    return lClosure;
  }

  @Override
  public String toString() {
    StringBuilder lBuffer = new StringBuilder();

    lBuffer.append("Initial State: " + getInitialState().ID + "\n");

    lBuffer.append("Final States: { ");

    boolean lFirst = true;

    for (State lFinalState : getFinalStates()) {
      if (lFirst) {
        lFirst = false;
      } else {
        lBuffer.append(", ");
      }

      lBuffer.append(lFinalState.ID);
    }

    lBuffer.append(" }\n");

    for (Edge lEdge : getEdges()) {
      T lLabel = lEdge.getLabel();

      String lLabelString;

      if (lLabel != null) {
        lLabelString = lLabel.toString();
      } else {
        lLabelString = "Lambda";
      }

      lBuffer.append(lEdge.getSource().ID + " -[" + lLabelString + "]> " + lEdge.getTarget().ID);
      lBuffer.append("\n");
    }

    return lBuffer.toString();
  }

}

