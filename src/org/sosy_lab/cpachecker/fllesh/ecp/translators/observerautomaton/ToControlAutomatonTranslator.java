package org.sosy_lab.cpachecker.fllesh.ecp.translators.observerautomaton;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPGuard;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.ecp.reduced.Automaton;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedLabel;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.ToGuardedAutomatonTranslator;

public class ToControlAutomatonTranslator {

  public static String translate(ElementaryCoveragePattern lPattern, CFAEdge pAlphaEdge, CFAEdge pOmegaEdge, String pAutomatonName) {
    Automaton<GuardedLabel> lAutomaton = ToGuardedAutomatonTranslator.translate(lPattern);
    
    Automaton<GuardedLabel> lLambdaFreeAutomaton = ToGuardedAutomatonTranslator.removeLambdaEdges(lAutomaton, pAlphaEdge, pOmegaEdge);
    
    Automaton<GuardedLabel> lNodeSetFreeAutomaton = ToGuardedAutomatonTranslator.removeNodeSetGuards(lLambdaFreeAutomaton);
    
    Set<Automaton<GuardedLabel>.State> lFinalStates = lNodeSetFreeAutomaton.getFinalStates();
    
    if (lFinalStates.isEmpty()) {
      // TODO implement immediately stopping automaton
      throw new UnsupportedOperationException();
    }
    
    // per construction there is exactly one final state 
    assert(lFinalStates.size() == 1);
    
    Automaton<GuardedLabel>.State lFinalState = lFinalStates.iterator().next();
    
    assert(lNodeSetFreeAutomaton.getOutgoingEdges(lFinalState).isEmpty());
    
    Automaton<GuardedLabel>.State lInitialState = lNodeSetFreeAutomaton.getInitialState();
    
    StateNameMap lStateNameMap = new StateNameMap();
    EdgeSetNameMap lEdgeSetNameMap = new EdgeSetNameMap();
    
    ECPEdgeSet lAlphaEdgeSet = new ECPEdgeSet(pAlphaEdge);
    String lAlphaEdgeSetName = lEdgeSetNameMap.get(lAlphaEdgeSet);
    
    StringWriter lResult = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lResult);
    
    // create header
    lWriter.println("AUTOMATON " + pAutomatonName);
    lWriter.println("INITIAL STATE " + lStateNameMap.get(lInitialState) + ";");
    lWriter.println();
    
    // create body
    for (Automaton<GuardedLabel>.State lState : lNodeSetFreeAutomaton.getStates()) {
      
      if (lState.equals(lFinalState)) {
        lWriter.println("STATE " + lStateNameMap.get(lState) + ":");
        // we stay in the accepting state
        lWriter.println("  TRUE -> GOTO " + lStateNameMap.get(lState) + ";");
      }
      else {
        lWriter.println("STATE NONDET " + lStateNameMap.get(lState) + ":");
        lWriter.print(getTransitions(lNodeSetFreeAutomaton.getOutgoingEdges(lState), lEdgeSetNameMap, lStateNameMap));
        if (lState.equals(lInitialState)) {      
          lWriter.println("  !CHECK(edgevisit(\"" + lAlphaEdgeSetName + "\")) -> GOTO " + lStateNameMap.get(lState) + ";");
        }
        lWriter.println("  TRUE -> STOP;");
      }
      
      lWriter.println();
    }
    
    return lResult.toString();
  }
  
  private static String getTransitions(Set<Automaton<GuardedLabel>.Edge> pOutgoingEdges, EdgeSetNameMap pEdgeSetNameMap, StateNameMap pStateNameMap) {
    StringWriter lResult = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lResult);
    
    for (Automaton<GuardedLabel>.Edge lEdge : pOutgoingEdges) {
      GuardedEdgeLabel lLabel = (GuardedEdgeLabel)lEdge.getLabel();
      String lEdgeName = pEdgeSetNameMap.get(lLabel.getEdgeSet());
      
      lWriter.println("  CHECK(edgevisit(\"" + lEdgeName + "\")) -> " + getModifyString(lEdge.getLabel()) + "GOTO " + pStateNameMap.get(lEdge.getTarget()) + ";");
    }
    
    return lResult.toString();
  }
  
  private static String getModifyString(GuardedLabel pLabel) {
    StringBuffer lModification = new StringBuffer();
    
    for (ECPGuard lGuard : pLabel) {
      lModification.append("MODIFY(SymbPredAbsCPA(\"" + lGuard.toString() + "\")) ");
    }
    
    return lModification.toString();
  }
  
  private static class StateNameMap {
    Map<Automaton<GuardedLabel>.State, String> mStateNames = new HashMap<Automaton<GuardedLabel>.State, String>();
    
    public String get(Automaton<GuardedLabel>.State pState) {
      if (mStateNames.containsKey(pState)) {
        return mStateNames.get(pState);
      }
      else {
        String lName = "S" + mStateNames.size();
        mStateNames.put(pState, lName);
        return lName;
      }
    }
  }
  
  private static class EdgeSetNameMap {
    Map<ECPEdgeSet, String> mEdgeSetNames = new HashMap<ECPEdgeSet, String>();
    
    public String get(ECPEdgeSet pEdgeSet) {
      if (mEdgeSetNames.containsKey(pEdgeSet)) {
        return mEdgeSetNames.get(pEdgeSet);
      }
      else {
        String lName = "E" + mEdgeSetNames.size();
        mEdgeSetNames.put(pEdgeSet, lName);
        return lName;
      }
    }
  }
  
}
