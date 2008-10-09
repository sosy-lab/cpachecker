package symbpredabstraction;

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

	public void setSymbolicFormula(SymbolicFormula symFormula) {
		this.symbolicFormula = symFormula;
	}
}
