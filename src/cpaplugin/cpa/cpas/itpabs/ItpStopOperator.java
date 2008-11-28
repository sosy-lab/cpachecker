package cpaplugin.cpa.cpas.itpabs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.TheoremProver;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.LazyLogger;

/**
 * Coverage check for interpolation-based lazy abstraction
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpStopOperator implements StopOperator {

    private ItpAbstractDomain domain;
    private TheoremProver thmProver;
    // cache for checking entailement. Can be disabled
    private boolean entailsUseCache;
    private Map<Pair<SymbolicFormula, SymbolicFormula>, Boolean> entailsCache;
    
    // statistics
    public long coverageCheckTime;
    public int numCoveredStates;
    public int numCoverageChecks;
    public int numCachedCoverageChecks;
    
    public ItpStopOperator(ItpAbstractDomain d, TheoremProver prover) {
        domain = d;
        coverageCheckTime = 0;
        numCoveredStates = 0;
        numCoverageChecks = 0;
        numCachedCoverageChecks = 0;
        thmProver = prover;
        
        entailsUseCache = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useCache");
        if (entailsUseCache) {
            entailsCache = 
                new HashMap<Pair<SymbolicFormula, SymbolicFormula>, Boolean>();
        }
    }

    
    public AbstractDomain getAbstractDomain() {
        return domain;
    }


    public boolean stop(AbstractElement element,
            Collection<AbstractElement> reached) throws CPAException {
        ItpCPA cpa = domain.getCPA();
        ItpAbstractElement ie = (ItpAbstractElement)element;
        if (cpa.isCovered(ie)) {
            return true;
        }
        for (AbstractElement e : reached) {
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
            int res = -1;
            boolean ok;
            
            Pair<SymbolicFormula, SymbolicFormula> key = null;            
            if (entailsUseCache) {
                key = new Pair<SymbolicFormula, SymbolicFormula>(
                        e1.getAbstraction(), e2.getAbstraction());
                if (entailsCache.containsKey(key)) {
                    res = entailsCache.get(key) ? 1 : 0;
                    ++numCachedCoverageChecks;
                }
            }
            if (res != -1) {
                ok = (res == 1);
            } else {            
                ok = mgr.entails(e1.getAbstraction(), e2.getAbstraction(),
                                 thmProver);
                if (entailsUseCache) {
                    assert(key != null);
                    entailsCache.put(key, ok);
                }
            }
            
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
