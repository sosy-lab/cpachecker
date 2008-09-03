package cpaplugin.cpa.cpas.symbpredabs;

import java.util.Collection;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;

/**
 * TODO. This is currently broken
 */
public class SymbPredAbstMerge implements MergeOperator {

    private SymbPredAbstDomain domain;
    
    public SymbPredAbstMerge(SymbPredAbstDomain domain) {
        this.domain = domain;
    }
    
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    public AbstractElement merge(AbstractElement element1,
                                 AbstractElement element2) {
        
        if (element1 instanceof BottomElement) {
            return element2;
        } else if (element2 instanceof BottomElement) {
            return element2;
        }
        
        SymbPredAbstElement e1 = (SymbPredAbstElement)element1;
        SymbPredAbstElement e2 = (SymbPredAbstElement)element2;

        
        // we can merge two states if their location is the same
        if (e1.getLocation().equals(e2.getLocation())) {
            LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                           "Merging elements: ", e1, " and: ", e2);
            // how the merge is performed depends on the kind of location. 
            // if we are at a loop start, then we have to compute the 
            // abstraction of the combined concrete formula. Otherwise, we
            // do only a "syntactic" merge, without taking the abstraction into
            // account
            CFANode n = e1.getLocation();
            SymbolicFormula f1 = e1.getFormula();
            SymbolicFormula f2 = e2.getFormula();
            
            SymbPredAbstCPA cpa = domain.getCPA();
            Collection<Predicate> predicates = 
                cpa.getPredicateMap().getRelevantPredicates(n);
            SymbolicFormulaManager mgr = cpa.getFormulaManager();
            AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
            
            Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> merger = 
                mgr.mergeSSAMaps(e1.getSSAMap(), e2.getSSAMap(), true);
            Pair<SymbolicFormula, SymbolicFormula> mf = merger.getFirst();
            SSAMap newssa = merger.getSecond();
            
            SymbPredAbstElement ret = null;
            if (n.isLoopStart()) {
                ret = new SymbPredAbstElement(n, mgr.makeTrue(), 
                        amgr.toAbstract(
                                mgr, 
                                mgr.makeOr(mgr.makeAnd(f1, mf.getFirst()),
                                           mgr.makeAnd(f2, mf.getSecond())),
                                newssa, predicates), e2.getParent(), newssa);
                if (e2.getConcreteFormula().isTrue()) {
                    LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                                   "CHECKING coverage of 'ret' by 'e2'");
                    if (amgr.entails(ret.getAbstractFormula(), 
                                     e2.getAbstractFormula())) {
                        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                                       "YES, COVERED");
                        ret = e2;
                    } else {
                        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                                       "NO, not covered");
                    }
                }                        
            } else {
                ret = new SymbPredAbstElement(
                        n, 
                        mgr.makeOr(mgr.makeAnd(f1, mf.getFirst()),
                                   mgr.makeAnd(f2, mf.getSecond())),
                        null, e2.getParent(), newssa);
            }
            // TODO - Shortcut, we set the coveredBy of e1 to be the new created
            // element, so that the stop operator detects that e1 is covered
            // by ret (see SymbPredAbstStop)
            e1.setCoveredBy((SymbPredAbstElement)ret);
            
            LazyLogger.log(CustomLogLevel.SpecificCPALevel, "result is: ", ret);

            return ret;
        }
        
        return e2;
    }
}
