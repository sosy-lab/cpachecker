package cpa.observeranalysis;

/**
 * Implements a boolean expression that evaluates and returns a <code>boolean</code> value when <code>eval()</code> is called.
 * The Expression can be evaluated multiple times.
 * @author rhein
 */
abstract class ObserverBoolExpr {
  private ObserverBoolExpr() {} //nobody can use this
  abstract boolean eval();
  @Override
  public boolean equals(Object pObj) {
    if (pObj instanceof ObserverBoolExpr)
      return this.eval() == ((ObserverBoolExpr)pObj).eval();
    else return super.equals(pObj);
  }
  
  /** Constant for true.
   * @author rhein
   */
  static class True extends ObserverBoolExpr {
    @Override boolean eval() {return true;}
  }
  
  /** Constant for false.
   * @author rhein
   */
  static class False extends ObserverBoolExpr {
    @Override boolean eval() {return false;}
  }
  
  /** Tests the equality of the values of two instances of {@link ObserverIntExpr}.
   * @author rhein
   */
  static class EqTest extends ObserverBoolExpr {
    ObserverIntExpr a, b;
    public EqTest(ObserverIntExpr pA, ObserverIntExpr pB) {this.a = pA; this.b = pB;}
    @Override boolean eval() {return a.eval() == b.eval();}
  }
  /** Tests whether two instances of {@link ObserverIntExpr} evaluate to different integers.
   * @author rhein
   */
  static class NotEqTest extends ObserverBoolExpr {
    ObserverIntExpr a, b;
    public NotEqTest(ObserverIntExpr pA, ObserverIntExpr pB) {this.a = pA; this.b = pB;}
    @Override boolean eval() {return a.eval() != b.eval();}
  }
}
