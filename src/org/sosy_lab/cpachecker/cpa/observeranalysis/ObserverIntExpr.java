package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Implements a integer expression that evaluates and returns a <code>int</code> value when <code>eval()</code> is called.
 * The Expression can be evaluated multiple times.
 * @author rhein
 */
abstract class ObserverIntExpr {
    
  private ObserverIntExpr() {} //nobody can use this
  
  abstract int eval(ObserverExpressionArguments pArgs);
  
  
  /** Stores a constant integer.
   * @author rhein
   */
  static class Constant extends ObserverIntExpr {
    int i;
    public Constant(int pI) {this.i = pI; }
    public Constant(String pI) {this.i = Integer.parseInt(pI); }
    public int eval() {return i;}
    @Override public int eval(ObserverExpressionArguments pArgs) {return i; }
  }

  
  /** Loads an {@link ObserverVariable} from the VariableMap and returns its int value. 
   * @author rhein
   */
  static class VarAccess extends ObserverIntExpr {
    
    private final String varId;

    private static Pattern TRANSITION_VARS_PATTERN = Pattern.compile("\\$\\d+");
    
    public VarAccess(String pId) {
      if (pId.startsWith("$$")) {
        // throws a NumberFormatException and this is good!
        Integer.parseInt(pId.substring(2));
      }
      this.varId = pId;
    }
    
    @Override
    public int eval(ObserverExpressionArguments pArgs) {
      if (TRANSITION_VARS_PATTERN.matcher(varId).matches()) { // $1  ObserverTransitionVariables
        // no exception here (would have come in the constructor)
        int key = Integer.parseInt(varId.substring(1));
        String val = pArgs.getTransitionVariable(key);
        if (val == null) {
          pArgs.getLogger().log(Level.WARNING, "could not find the transition variable $" + key + ". Returning -1.");
          return -1;
        }
        int value = -1;
        try {
          value = Integer.parseInt(val);
        } catch (NumberFormatException e) {
          pArgs.getLogger().log(Level.WARNING, "could not parse the contents of variable $" + key + "=\"" + val +"\". Returning -1.");
          return -1;
        }
        return value;
      } else if (varId.equals("$line")) { // $line  line number in sourcecode
        return pArgs.getCfaEdge().getLineNumber();
      } else {
        return pArgs.getObserverVariables().get(varId).getValue(); // only ints supported so far
      }
    }
    
    @Override
    public String toString() {
      return varId;
    }
  }
  
  
  /** Addition of {@link ObserverIntExpr} instances. 
   * @author rhein
   */
  static class Plus extends ObserverIntExpr {
    
    private final ObserverIntExpr a;
    private final ObserverIntExpr b;
    
    public Plus(ObserverIntExpr pA, ObserverIntExpr pB) {
      this.a = pA;
      this.b = pB;
    }
    
    @Override
    public int eval(ObserverExpressionArguments pArgs) {
      return a.eval(pArgs) + b.eval(pArgs);
    }
    
    @Override
    public String toString() {
      return "(" + a + " + " + b + ")";
    }
  }
  
  
  /** Subtraction of {@link ObserverIntExpr} instances.
   * @author rhein
   */
  static class Minus extends ObserverIntExpr {
    
    private final ObserverIntExpr a;
    private final ObserverIntExpr b;
    
    public Minus(ObserverIntExpr pA, ObserverIntExpr pB) {
      this.a = pA;
      this.b = pB;
    }
    
    @Override
    public int eval(ObserverExpressionArguments pArgs) {
      return a.eval(pArgs) - b.eval(pArgs);
    }
    
    @Override
    public String toString() {
      return "(" + a + " - " + b + ")";
    }
  }
}