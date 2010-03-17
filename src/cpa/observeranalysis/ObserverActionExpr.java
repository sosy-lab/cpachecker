package cpa.observeranalysis;

import java.util.Map;
import java.util.logging.Level;


/**
 * Implements an Action with side-effects that has no return value.
 * The Action can be executed multiple times.
 */
abstract class ObserverActionExpr {
  private ObserverActionExpr() {};
  abstract void execute(ObserverExpressionArguments pArgs);
  
  /**
   * Prints a string to System.out when executed.
   * @author rhein
   */
  static class Print extends ObserverActionExpr {
    private String toPrint;
    public Print(String pToPrint) { toPrint = pToPrint; }
    @Override void execute(ObserverExpressionArguments pArgs) { 
      if (toPrint.toLowerCase().equals("$rawstatement")) {
        pArgs.getLogger().log(Level.INFO, pArgs.getCfaEdge().getRawStatement());
      } else {
        pArgs.getLogger().log(Level.INFO, toPrint);
      }
    }
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
    @Override void execute(ObserverExpressionArguments pArgs) { 
      pArgs.getLogger().log(Level.INFO, toPrint.eval(pArgs));
    }
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
    @Override  void execute(ObserverExpressionArguments pArgs) {
      Map<String, ObserverVariable> vars = pArgs.getObserverVariables();
      if (vars.containsKey(varId)) {
        vars.get(varId).setValue(var.eval(pArgs));
      } else {
        ObserverVariable newVar = new ObserverVariable("int", varId);
        newVar.setValue(var.eval(pArgs));
        vars.put(varId, newVar);
        pArgs.getLogger().log(Level.WARNING, "Defined a Variable " + varId + " that was unknown before (not set in automaton Definition).");
      }
    }
  }
}
