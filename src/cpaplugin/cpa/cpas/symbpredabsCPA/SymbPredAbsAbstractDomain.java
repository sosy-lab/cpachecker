package cpaplugin.cpa.cpas.symbpredabsCPA;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.TopElement;
import cpaplugin.exceptions.CPAException;
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

	private final class SymbPredAbsPreOrder implements PreOrder {
		public boolean satisfiesPreOrder(AbstractElement element1,
				AbstractElement element2) throws CPAException {
			SymbPredAbsAbstractElement e1 = (SymbPredAbsAbstractElement)element1;
			SymbPredAbsAbstractElement e2 = (SymbPredAbsAbstractElement)element2;

			assert(e1.getAbstraction() != null);
			assert(e2.getAbstraction() != null);

			if (e1.getLocation().equals(e2.getLocation())) {
				AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
				return amgr.entails(e1.getAbstraction(), e2.getAbstraction());
			}
			return false;
		}
	}

	private final BottomElement bottom = new SymbPredAbsBottomElement();
	private final TopElement top = new SymbPredAbsTopElement();
	private final JoinOperator join = new SymbPredAbsJoinOperator();
	private final PreOrder pre = new SymbPredAbsPreOrder();

	public BottomElement getBottomElement() {
		return bottom;
	}

	public JoinOperator getJoinOperator() {
		return join;
	}

	public PreOrder getPreOrder() {
		return pre;
	}

	public TopElement getTopElement() {
		return top;
	}

	public SymbPredAbsCPA getCPA() {
		return cpa;
	}
}
