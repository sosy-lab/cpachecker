package cpa.observeranalysis;

import java.util.Map;


/**
 * Implements an Action with side-effects that has no return value.
 * The Action can be executed multiple times.
 */
abstract class ObserverActionExpr {
  private ObserverActionExpr() {};
  abstract void execute();
  
  /**
   * Prints a string to System.out when executed.
   * @author rhein
   */
  static class Print extends ObserverActionExpr {
    private String toPrint;
    public Print(String pToPrint) { toPrint = pToPrint; }
    @Override void execute() { System.out.println(toPrint); }
  }
  
  /** Assigns the value of a ObserverIntExpr to a ObserverVariable determined by its name.
   * @author rhein
   */
  static class Assignment extends ObserverActionExpr {
    private Map<String, ObserverVariable> vars;
    private String varId;
    private ObserverIntExpr var;
    public Assignment(String pVarId, ObserverIntExpr pVar, Map<String, ObserverVariable> pVars) {
      this.varId = pVarId;
      this.var = pVar;
      this.vars = pVars;
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
