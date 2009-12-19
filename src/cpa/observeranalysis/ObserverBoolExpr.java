package cpa.observeranalysis;

abstract class ObserverBoolExpr {
  private ObserverBoolExpr() {} //nobody can use this
  abstract boolean eval();
  @Override
  public boolean equals(Object pObj) {
    if (pObj instanceof ObserverBoolExpr)
      return this.eval() == ((ObserverBoolExpr)pObj).eval();
    else return super.equals(pObj);
  }
  
  static class True extends ObserverBoolExpr {
    @Override boolean eval() {return true;}
  }
  
  static class False extends ObserverBoolExpr {
    @Override boolean eval() {return false;}
  }
  
  static class EqTest extends ObserverBoolExpr {
    ObserverIntExpr a, b;
    public EqTest(ObserverIntExpr a, ObserverIntExpr b) {this.a = a; this.b = b;}
    @Override boolean eval() {return a.eval() == b.eval();}
  }
}
