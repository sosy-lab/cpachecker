package cpaplugin.cpa.cpas.itpabs;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;
import cpaplugin.exceptions.CPAException;

/**
 * Coverage check for interpolation-based lazy abstraction
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpStopOperator implements StopOperator {

    private ItpAbstractDomain domain;
    
    // statistics
    public long coverageCheckTime;
    public int numCoveredStates;
    public int numCoverageChecks;
    public ItpStopOperator(ItpAbstractDomain d) {
        domain = d;
        coverageCheckTime = 0;
        numCoveredStates = 0;
        numCoverageChecks = 0;
    }

    
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    
    public boolean isBottomElement(AbstractElement element) {
        return element == domain.getBottomElement();
    }

    
    public boolean stop(AbstractElement element,
            Collection<AbstractElement> reached) throws CPAException {
        ItpCPA cpa = domain.getCPA();
        for (AbstractElement e : reached) {
            ItpAbstractElement ie = (ItpAbstractElement)element;
            if (cpa.isCovered(ie)) {
                return true;
            }
            if (stop(element, e)) {
                return true;
            }
        }
        return false;
    }

    public boolean stop(AbstractElement element, AbstractElement reachedElement)
        throws CPAException {
        long start = System.currentTimeMillis();
        boolean res = stopPriv(element, reachedElement);
        long end = System.currentTimeMillis();
        coverageCheckTime += end - start;
        return res;
    }
    
    public boolean stopPriv(AbstractElement element, 
                            AbstractElement reachedElement)
            throws CPAException {

        ItpAbstractElement e1 = (ItpAbstractElement)element;
        ItpAbstractElement e2 =
            (ItpAbstractElement)reachedElement;
        
        if (e1.getLocation().equals(e2.getLocation()) &&
                e2.getId() < e1.getId()) {
            LazyLogger.log(LazyLogger.DEBUG_1, 
                    "Checking Coverage of element: ", element);
            
            if (!e1.sameContext(e2)) {
                LazyLogger.log(LazyLogger.DEBUG_1,
                               "NO, not covered: context differs");
                return false;
            }
            
            assert(e1.getAbstraction() != null);
            assert(e2.getAbstraction() != null);

            ItpCPA cpa = domain.getCPA();
            
            ++numCoverageChecks;
            SymbolicFormulaManager mgr = cpa.getFormulaManager();
            boolean ok = mgr.entails(e1.getAbstraction(), e2.getAbstraction());
            
            if (ok) {
                ++numCoveredStates;
                LazyLogger.log(LazyLogger.DEBUG_1,
                               "Element: ", e1, " COVERED by: ", e2);
                LazyLogger.log(LazyLogger.DEBUG_1,
                        "Abstraction for ", e1, ": ", e1.getAbstraction());
                LazyLogger.log(LazyLogger.DEBUG_1,
                        "Abstraction for ", e2, ": ", e2.getAbstraction());
                cpa.setCoveredBy(e1, e2);
                ItpTransferRelation trans = 
                    (ItpTransferRelation)cpa.getTransferRelation();
                trans.addToProcess(cpa.removeDescendantsFromCovering(e1));
            } else {
                LazyLogger.log(LazyLogger.DEBUG_1,
                               "NO, not covered");
            }

            return ok;
        } else {
            return false;
        }
    }

}
