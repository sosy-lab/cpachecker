package fllesh.fql.backend.query;

import common.Pair;

import fllesh.fql.backend.pathmonitor.Automaton;
import fllesh.fql.backend.targetgraph.TargetGraph;
import fllesh.fql.backend.testgoals.CoverageSequence;
import fllesh.fql.frontend.ast.query.Query;

public class QueryEvaluation {
  
  public static Pair<CoverageSequence, Automaton> evaluate(Query pQuery, TargetGraph pTargetGraph) {
    CoverageSequence lCoverageSequence = CoverageSequence.create(pQuery.getCoverage(), pTargetGraph);
    
    Automaton lPassingAutomaton = Automaton.create(pQuery.getPassingMonitor(), pTargetGraph);
   
    return new Pair<CoverageSequence, Automaton>(lCoverageSequence, lPassingAutomaton);
  }
  
}
