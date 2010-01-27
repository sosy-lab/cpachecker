package cpa.observeranalysis;

import java.util.Map;

/**
 * Implements a integer expression that evaluates and returns a <code>int</code> value when <code>eval()</code> is called.
 * The Expression can be evaluated multiple times.
 * @author rhein
 */
abstract class ObserverIntExpr {
    private ObserverIntExpr() {} //nobody can use this
    abstract int eval(Map<String, ObserverVariable> pVars);
    /** Stores a constant integer.
     * @author rhein
     */
    static class Constant extends ObserverIntExpr {
      int i;
      public Constant(int pI) {this.i = pI; }
      public Constant(String pI) {this.i = Integer.parseInt(pI); }
      @Override public int eval(Map<String, ObserverVariable> pVars) {return i; }
    }
    /** Loads an {@link ObserverVariable} from the VariableMap and returns its int value. 
     * @author rhein
     */
    static class VarAccess extends ObserverIntExpr {
      String varId;
      public VarAccess(String pId) {this.varId = pId; }
      @Override public int eval(Map<String, ObserverVariable> pVars) {
        return pVars.get(varId).getValue(); // only ints supported so far
      }
    }
    /** Addition of {@link ObserverIntExpr} instances. 
     * @author rhein
     */
    static class Plus extends ObserverIntExpr {
      ObserverIntExpr a, b;
      public Plus(ObserverIntExpr pA, ObserverIntExpr pB) {this.a = pA; this.b = pB;}
      @Override public int eval(Map<String, ObserverVariable> pVars) {
        return a.eval(pVars) + b.eval(pVars);
      }
    }
    /** Subtraction of {@link ObserverIntExpr} instances.
     * @author rhein
     */
    static class Minus extends ObserverIntExpr {
      ObserverIntExpr a, b;
      public Minus(ObserverIntExpr pA, ObserverIntExpr pB) {this.a = pA; this.b = pB;}
      @Override public int eval(Map<String, ObserverVariable> pVars) {
        return a.eval(pVars) - b.eval(pVars);
      }
    }
}