package fql.backend.testgoals;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import common.Pair;

import fql.backend.pathmonitor.Automaton;
import fql.backend.targetgraph.TargetGraph;
import fql.frontend.ast.coverage.Coverage;
import fql.frontend.ast.coverage.Sequence;
import fql.frontend.ast.filter.Identity;
import fql.frontend.ast.pathmonitor.LowerBound;
import fql.frontend.ast.pathmonitor.PathMonitor;

public class CoverageSequence implements Iterable<Pair<Automaton, Set<? extends TestGoal>>> {
  private Automaton mFinalMonitor;
  private LinkedList<Pair<Automaton, Set<? extends TestGoal>>> mSequence;
  
  private CoverageSequence() {
    mSequence = new LinkedList<Pair<Automaton, Set<? extends TestGoal>>>();
  }
  
  private CoverageSequence(Automaton pInitialAutomaton, Set<? extends TestGoal> pTestGoals, Automaton pFinalAutomaton) {
    assert(pInitialAutomaton != null);
    assert(pTestGoals != null);
    assert(pFinalAutomaton != null);
    
    mSequence = new LinkedList<Pair<Automaton, Set<? extends TestGoal>>>();
    
    mSequence.add(new Pair<Automaton, Set<? extends TestGoal>>(pInitialAutomaton, pTestGoals));
    
    mFinalMonitor = pFinalAutomaton;
  }
  
  public Automaton getFinalMonitor() {
    return mFinalMonitor;
  }
  
  public int size() {
    return mSequence.size();
  }
  
  public Pair<Automaton, Set<? extends TestGoal>> get(int pIndex) {
    assert(0 <= pIndex);
    assert(pIndex < mSequence.size());
    
    return mSequence.get(pIndex);
  }
  
  @Override
  public String toString() {
    StringBuffer lBuffer = new StringBuffer();
    
    lBuffer.append("<");
    
    boolean isFirst = true; 
    
    for (Pair<Automaton, Set<? extends TestGoal>> lPair : mSequence) {
      Automaton lMonitor = lPair.getFirst();
      Set<? extends TestGoal> lTestGoal = lPair.getSecond();
      
      if (isFirst) {
        isFirst = false;
      }
      else {
        lBuffer.append(",");
      }
      
      lBuffer.append(lMonitor.toString());
      lBuffer.append(",");
      lBuffer.append(lTestGoal.toString());
    }

    lBuffer.append(",");
    lBuffer.append(mFinalMonitor.toString());
    lBuffer.append(">");
    
    return lBuffer.toString();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      CoverageSequence lSequence = (CoverageSequence)pOther;
      
      return lSequence.mSequence.equals(mSequence) 
              && lSequence.mFinalMonitor.equals(mFinalMonitor);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 398231 + mSequence.hashCode() + mFinalMonitor.hashCode();
  }

  @Override
  public Iterator<Pair<Automaton, Set<? extends TestGoal>>> iterator() {
    return mSequence.iterator();
  }
  
  public static CoverageSequence create(TargetGraph pTargetGraph, Coverage pCoverageSpecification) {
    assert(pTargetGraph != null);
    assert(pCoverageSpecification != null);
    
    if (pCoverageSpecification instanceof Sequence) {
      CoverageSequence lCoverageSequence = new CoverageSequence();
      
      Sequence lSequenceSpecification = (Sequence)pCoverageSpecification;
      
      for (Pair<PathMonitor, Coverage> lPair : lSequenceSpecification) {
        PathMonitor lMonitor = lPair.getFirst();
        Coverage lCoverageSpecification = lPair.getSecond();
        
        Automaton lAutomaton = Automaton.create(lMonitor, pTargetGraph);
        
        Set<? extends TestGoal> lTestGoals = pTargetGraph.apply(lCoverageSpecification);
        
        lCoverageSequence.mSequence.add(new Pair<Automaton, Set<? extends TestGoal>>(lAutomaton, lTestGoals));
      }
      
      Automaton lFinalAutomaton = Automaton.create(lSequenceSpecification.getFinalMonitor(), pTargetGraph);
      
      lCoverageSequence.mFinalMonitor = lFinalAutomaton;
      
      return lCoverageSequence;
    }
    else {
      // simple coverage specification
      Set<? extends TestGoal> lTestGoals = pTargetGraph.apply(pCoverageSpecification);
      
      PathMonitor lIdStarMonitor = new LowerBound(Identity.getInstance(), 0);
      
      Automaton lAutomaton = Automaton.create(lIdStarMonitor, pTargetGraph);
      
      return new CoverageSequence(lAutomaton, lTestGoals, lAutomaton);
    }
  }
}

