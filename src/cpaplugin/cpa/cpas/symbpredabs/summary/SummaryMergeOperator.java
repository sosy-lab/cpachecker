package cpaplugin.cpa.cpas.symbpredabs.summary;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.logging.LazyLogger;

/**
 * trivial merge operation for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryMergeOperator implements MergeOperator {

    private SummaryAbstractDomain domain;
    
    public SummaryMergeOperator(SummaryAbstractDomain d) {
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
