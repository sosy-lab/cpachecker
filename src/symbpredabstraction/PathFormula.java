package symbpredabstraction;

import common.Pair;

import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormulaManager;

public class PathFormula {

  SSAMap ssa;
  SymbolicFormula symbolicFormula;

  public PathFormula(SymbolicFormula pf, SSAMap newssa){
    ssa = newssa;
    symbolicFormula = pf;
  }

  public SSAMap getSsa() {
    return ssa;
  }

  public void setSsa(SSAMap ssa) {
    this.ssa = ssa;
  }

  public SymbolicFormula getSymbolicFormula() {
    return symbolicFormula;
  }

  public PathFormula getInitSymbolicFormula(SymbolicFormulaManager mgr, boolean replace) {
    SSAMap ssa = new SSAMap();
    SymbolicFormula f = mgr.makeFalse();
    Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mp =
      mgr.mergeSSAMaps(ssa, this.getSsa(), false);
    SymbolicFormula curf = this.getSymbolicFormula();
    // TODO modified if
    if (replace) {
      curf = ((MathsatSymbolicFormulaManager)mgr).replaceAssignments((MathsatSymbolicFormula)curf);
    }
    f = mgr.makeAnd(f, mp.getFirst().getFirst());
    curf = mgr.makeAnd(curf, mp.getFirst().getSecond());
    f = mgr.makeOr(f, curf);
    ssa = mp.getSecond();
    return new PathFormula(f,ssa);
  }

  public void setSymbolicFormula(SymbolicFormula symFormula) {
    this.symbolicFormula = symFormula;
  }

  @Override
  public String toString(){
    return symbolicFormula.toString();
  }

}
