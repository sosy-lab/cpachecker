package fql.fllesh;

import cpa.common.interfaces.AbstractElement;
import fql.backend.pathmonitor.Automaton;
import fql.backend.testgoals.TestGoal;

import java.util.Set;
import java.util.List;

import common.Pair;

public class TestGoalEnumeration {
  
  public static void run(List<Pair<Automaton, Set<? extends TestGoal>>> pCoverageSequence, Automaton pPassingMonitor, AbstractElement pInitialState) {
    assert(pCoverageSequence != null);
    assert(pPassingMonitor != null);
    //assert(pInitialState != null);
    
    
  }
  
}
