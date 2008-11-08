package cpaplugin.cpa.cpas.symbpredabsCPA;

import java.util.ArrayList;
import java.util.List;

import symbpredabstraction.MathsatSymbPredAbsFormulaManager;
import symbpredabstraction.MathsatSymbolicFormula;
import symbpredabstraction.Pair;
import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import symbpredabstraction.SymbolicFormula;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;

/**
 * trivial merge operation for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsMergeOperator implements MergeOperator {

	private SymbPredAbsAbstractDomain domain;

	private MathsatSymbPredAbsFormulaManager mgr; 

	public SymbPredAbsMergeOperator(SymbPredAbsAbstractDomain d) {
		domain = d;
		mgr = d.getCPA().getMathsatSymbPredAbsFormulaManager();
	}


	public AbstractDomain getAbstractDomain() {
		return domain;
	}

	public AbstractElement merge(AbstractElement element1,
			AbstractElement element2) {

		SymbPredAbsAbstractElement elem1 = (SymbPredAbsAbstractElement)element1;
		SymbPredAbsAbstractElement elem2 = (SymbPredAbsAbstractElement)element2;
		CFANode loc1 = elem1.getLocation();
		CFANode loc2 = elem2.getLocation();

		if(loc1.getNodeNumber() != loc2.getNodeNumber() || 
				!(elem1.getParents().equals(elem2.getParents()))){
			return element2;
		}
		else{
			boolean b = ((SymbPredAbsAbstractDomain)getAbstractDomain()).getCPA().isAbstractionLocation(loc1);
			SymbPredAbsAbstractElement merged = new SymbPredAbsAbstractElement(loc1, elem1.getAbstractionLocation()); 
			if(!b){
				// TODO check
				MathsatSymbolicFormula form1 = 
					(MathsatSymbolicFormula)elem1.getPathFormula().getSymbolicFormula();
				MathsatSymbolicFormula form2 = 
					(MathsatSymbolicFormula)elem2.getPathFormula().getSymbolicFormula();
				SSAMap ssa2 = elem2.getPathFormula().getSsa();
				SSAMap ssa1 = elem1.getPathFormula().getSsa();
				Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = mgr.mergeSSAMaps(ssa2, ssa1, false);
				MathsatSymbolicFormula old = (MathsatSymbolicFormula)mgr.makeAnd(
						form2, pm.getFirst().getFirst());
				SymbolicFormula newFormula = mgr.makeAnd(form1, pm.getFirst().getSecond());
				newFormula = mgr.makeOr(old, newFormula);
				ssa1 = pm.getSecond();

				// TODO these parameters should be cloned (really?)
				merged.setAbstraction(elem1.getAbstraction());
				merged.setParents(elem1.getParents());
				merged.setPredicates(elem1.getPredicates());
				merged.setPathFormula(new PathFormula(newFormula, ssa1));

				// TODO check, what is that???
				// merged.setMaxIndex(maxIndex)	
				merged.updateMaxIndex(ssa1);
			}
			else{
				// set path formula - it is true
				PathFormula pf = elem1.getPathFormula();
				merged.setPathFormula(pf);
				
				// update initial formula
				// TODO check
				MathsatSymbolicFormula form1 = 
					(MathsatSymbolicFormula)elem1.getInitAbstractionSet().getSymbolicFormula();
				MathsatSymbolicFormula form2 = 
					(MathsatSymbolicFormula)elem2.getInitAbstractionSet().getSymbolicFormula();
				SSAMap ssa2 = elem2.getInitAbstractionSet().getSsa();
				SSAMap ssa1 = elem1.getInitAbstractionSet().getSsa();
				Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = mgr.mergeSSAMaps(ssa2, ssa1, false);
				MathsatSymbolicFormula old = (MathsatSymbolicFormula)mgr.makeAnd(
						form2, pm.getFirst().getFirst());
				SymbolicFormula newFormula = mgr.makeAnd(form1, pm.getFirst().getSecond());
				newFormula = mgr.makeOr(old, newFormula);
				ssa1 = pm.getSecond();
				
				// TODO these parameters should be cloned (really?)
				merged.setParents(elem1.getParents());
				merged.setPredicates(elem1.getPredicates());
				merged.setPathFormula(new PathFormula(newFormula, ssa1));
				
				// TODO compute abstraction here
				merged.setAbstraction(elem1.getAbstraction());
				
				// TODO check, what is that???
				// merged.setMaxIndex(maxIndex)	
				merged.updateMaxIndex(ssa1);
			}

			return merged;
		}
	}
}
