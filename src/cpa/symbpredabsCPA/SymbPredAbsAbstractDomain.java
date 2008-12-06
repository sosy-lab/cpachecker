package cpa.symbpredabsCPA;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;
import exceptions.CPAException;
import symbpredabstraction.*;


/**
 * Abstract domain for Symbolic lazy abstraction with summaries.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsAbstractDomain implements AbstractDomain {

	private SymbPredAbsCPA cpa;

	public SymbPredAbsAbstractDomain(SymbPredAbsCPA cpa) {
		this.cpa = cpa;
	}

	private final class SymbPredAbsBottomElement implements BottomElement {
		@Override
		public String toString() {
			return "<BOTTOM>";
		}
	}
	private final class SymbPredAbsTopElement implements TopElement {}

	private final class SymbPredAbsJoinOperator implements JoinOperator {
		public AbstractElement join(AbstractElement element1,
				AbstractElement element2) throws CPAException {
			throw new CPAException("Can't join summaries!");
		}
	}

	private final class SymbPredAbsPartialOrder implements PartialOrder {
		public boolean satisfiesPartialOrder(AbstractElement element1,
				AbstractElement element2) throws CPAException {
			SymbPredAbsAbstractElement e1 = (SymbPredAbsAbstractElement)element1;
			SymbPredAbsAbstractElement e2 = (SymbPredAbsAbstractElement)element2;

			assert(e1.getAbstraction() != null);
			assert(e2.getAbstraction() != null);

			// TODO check later
			//if (e1.getLocation().equals(e2.getLocation())) {
				AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
				return amgr.entails(e1.getAbstraction(), e2.getAbstraction());
			//}
			// return false;
		}
	}

	private final BottomElement bottom = new SymbPredAbsBottomElement();
	private final TopElement top = new SymbPredAbsTopElement();
	private final JoinOperator join = new SymbPredAbsJoinOperator();
	private final PartialOrder partial = new SymbPredAbsPartialOrder();

	public AbstractElement getBottomElement() {
		return bottom;
	}

    public boolean isBottomElement(AbstractElement element) {
    	SymbPredAbsAbstractElement symbPredAbsElem = (SymbPredAbsAbstractElement) element;

//		if(predAbsElem == (domain.getBottomElement())){
//			System.out.println("==========================");
//			return true;
//		}
    	// TODO if the element is the bottom element
    	if (getCPA().getBDDMathsatSymbPredAbsAbstractManager().isFalse(symbPredAbsElem.getAbstraction())){
    		return true;
    	}

		return false;
    }

	public JoinOperator getJoinOperator() {
		return join;
	}

	public PartialOrder getPartialOrder() {
		return partial;
	}

	public AbstractElement getTopElement() {
		return top;
	}

	public SymbPredAbsCPA getCPA() {
		return cpa;
	}
}
