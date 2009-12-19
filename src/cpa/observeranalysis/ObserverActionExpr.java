package cpa.observeranalysis;

import java.util.Map;


abstract class ObserverActionExpr {
  private ObserverActionExpr() {};
  abstract void execute();
  
  static class Assignment extends ObserverActionExpr {
    private Map<String, ObserverVariable> vars;
    private String varId;
    private ObserverIntExpr var;
    public Assignment(String varId, ObserverIntExpr var, Map<String, ObserverVariable> vars) {
      this.varId = varId;
      this.var = var;
      this.vars = vars;
    }
    @Override  void execute() {
      if (vars.containsKey(varId)) {
        vars.get(varId).setValue(var.eval());
      } else {
        ObserverVariable newVar = new ObserverVariable("int", varId);
        newVar.setValue(var.eval());
        vars.put(varId, newVar);
        System.out.println("Defined a Variable " + varId + " that was unknown before (not set in automaton Definition).");
      }
    }
  }
}
