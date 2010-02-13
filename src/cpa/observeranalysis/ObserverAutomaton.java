package cpa.observeranalysis;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * @author rhein
 */
class ObserverAutomaton {
  // default name of this automaton is "anonymous".
  private String name = "anonymous";
  /* The internal variables used by the actions/ assignments of this automaton.
   * This reference of the Map is unused because the actions/assignments get their reference from the parser.
   */
  private Map<String, ObserverVariable> initVars;
  private List<ObserverInternalState> states;
  private ObserverInternalState initState;

  public ObserverAutomaton(Map<String, ObserverVariable> pVars, List<ObserverInternalState> pStates,
      String pInit) {
    this.initVars = pVars;
    this.states = pStates;
    for (ObserverInternalState s : pStates) {
      if (s.getName().equals(pInit)) {
        this.initState = s;
      }
    }
    if (initState == null) {
      System.out.println("InitState not found. Going to ErrorState");
      initState = ObserverInternalState.ERR;
    }
    // implicit error State (might be followState of Transitions)
    pStates.add(ObserverInternalState.ERR);
    // i think i do not need to add TOP and BOTTOM
    
    // set the FollowStates of all Transitions
    for (ObserverInternalState s : pStates) {
      s.setFollowStates(pStates);
    }
  }  

  public void setName(String pName) {
    this.name = pName;
  }
  public String getName() {
    return name;
  }
  
  ObserverInternalState getInitialState() {
    return initState;
  }
  
  /**
   * Prints the contents of a DOT file representing this automaton to the PrintStream.
   * @param pOut
   */
  void writeDotFile(PrintStream pOut) {
    pOut.println("digraph " + name + "{");
    for (ObserverInternalState s : states) {
      if (initState.equals(s)) {
        pOut.println(s.getStateId() + " [shape=\"circle\" color=\"green\" label=\"" +  s.getName() + "\"]");
      } else {
        pOut.println(s.getStateId() + " [shape=\"circle\" color=\"black\" label=\"" +  s.getName() + "\"]");
      }
      s.writeTransitionsToDotFile(pOut);
    }
    pOut.println("}");
  }

  public Map<String, ObserverVariable> getInitialVariables() {
    return initVars;
  }
}
