package cpaplugin.cpa.cpas.symbpredabsCPA;

import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpaplugin.logging.LazyLogger;

/**
 * trivial merge operation for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsMergeOperator implements MergeOperator {

    private SymbPredAbsAbstractDomain domain;
    
    public SymbPredAbsMergeOperator(SymbPredAbsAbstractDomain d) {
        domain = d;
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
    	// TODO later
    	if(loc1.getNodeNumber() != loc2.getNodeNumber()){
    		return element2;
    	}
    	// if elemen1's location is not abstraction location
    	if(!isAbstractionLocation(loc1)){
    		// if elements cannot be merged
    		if(loc1.getNodeNumber() != loc2.getNodeNumber() || 
   			   !elem1.getParents().equals(elem2.getParents())){
    			return element2;		
    		}
    		else{
    			MathsatSymbolicFormula old = 
                    nodeToFormula.get(succ);
                SSAMap oldssa = nodeToSSA.get(succ);
                Pair<Pair<SymbolicFormula, SymbolicFormula>, 
                     SSAMap> pm = mergeSSAMaps(oldssa, ssa1, false);
                old = (MathsatSymbolicFormula)makeAnd(
                        old, pm.getFirst().getFirst());
                t1 = makeAnd(t1, pm.getFirst().getSecond());
                t1 = makeOr(old, t1);
                ssa1 = pm.getSecond();
    		}
    	}
    	else {
    		// TODO later
    		return element2;
    	}
    	
        return element2;
    }
    
	private boolean isAbstractionLocation(CFANode succLoc) {
		if (succLoc.isLoopStart() || succLoc instanceof CFAErrorNode
				|| succLoc.getNumLeavingEdges() == 0) {
			return true;
		} else if (succLoc instanceof CFAFunctionDefinitionNode) {
			return true;
		} else if (succLoc.getEnteringSummaryEdge() != null) {
			return true;
			// if a node has two or more incoming edges from different
			// summary nodes, it is a abstraction location
		} else {
			// TODO implement
//			CFANode cur = null;
//			Map<CFANode, AbstractionLocationPointer> abstLocsMap = domain
//			.getCPA().getAbstracionLocsMap();
//			for (int i = 0; i < succLoc.getNumEnteringEdges(); ++i) {
//				CFAEdge e = succLoc.getEnteringEdge(i);
//				if (!isLoopBack(e)) {
//					CFANode p = e.getPredecessor();
//					if (!abstLocsMap.containsKey(p)) {
//						// this might happen if this e is a jump edge: in this
//						// case, we ignore it...
//						assert (e instanceof BlankEdge);
//						continue;
//					}
//					assert (abstLocsMap.containsKey(p));
//					AbstractionLocationPointer abp = abstLocsMap.get(p);
//					CFANode summ = abp.getAbstractionLocation();
//					if (cur == null) {
//						cur = summ;
//					} else if (cur != summ) {
//						return true;
//					}
//				}
//			}
//			// check if we have only blank incoming edges, and the current
//			// summary is already big TODO
//			if (CPAMain.cpaConfig
//					.getBooleanValue("cpas.symbpredabs.smallSummaries")) {
//				if (succLoc.getNumEnteringEdges() >= 1) {
//					for (int i = 0; i < succLoc.getNumEnteringEdges(); ++i) {
//						CFAEdge e = succLoc.getEnteringEdge(i);
//						if (!(e instanceof BlankEdge))
//							break;
//						if (e instanceof BlankEdge
//								&& e.getRawStatement().startsWith(
//								"Goto: BREAK_SUMMARY")) {
//							return true;
//						}
//					}
//				}
//			}
//			// int summarySize = 0;
//			// if (cur != null && summarySizeMap.containsKey(cur)) {
//			// summarySize = summarySizeMap.get(cur);
//			// }
//			// final int MAX_SUMMARY_SIZE = 5;
//			// if (summarySize > MAX_SUMMARY_SIZE) {
//			// boolean allIncomingBlank = true;
//			// for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
//			// CFAEdge e = n.getEnteringEdge(i);
//			// if (!isLoopBack(e) && !(e instanceof BlankEdge)) {
//			// allIncomingBlank = false;
//			// break;
//			// }
//			// }
//			// if (allIncomingBlank) return true;
//			// }
			return false;
		}
	}

}
