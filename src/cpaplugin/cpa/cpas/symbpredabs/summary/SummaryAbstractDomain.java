package cpaplugin.cpa.cpas.symbpredabs.summary;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.TopElement;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormulaManager;
import cpaplugin.exceptions.CPAException;

public class SummaryAbstractDomain implements AbstractDomain {
    
    private SummaryCPA cpa;
    
    public SummaryAbstractDomain(SummaryCPA cpa) {
        this.cpa = cpa;
    }
    
    private final class SummaryBottomElement implements BottomElement {
        public String toString() { return "<BOTTOM>"; }
    }
    private final class SummaryTopElement implements TopElement {}
    
    private final class SummaryJoinOperator implements JoinOperator {
        @Override
        public AbstractElement join(AbstractElement element1,
                AbstractElement element2) throws CPAException {
            throw new CPAException("Can't join summaries!");
        }
    }
    
    private final class SummaryPreOrder implements PreOrder {
        @Override
        public boolean satisfiesPreOrder(AbstractElement element1,
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
    private final PreOrder pre = new SummaryPreOrder();

    @Override
    public BottomElement getBottomElement() {
        return bottom;
    }

    @Override
    public JoinOperator getJoinOperator() {
        return join;
    }

    @Override
    public PreOrder getPreOrder() {
        return pre;
    }

    @Override
    public TopElement getTopElement() {
        return top;
    }

    public SummaryCPA getCPA() {
        return cpa;
    }

}
