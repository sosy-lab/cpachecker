package cpa.observeranalysis;

import java.util.Map;


/**
 * Implements an Action with side-effects that has no return value.
 * The Action can be executed multiple times.
 */
abstract class ObserverActionExpr {
  private ObserverActionExpr() {};
  abstract void execute(Map<String, ObserverVariable> pVars);
  
  /**
   * Prints a string to System.out when executed.
   * @author rhein
   */
  static class Print extends ObserverActionExpr {
    private String toPrint;
    public Print(String pToPrint) { toPrint = pToPrint; }
    @Override void execute(Map<String, ObserverVariable> pVars) { System.out.println(toPrint); }
  }
  
  /**
   * Prints the value of an ObserverIntExpr
   * @author rhein
   */
  static class PrintInt extends ObserverActionExpr {
    private ObserverIntExpr toPrint;
    public PrintInt(ObserverIntExpr pIntExpr) {
      toPrint = pIntExpr;
    }
    @Override void execute(Map<String, ObserverVariable> pVars) { System.out.println(toPrint.eval(pVars)); }
  }
  
  /** Assigns the value of a ObserverIntExpr to a ObserverVariable determined by its name.
   * @author rhein
   */
  static class Assignment extends ObserverActionExpr {
    private String varId;
    private ObserverIntExpr var;
    public Assignment(String pVarId, ObserverIntExpr pVar) {
      this.varId = pVarId;
      this.var = pVar;
    }
    @Override  void execute(Map<String, ObserverVariable> pVars) {
      if (pVars.containsKey(varId)) {
        pVars.get(varId).setValue(var.eval(pVars));
      } else {
        ObserverVariable newVar = new ObserverVariable("int", varId);
        newVar.setValue(var.eval(pVars));
        pVars.put(varId, newVar);
        System.out.println("Defined a Variable " + varId + " that was unknown before (not set in automaton Definition).");
      }
    }
  }
}
