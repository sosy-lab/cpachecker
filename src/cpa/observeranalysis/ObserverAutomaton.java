package cpa.observeranalysis;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

class ObserverAutomaton {
  private String name = "anonymous";
  private Map<String, ObserverVariable> vars;
  private List<ObserverState> states;
  private ObserverState initState;

  public ObserverAutomaton(Map<String, ObserverVariable> vars, List<ObserverState> states,
      ObserverState initState) {
    this.vars = vars;
    this.states = states;
    this.initState = initState;
    // implicit error State (might be followState of Transitions)
    states.add(ObserverState.ERR);
    // i think i do not need to add TOP and BOTTOM
    
    // set the FollowStates of all Transitions
    for (ObserverState s : states) {
      s.setFollowStates(states);
    }
  }  

  public void setName(String n) {
    this.name = n;
  }
  public String getName() {
    return name;
  }
  
  ObserverState getInitialState() {
    return initState;
  }
  
  void writeDotFile(PrintStream out) {
    out.println("digraph " + name + "{");
    for (ObserverState s : states) {
      if (initState.equals(s)) {
        out.println(s.getStateId() + " [shape=\"circle\" color=\"green\" label=\"" +  s.getName() + "\"]");
      } else {
        out.println(s.getStateId() + " [shape=\"circle\" color=\"black\" label=\"" +  s.getName() + "\"]");
      }
      s.writeTransitionsToDotFile(out);
    }
    out.println("}");
  }
}
