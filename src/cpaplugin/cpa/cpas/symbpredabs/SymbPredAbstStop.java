package cpaplugin.cpa.cpas.symbpredabs;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CustomLogLevel;

public class SymbPredAbstStop implements StopOperator {

    private SymbPredAbstDomain domain;
    
    SymbPredAbstStop(SymbPredAbstDomain domain) {
        this.domain = domain;
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    @Override
    public boolean isBottomElement(AbstractElement element) {
        return element instanceof BottomElement;
    }

    @Override
    public boolean stop(AbstractElement element,
            Collection<AbstractElement> reached) throws CPAException {
        for (AbstractElement e2 : reached) {
            if (stop(element, e2)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stop(AbstractElement element, AbstractElement reachedElement)
            throws CPAException {
        SymbPredAbstElement e = (SymbPredAbstElement)element;
        SymbPredAbstElement e2 = (SymbPredAbstElement)reachedElement;
        SymbPredAbstCPA cpa = domain.getCPA();
        AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
        
        // coverage test: both elements should refer to the same location,
        // both should have only an abstract formula, and the data region 
        // represented by the abstract formula of e should be included
        // in that of e2
        if (e.getLocation().equals(e2.getLocation()) &&
            e.getConcreteFormula().isTrue() && 
            e2.getConcreteFormula().isTrue() &&
            amgr.entails(e.getAbstractFormula(), e2.getAbstractFormula())) {

            LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                           "Element: ", e, " covered by: ", e2);

            return true;
        } else if (e.getLocation().equals(e2.getLocation()) &&
                   e.getCoveredBy() == e2) {
            // TODO Shortcut: basically, when we merge two paths after
            // an if-then-else or a loop, we set the coveredBy of the old one to
            // the new one, so that we can then detect the coverage here. 
            // This has to change to something nicer in the future!!
            LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                           "Element: ", e, " covered by: ", e2);

            return true;
        }

        LazyLogger.log(LazyLogger.DEBUG_1, "Element: ", e, " not covered");

        return false;
    }

}
