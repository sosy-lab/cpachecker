package cpa.itpabs;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;
import cpa.itpabs.ItpAbstractElement;
import cpa.itpabs.ItpCPA;
import cpa.symbpredabs.SymbolicFormulaManager;
import exceptions.CPAException;

/**
 * Abstract domain for interpolation-based lazy abstraction
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpAbstractDomain implements AbstractDomain {

    private ItpCPA cpa;

    public ItpAbstractDomain(ItpCPA cpa) {
        this.cpa = cpa;
    }

    private final class ExplicitBottomElement implements BottomElement {
        @Override
    	public String toString() { return "<BOTTOM>"; }
    }
    private final class ExplicitTopElement implements TopElement {}

    private final class ExplicitJoinOperator implements JoinOperator {
        public AbstractElement join(AbstractElement element1,
                AbstractElement element2) throws CPAException {
            throw new CPAException("Can't join summaries!");
        }
    }

    private final class ExplicitPartialOrder implements PartialOrder {
        public boolean satisfiesPartialOrder(AbstractElement element1,
                AbstractElement element2) throws CPAException {
            ItpAbstractElement e1 = (ItpAbstractElement)element1;
            ItpAbstractElement e2 = (ItpAbstractElement)element2;

            assert(e1.getAbstraction() != null);
            assert(e2.getAbstraction() != null);

            if (e1.getLocation().equals(e2.getLocation())) {
                SymbolicFormulaManager mgr = cpa.getFormulaManager();
                return mgr.entails(e1.getAbstraction(), e2.getAbstraction());
            }
            return false;
        }
    }

    private final BottomElement bottom = new ExplicitBottomElement();
    private final TopElement top = new ExplicitTopElement();
    private final JoinOperator join = new ExplicitJoinOperator();
    private final PartialOrder partial = new ExplicitPartialOrder();

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

    public ItpCPA getCPA() {
        return cpa;
    }

}
