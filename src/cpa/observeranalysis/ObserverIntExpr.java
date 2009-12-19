package cpa.observeranalysis;

import java.util.Map;

abstract class ObserverIntExpr {
    private ObserverIntExpr() {} //nobody can use this
    abstract int eval();
    @Override
    public boolean equals(Object pObj) {
      if (pObj instanceof ObserverIntExpr)
        return this.eval() == ((ObserverIntExpr)pObj).eval();
      else return super.equals(pObj);
    }
    static class Constant extends ObserverIntExpr {
      int i;
      public Constant(int i) {this.i = i; }
      public Constant(String i) {this.i = Integer.parseInt(i); }
      @Override public int eval() {return i; }
    }
    static class VarAccess extends ObserverIntExpr {
      Map<String, ObserverVariable> varsMap;
      String varId;
      public VarAccess(String id, Map<String, ObserverVariable> map) {this.varId = id; this.varsMap = map; }
      @Override public int eval() {
        return varsMap.get(varId).getValue(); // only ints supported so far
      }
    }
    static class Plus extends ObserverIntExpr {
      ObserverIntExpr a, b;
      public Plus(ObserverIntExpr a, ObserverIntExpr b) {this.a = a; this.b = b;}
      @Override public int eval() {
        return a.eval() + b.eval();
      }
    }
}