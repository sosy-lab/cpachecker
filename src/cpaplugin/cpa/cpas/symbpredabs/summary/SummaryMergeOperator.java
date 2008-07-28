package cpaplugin.cpa.cpas.symbpredabs.summary;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

public class SummaryMergeOperator implements MergeOperator {

    private SummaryAbstractDomain domain;
    
    public SummaryMergeOperator(SummaryAbstractDomain d) {
        domain = d;
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    @Override
    public AbstractElement merge(AbstractElement element1,
                                 AbstractElement element2) {
        CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                "Trying to merge elements: " + element1.toString() + 
                " and: " + element2.toString());
        
        return element2;
    }

}
