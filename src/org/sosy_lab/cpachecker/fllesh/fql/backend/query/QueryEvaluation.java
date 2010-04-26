package org.sosy_lab.cpachecker.fllesh.fql.backend.query;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor.Automaton;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.CoverageSequence;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.query.Query;

public class QueryEvaluation {
  
  public static Pair<CoverageSequence, Automaton> evaluate(Query pQuery, TargetGraph pTargetGraph) {
    CoverageSequence lCoverageSequence = CoverageSequence.create(pQuery.getCoverage(), pTargetGraph);
    
    Automaton lPassingAutomaton = Automaton.create(pQuery.getPassingMonitor(), pTargetGraph);
   
    return new Pair<CoverageSequence, Automaton>(lCoverageSequence, lPassingAutomaton);
  }
  
}
