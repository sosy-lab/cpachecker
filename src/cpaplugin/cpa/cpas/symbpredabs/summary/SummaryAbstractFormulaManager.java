package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.util.Collection;
import java.util.Deque;

import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.CounterexampleTraceInfo;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;

public interface SummaryAbstractFormulaManager extends AbstractFormulaManager {

    public AbstractFormula buildAbstraction(SummaryFormulaManager mgr,
            SummaryAbstractElement e, SummaryAbstractElement succ, 
            Collection<Predicate> predicates);

    public CounterexampleTraceInfo buildCounterexampleTrace(
            SummaryFormulaManager mgr, 
            Deque<SummaryAbstractElement> abstractTrace);
}
