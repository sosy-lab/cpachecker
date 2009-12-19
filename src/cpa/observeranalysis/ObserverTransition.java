package cpa.observeranalysis;

import java.io.PrintStream;
import java.util.List;

import cfa.objectmodel.CFAEdge;

public class ObserverTransition {

  private String pattern;
  private List<ObserverBoolExpr> assertions;
  private List<ObserverActionExpr> actions;
  private String followStateName;
  private ObserverState followState;

  public ObserverTransition(String pattern, List<ObserverBoolExpr> assertions, List<ObserverActionExpr> actions,
      String followStateName) {
    this.pattern = pattern;
    this.assertions = assertions;
    this.actions = actions;
    this.followStateName = followStateName;
  }

  public void setFollowState(List<ObserverState> allStates) {
    for (ObserverState s : allStates) {
      if (s.getName().equals(followStateName)) {
        this.followState = s;
        return;
      }
    }
    System.err.println("No Follow-State found");
  }
  
  void writeTransitionToDotFile(int sourceStateId, PrintStream out) {
    out.println(sourceStateId + " -> " + followState.getStateId() + " [label=\"" + pattern + "\"]");
  }

  public boolean match(CFAEdge pCfaEdge) {
    //System.out.println("Raw Statement: " + pCfaEdge.getRawStatement());
    return pCfaEdge.getRawStatement().equals(pattern);
  }

  public boolean assertionsHold() {
    for (ObserverBoolExpr assertion : assertions) {
      if (assertion.eval() == false) {
        return false; // Lazy Evaluation
      }
    }
    return true;
  }
  
  public void executeActions() {
    for (ObserverActionExpr action : actions) {
      action.execute();
    }
  }

  public ObserverState getFollowState() {
    return followState;
  }

}
