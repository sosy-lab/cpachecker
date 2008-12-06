package cpa.itpabs;

import java.util.Collection;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;
import cpa.symbpredabs.SymbolicFormulaManager;
import exceptions.CPAException;

/**
 * Abstract domain for interpolation-based lazy abstraction
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpAbstractDomain implements AbstractDomain {

    private final ItpCPA cpa;

    public ItpAbstractDomain(ItpCPA cpa) {
        this.cpa = cpa;
    }

    private final class ExplicitBottomElement extends ItpAbstractElement implements BottomElement {
      public ExplicitBottomElement() {
        super(null);
        // TODO Auto-generated constructor stub
      }

        @Override
    	public String toString() { return "<BOTTOM>"; }

        @Override
        public Collection<CFANode> getLeaves() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public boolean isErrorLocation() {
          // TODO Auto-generated method stub
          return false;
        }
    }
    private final class ExplicitTopElement extends ItpAbstractElement implements TopElement {

      public ExplicitTopElement() {
        super(null);
        // TODO Auto-generated constructor stub
      }

      @Override
      public Collection<CFANode> getLeaves() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean isErrorLocation() {
        // TODO Auto-generated method stub
        return false;
      }

    }

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

    private final ExplicitBottomElement bottom = new ExplicitBottomElement();
    private final ExplicitTopElement top = new ExplicitTopElement();
    private final JoinOperator join = new ExplicitJoinOperator();
    private final PartialOrder partial = new ExplicitPartialOrder();

    public ItpAbstractElement getBottomElement() {
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

    public ItpAbstractElement getTopElement() {
        return top;
    }

    public ItpCPA getCPA() {
        return cpa;
    }

}
