package cpa.symbpredabs.summary;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;
import cpa.symbpredabs.AbstractFormulaManager;
import exceptions.CPAException;


/** 
 * Abstract domain for Symbolic lazy abstraction with summaries.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */ 
public class SummaryAbstractDomain implements AbstractDomain {
    
    private SummaryCPA cpa;
    
    public SummaryAbstractDomain(SummaryCPA cpa) {
        this.cpa = cpa;
    }
    
    private final class SummaryBottomElement implements BottomElement {
        @Override
    	public String toString() { return "<BOTTOM>"; }
    }
    private final class SummaryTopElement implements TopElement {}
    
    private final class SummaryJoinOperator implements JoinOperator {
        public AbstractElement join(AbstractElement element1,
                AbstractElement element2) throws CPAException {
            throw new CPAException("Can't join summaries!");
        }
    }
    
    private final class SummaryPartialOrder implements PartialOrder {
        public boolean satisfiesPartialOrder(AbstractElement element1,
                AbstractElement element2) throws CPAException {
            SummaryAbstractElement e1 = (SummaryAbstractElement)element1;
            SummaryAbstractElement e2 = (SummaryAbstractElement)element2;

            assert(e1.getAbstraction() != null);
            assert(e2.getAbstraction() != null);
            
            if (e1.getLocation().equals(e2.getLocation())) {
                AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
                return amgr.entails(e1.getAbstraction(), e2.getAbstraction());
            }
            return false;
        }
    }
    
    private final BottomElement bottom = new SummaryBottomElement();
    private final TopElement top = new SummaryTopElement();
    private final JoinOperator join = new SummaryJoinOperator();
    private final PartialOrder partial = new SummaryPartialOrder();

    public BottomElement getBottomElement() {
        return bottom;
    }
    
    public boolean isBottomElement(AbstractElement element) {
        return element == bottom;
    }

    public JoinOperator getJoinOperator() {
        return join;
    }

    public PartialOrder getPartialOrder() {
        return partial;
    }

    public TopElement getTopElement() {
        return top;
    }

    public SummaryCPA getCPA() {
        return cpa;
    }

}
