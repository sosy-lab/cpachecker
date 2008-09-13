package cpaplugin.cpa.cpas.symbpredabsCPA;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;
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
        LazyLogger.log(LazyLogger.DEBUG_4, 
                "Trying to merge elements: ", element1, 
                " and: ", element2);
        
        return element2;
    }

}
