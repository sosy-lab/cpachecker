package symbpredabstraction;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;

public class AbstractionLocationPointer {

	private SymbolicFormula symbFormula;
	private SSAMap ssa;
	private CFANode abstractionLocation;

	public AbstractionLocationPointer(SymbolicFormula formula, SSAMap ssaMap, CFANode node){
		symbFormula = formula;
		ssa = ssaMap;
		abstractionLocation = node;
	}

	public SymbolicFormula getSymbolicFormula(){
		return symbFormula;
	}
	
	public SSAMap getSSA(){
		return ssa;
	}
	
	public CFANode getAbstractionLocation(){
		return abstractionLocation;
	}
	
}
