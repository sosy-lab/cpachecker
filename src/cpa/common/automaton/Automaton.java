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
      
      mOutgoingTransitions.put(pLabel, pState);
    }
    
    public void addTransition(Label<E> pLabel, int pIndex) {
      assert(pIndex >= 0 && pIndex < mAutomaton.mStates.size());      
      
      addTransition(pLabel, mAutomaton.mStates.get(pIndex));
    }
    
    public void addSelfLoop(Label<E> pLabel) {
      assert(pLabel != null);
      
      mOutgoingTransitions.put(pLabel, this);
    }
    
    public void addUnconditionalSelfLoop() {
      addSelfLoop(mAutomaton.mTrueLabel);
    }
    
    public void add(Automaton<E> pAutomaton) {
      assert(pAutomaton != null);
      
      int lOffset = pAutomaton.mStates.size() - 1;
      
      // copy states
      for (int i = 0; i < lOffset; i++) {
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
        
        for (Map.Entry<Label<E>, State> lEntry : lState.mOutgoingTransitions.entrySet()) {
          State lTmpState = lEntry.getValue();
          
          int lIndex2;
          
          if (lTmpState.isInitial()) {
            lIndex2 = mIndex;
          }
          else {
            lIndex2 = lOffset + lTmpState.getIndex();
          }
          
          lLocalState.addTransition(lEntry.getKey(), lIndex2);
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
    
    @Override
    public String toString() {
      String result = "node [shape=";
      
      if (isInitial()) {
        result += "box";
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
      
      for (Map.Entry<Label<E>, State> lEntry : lState.mOutgoingTransitions.entrySet()) {
        lLocalState.addTransition(lEntry.getKey(), lEntry.getValue().getIndex());
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
    for (int i = 0; i < lOffset; i++) {
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
      
      for (Map.Entry<Label<E>, State> lEntry : lState.mOutgoingTransitions.entrySet()) {
        State lTmpState = lEntry.getValue();
        
        int lIndex2;
        
        if (lTmpState.isInitial()) {
          lIndex2 = 0;
        }
        else {
          lIndex2 = lOffset + lTmpState.getIndex();
        }
        
        lLocalState.addTransition(lEntry.getKey(), lIndex2);
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
      // append pAutomaton to every final state
      lFinalState.add(pAutomaton);
      
      // this state is not final anymore
      lFinalState.unsetFinal();
      
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
        
        for (Map.Entry<Label<E>, State> lEntry : lState.mOutgoingTransitions.entrySet()) {
          State lTmpState = lEntry.getValue();
          
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
          
          lModifiedAutomatonState.addTransition(lEntry.getKey(), lCurrentState);
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
      for (Map.Entry<Label<E>, State> lEntry : lState.mOutgoingTransitions.entrySet()) {
        result += "q" + lState.getIndex() + " -> q" + lEntry.getValue().getIndex() + " [label=\"" + lEntry.getKey().toString() + "\"];\n"; 
      }
    }
    
    result += "}\n";
    
    return result;
  }
}
