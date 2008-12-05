/**
 * 
 */
package cpa.common.automaton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * @author holzera
 *
 */
public class Automaton<E> {
  public class State {
    private Automaton<E> mAutomaton;
    private int mIndex;
    private Map<Label<E>, State> mOutgoingTransitions;
    
    
    public State(Automaton<E> pAutomaton, int pIndex) {
      mAutomaton = pAutomaton;
      mIndex = pIndex;
      
      mOutgoingTransitions = new HashMap<Label<E>, State>();
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
      
      mOutgoingTransitions.put(pLabel, pState);
    }
    
    public void addSelfLoop(Label<E> pLabel) {
      assert(pLabel != null);
      
      mOutgoingTransitions.put(pLabel, this);
    }
    
    public void addUnconditionalSelfLoop() {
      addSelfLoop(mAutomaton.mTrueLabel);
    }
    
    // TODO: Provide bit vector implementation
    // because test goal automaton can become huge.
    public Collection<State> getSuccessors(E pE) {
      Collection<State> lSuccessors = new ArrayList<State>();
      
      for (Map.Entry<Label<E>, State> lEntry : mOutgoingTransitions.entrySet()) {
        if (lEntry.getKey().matches(pE)) {
          lSuccessors.add(lEntry.getValue());
        }
      }
      
      return lSuccessors;
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
  
  public State createState() {
    State lState = new State(this, mStates.size());
    
    mStates.add(lState);
    
    return lState;
  }
  
  public Set<State> getFinalStates() {
    return mFinalStates;
  }
}
