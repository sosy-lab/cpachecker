package org.sosy_lab.cpachecker.fllesh.ecp.reduced;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Automaton<T> {
  
  public class State {
    
    private State() {
      
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
      return mSource.hashCode() + mTarget.hashCode();
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      
      if (pOther == null) {
        return false;
      }
      
      try {
        Edge lEdge = (Edge)pOther;
        
        return lEdge.mSource.equals(mSource) && lEdge.mTarget.equals(mTarget) && lEdge.getLabel().equals(mLabel);
      }
      catch (ClassCastException lException) {
        return false;
      }
    }
  }
  
  private class LambdaEdge extends Edge {
    
    private LambdaEdge(State pSource, State pTarget) {
      super(pSource, pTarget, null);
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      
      if (pOther == null) {
        return false;
      }
      
      try {
        LambdaEdge lEdge = (LambdaEdge)pOther;
        
        return lEdge.getSource().equals(getSource()) && lEdge.getTarget().equals(getTarget());
      }
      catch (ClassCastException lException) {
        return false;
      }
    }
    
    @Override
    public String toString() {
      return "Lambda";
    }
    
  }
  
  private State mInitialState;
  private HashSet<State> mFinalStates;
  
  private Map<State, Set<Edge>> mOutgoingEdges;
  private Map<State, Set<Edge>> mIncomingEdges;
  
  public Automaton() {
    mFinalStates = new HashSet<State>();
    
    mOutgoingEdges = new HashMap<State, Set<Edge>>();
    mIncomingEdges = new HashMap<State, Set<Edge>>();
    
    setInitialState(this.createState());
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
    mFinalStates = new HashSet<State>();
    mFinalStates.addAll(pFinalStates);
  }
  
  public void addToFinalStates(State pFinalStates) {
    mFinalStates.add(pFinalStates);
  }
  
  public Set<State> getStates() {
    return mOutgoingEdges.keySet();
  }
  
  public State createState() {
    State lState = new State();
    
    mOutgoingEdges.put(lState, new HashSet<Edge>());
    mIncomingEdges.put(lState, new HashSet<Edge>());

    return lState;
  }

  public LambdaEdge createLambdaEdge(State pState1, State pState2) {
    LambdaEdge lEdge = new LambdaEdge(pState1, pState2);
      
    mOutgoingEdges.get(pState1).add(lEdge);
    mIncomingEdges.get(pState2).add(lEdge);
    
    return lEdge;
  }
  
  /*
   * if pLabel is null it is treated as lambda.
   * 
   */
  public Edge createEdge(State pState1, State pState2, T pLabel) {
    Edge lEdge = new Edge(pState1, pState2, pLabel);
    
    mOutgoingEdges.get(pState1).add(lEdge);
    mIncomingEdges.get(pState2).add(lEdge);

    return lEdge;
  }
  
  public Set<Edge> getEdges() {
    Set<Edge> lEdges = new HashSet<Edge>();
    
    for (Set<Edge> lOutgoingEdges : mOutgoingEdges.values()) {
      lEdges.addAll(lOutgoingEdges);
    }
    
    return lEdges;
  }

  public Set<Edge> getOutgoingEdges(State pState) {
    return mOutgoingEdges.get(pState);
  }

  public Set<Edge> getIncomingEdges(State pState) {
    return mIncomingEdges.get(pState);
  }
  
  public Automaton<T> getLambdaFreeAutomaton() {
    Automaton<T> lLambdaFreeAutomaton = new Automaton<T>();
    
    Map<State, State> lStateMapping = new HashMap<State, State>();
    
    State lInitialState = getInitialState();
    
    lStateMapping.put(lInitialState, lLambdaFreeAutomaton.getInitialState());
    
    for (State lState : getStates()) {
      if (!lState.equals(lInitialState)) {
        lStateMapping.put(lState, lLambdaFreeAutomaton.createState());
      }
    }
    
    Set<State> lFinalStates = getFinalStates();
    
    for (State lFinalState : lFinalStates) {
      lLambdaFreeAutomaton.addToFinalStates(lStateMapping.get(lFinalState));
    }
    
    Set<State> lInitialClosure = getLambdaClosure(lInitialState);
    
    for (State lState : lInitialClosure) {
      if (lFinalStates.contains(lState)) {
        lLambdaFreeAutomaton.addToFinalStates(lInitialState);
      }
    }
    
    for (State lState : getStates()) {
      Set<State> lClosure = this.getLambdaClosure(lState);
      
      for (State lClosureElement : lClosure) {
        for (Edge lOutgoingEdge : getOutgoingEdges(lClosureElement)) {
          if (lOutgoingEdge.getLabel() != null) {
            // not a lambda edge
            State lTarget = lOutgoingEdge.getTarget();
            
            Set<State> lTargetClosure = this.getLambdaClosure(lTarget);
            
            for (State lTargetClosureElement : lTargetClosure) {
              lLambdaFreeAutomaton.createEdge(lStateMapping.get(lState), lStateMapping.get(lTargetClosureElement), lOutgoingEdge.getLabel());
            }
          }
        }
      }
    }
    
    return lLambdaFreeAutomaton;
  }
  
  /*private Set<State> getLambdaClosure(Set<State> pStates) {
    Set<State> lClosure = new HashSet<State>();
    
    for (State lState : pStates) {
      lClosure.addAll(getLambdaClosure(lState));
    }
    
    return lClosure;
  }*/
  
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
      
      for (Edge lOutgoingEdge : this.getOutgoingEdges(lCurrentState)) {
        if (lOutgoingEdge.getLabel() == null) {
          // lambda edge
          // TODO make this nicer (use of LambdaEdge)
          State lTarget = lOutgoingEdge.getTarget();
          
          lWorkset.add(lTarget);
        }
      }
      
    }
    
    return lClosure;
  }
  
  @Override
  public String toString() {
    Map<State, Integer> lIdMap = new HashMap<State, Integer>();
    
    for (State lState : getStates()) {
      lIdMap.put(lState, lIdMap.size());
    }
    
    StringBuffer lBuffer = new StringBuffer();
    
    lBuffer.append("Initial State: " + lIdMap.get(getInitialState()));
    lBuffer.append("\n");
    
    lBuffer.append("Final States: { ");
    
    boolean lFirst = true;
    
    for (State lFinalState : getFinalStates()) {
      if (lFirst) {
        lFirst = false;
      }
      else {
        lBuffer.append(", ");
      }
      
      lBuffer.append(lIdMap.get(lFinalState).toString());
    }
    
    lBuffer.append(" }\n");
    
    for (Edge lEdge : getEdges()) {
      T lLabel = lEdge.getLabel();
      
      String lLabelString;
      
      if (lLabel != null) {
        lLabelString = lLabel.toString();          
      }
      else {
        // TODO beautify
        lLabelString = "Lambda";
      }
      
      lBuffer.append(lIdMap.get(lEdge.getSource()) + " -[" + lLabelString + "]> " + lIdMap.get(lEdge.getTarget()));
      lBuffer.append("\n");
    }
    
    return lBuffer.toString();
  }
  
}

