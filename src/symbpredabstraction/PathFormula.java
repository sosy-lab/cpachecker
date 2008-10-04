package symbpredabstraction;

public class PathFormula {
	
	SSAMap ssa;
	SymbolicFormula pathFormula;
	
	public PathFormula(SymbolicFormula pf, SSAMap newssa){
		ssa = newssa;
		pathFormula = pf;
	}

	public SSAMap getSsa() {
		return ssa;
	}

	public void setSsa(SSAMap ssa) {
		this.ssa = ssa;
	}

	public SymbolicFormula getPathFormula() {
		return pathFormula;
	}

	public void setPathFormula(SymbolicFormula pathFormula) {
		this.pathFormula = pathFormula;
	}
}
