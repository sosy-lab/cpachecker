package cpa.observeranalysis;

import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    // the pattern \$\d+ matches Expressions like $1 $2 $3
    static Pattern TRANSITION_VARS_PATTERN = Pattern.compile("\\$\\d+");
    private String toPrint;
    public Print(String pToPrint) { toPrint = pToPrint; }
    @Override void execute(ObserverExpressionArguments pArgs) { 
      if (toPrint.toLowerCase().equals("$rawstatement")) {
        pArgs.appendToLogMessage(pArgs.getCfaEdge().getRawStatement());
      } else {
        pArgs.appendToLogMessage(replaceVariables(pArgs, toPrint));
      }
    }
    static String replaceVariables (
        ObserverExpressionArguments pArgs, String str) {
      // replace Transition Variables
      Matcher matcher = TRANSITION_VARS_PATTERN.matcher(str);
      StringBuffer result = new StringBuffer();
      while (matcher.find()) {
        matcher.appendReplacement(result, "");
        String key = str.substring(matcher.start()+1, matcher.end());
        try {
          int varKey = Integer.parseInt(key);
          String var = pArgs.getTransitionVariable(varKey);
          if (var == null) {
            // this variable has not been set.
            pArgs.getLogger().log(Level.WARNING, "could not replace the transition variable $" + varKey + " (not found).");
            return null;
          } else {
            result.append(var);
          }
        } catch (NumberFormatException e) {
          pArgs.getLogger().log(Level.WARNING, "could not parse the int in " + matcher.group() + " , leaving it untouched");
          result.append(matcher.group());
        }
      }
      matcher.appendTail(result);
      return result.toString();
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
      pArgs.appendToLogMessage(toPrint.eval(pArgs));
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
