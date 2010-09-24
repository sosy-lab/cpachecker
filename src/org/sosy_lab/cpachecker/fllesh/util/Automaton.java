package org.sosy_lab.cpachecker.fllesh.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Automaton<T> {
  
  public static class State {
    
    public final int ID;
    
    private State(int pId) {
      ID = pId;
    }
    
  }
  
  private static class StatePool {
    
    private static class StateIterable implements Iterable<State> {

      private final Automaton<?> mAutomaton;
      
      public StateIterable(Automaton<?> pAutomaton) {
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
        return STATE_POOL.mPool.get(mCounter);
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
      mPool = new ArrayList<State>();
    }
    
    public State get(Automaton<?> pAutomaton) {
      State lState;
      
      if (pAutomaton.mStatesCounter == mPool.size()) {
        // we have to create a new state
        lState = new State(mNextStateId++);
        mPool.add(lState);
      }
      else {
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
      
      Edge lEdge = getClass().cast(pOther);
      
      if (lEdge.mSource.equals(mSource) && lEdge.mTarget.equals(mTarget)) {
        if (mLabel == null) {
          return (lEdge.mLabel == null);
        }
        else {
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
  
  public Automaton() {
    mStateIterable = new StatePool.StateIterable(this);
    
    mFinalStates = new HashSet<State>();
    
    mOutgoingEdges = new ArrayList<Set<Edge>>();
    mIncomingEdges = new ArrayList<Set<Edge>>();
    
    mEdges = new ArrayList<Edge>();
    
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
  
  public Iterable<State> getStates() {
    return mStateIterable;
  }
  
  public State createState() {
    State lState = StatePool.STATE_POOL.get(this);
    
    mOutgoingEdges.add(new HashSet<Edge>());
    mIncomingEdges.add(new HashSet<Edge>());
    
    return lState;
  }
  
  /*
   * if pLabel is null it is treated as lambda.
   * 
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
  
  public Automaton<T> getLambdaFreeAutomaton() {
    Automaton<T> lLambdaFreeAutomaton = new Automaton<T>();
    
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
              }
              else {
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
    Set<State> lClosure = new HashSet<State>();
    Set<State> lWorkset = new HashSet<State>();
    
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
    StringBuffer lBuffer = new StringBuffer();
    
    lBuffer.append("Initial State: " + getInitialState().ID + "\n");
    
    lBuffer.append("Final States: { ");
    
    boolean lFirst = true;
    
    for (State lFinalState : getFinalStates()) {
      if (lFirst) {
        lFirst = false;
      }
      else {
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
      }
      else {
        lLabelString = "Lambda";
      }
      
      lBuffer.append(lEdge.getSource().ID + " -[" + lLabelString + "]> " + lEdge.getTarget().ID);
      lBuffer.append("\n");
    }
    
    return lBuffer.toString();
  }
  
}

