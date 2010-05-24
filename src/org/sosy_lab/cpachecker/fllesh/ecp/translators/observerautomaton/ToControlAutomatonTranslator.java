package org.sosy_lab.cpachecker.fllesh.ecp.translators.observerautomaton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.fllesh.cpa.edgevisit.ECPEdgeSetBasedAnnotations;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPGuard;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.ecp.reduced.Automaton;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedLabel;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.ToGuardedAutomatonTranslator;

public class ToControlAutomatonTranslator {
  
  private ECPEdgeSet mAlphaEdgeSet;
  private ECPEdgeSet mOmegaEdgeSet;
  private EdgeSetNameMap mEdgeSetNameMap;
  private ECPEdgeSetBasedAnnotations mAnnotations;
  
  public ToControlAutomatonTranslator(CFAEdge pAlphaEdge, CFAEdge pOmegaEdge) {
    mAlphaEdgeSet = new ECPEdgeSet(pAlphaEdge);
    mOmegaEdgeSet = new ECPEdgeSet(pOmegaEdge);
    mEdgeSetNameMap = new EdgeSetNameMap();
    mAnnotations = new ECPEdgeSetBasedAnnotations(mEdgeSetNameMap);
  }
  
  public static String getAcceptingStateName() {
    return "Accept";
  }
  
  public ECPEdgeSetBasedAnnotations getAnnotations() {
    return mAnnotations;
  }
  
  public String translate(ElementaryCoveragePattern pPattern, String pAutomatonName) {
    Automaton<GuardedLabel> lAutomaton = ToGuardedAutomatonTranslator.translate(pPattern);
    
    Automaton<GuardedLabel> lLambdaFreeAutomaton = ToGuardedAutomatonTranslator.removeLambdaEdges(lAutomaton, mAlphaEdgeSet, mOmegaEdgeSet);
    
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
    
    StateNameMap lStateNameMap = new StateNameMap(lFinalState);
    
    String lAlphaEdgeSetName = mEdgeSetNameMap.get(mAlphaEdgeSet);
    
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
        lWriter.print(getTransitions(lNodeSetFreeAutomaton.getOutgoingEdges(lState), mEdgeSetNameMap, lStateNameMap));
        if (lState.equals(lInitialState)) {      
          lWriter.println("  !CHECK(edgevisit(\"" + lAlphaEdgeSetName + "\")) -> GOTO " + lStateNameMap.get(lState) + ";");
        }
        lWriter.println("  TRUE -> STOP;");
      }
      
      lWriter.println();
    }
    
    // determine annotations
    for (Automaton<GuardedLabel>.Edge lEdge : lNodeSetFreeAutomaton.getEdges()) {
      GuardedLabel lLabel = lEdge.getLabel();
      
      assert(lLabel instanceof GuardedEdgeLabel);
      
      GuardedEdgeLabel lEdgeLabel = (GuardedEdgeLabel)lLabel;
      
      mAnnotations.add(lEdgeLabel.getEdgeSet());
    }
    
    return lResult.toString();
  }
  
  /**
   * 
   * @param pPattern        Elementary coverage pattern that gets translated into a control automaton.
   * @param pAlphaEdge      First edge that starts tracking with respect to pPattern.
   * @param pOmegaEdge      Finally edge that has to be passed by the control automaton to get into an accepting state.
   * @param pAutomatonName  Name of the generated control automaton.
   * @return                A temporary file containing the sources of a control automaton corresponding to pPattern.
   */
  public File getControlAutomatonFile(ElementaryCoveragePattern pPattern, String pAutomatonName) {
    String lControlAutomatonString = translate(pPattern, pAutomatonName);
    
    File lControlAutomatonFile;
    try {
      lControlAutomatonFile = File.createTempFile("fllesh." + pAutomatonName + ".", ".ca");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    //lControlAutomatonFile.deleteOnExit();
    
    PrintStream lControlAutomaton;
    try {
      lControlAutomaton = new PrintStream(new FileOutputStream(lControlAutomatonFile));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    lControlAutomaton.println(lControlAutomatonString);
    lControlAutomaton.close();
    
    return lControlAutomatonFile;
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
  
  private class StateNameMap {
    private Map<Automaton<GuardedLabel>.State, String> mStateNames = new HashMap<Automaton<GuardedLabel>.State, String>();
    
    public StateNameMap(Automaton<GuardedLabel>.State pFinalState) {
      // We need a special name for the accepting state to be able
      // to refer to it in the product automaton CPA.
      mStateNames.put(pFinalState, getAcceptingStateName());
    }
    
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
  
  public class EdgeSetNameMap {
    private Map<ECPEdgeSet, String> mEdgeSetNames = new HashMap<ECPEdgeSet, String>();
    
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
