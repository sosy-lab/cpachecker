package cpaplugin.cpa.cpas.symbpredabsCPA;

import java.util.Collection;
import java.util.Map;
import java.util.Stack;

import symbpredabstraction.SymbPredAbsCFANode;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;

/**
 * AbstractElement for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsAbstractElement 
implements AbstractElement, AbstractElementWithLocation {

//	private int elemId;
	private CFANode CFALocation;
	// for each "leaf" node in the inner CFA of this summary, we keep the 
	// symbolic representation of all the paths leading to the leaf
	private Pair<SymbolicFormula, SSAMap> pathFormula;
	private AbstractFormula abstraction;
	private SymbPredAbsAbstractElement parent;

	// context is used to deal with function calls/returns
//	private Stack<Pair<AbstractFormula, SymbPredAbsCFANode>> context;
//	private boolean ownsContext;

//	private static int nextAvailableId = 1;

//	public int getId() { return elemId; }
	public CFANode getLocation() { 
		return CFALocation; 
	}

	public Pair<SymbolicFormula, SSAMap> getPathFormula() { 
		return pathFormula;
	}

	public AbstractFormula getAbstraction() { 
		return abstraction; 
	}

	public void setAbstraction(AbstractFormula a) { 
		abstraction = a;
	}
	public void setPathFormula(Pair<SymbolicFormula, SSAMap> pf){
		pathFormula = pf;
	}

	public SymbPredAbsAbstractElement getParent() { 
		return parent; 
	}
	
	public void setParent(SymbPredAbsAbstractElement p) { 
		parent = p;
	}

	private SymbPredAbsAbstractElement(CFANode CFALoc, AbstractFormula a, 
			Pair<SymbolicFormula, SSAMap> pf, SymbPredAbsAbstractElement p) {
//		elemId = nextAvailableId++;
		
		CFALocation = CFALoc;
		abstraction = a;
		pathFormula = pf;
		parent = p;
//		context = null;
//		ownsContext = true;
	}

	public SymbPredAbsAbstractElement(CFANode loc) {
		this(loc, null, null, null);
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof SymbPredAbsAbstractElement)) {
			return false;
		} 
//		else {
//			return elemId == ((SymbPredAbsAbstractElement)o).elemId;
//		}
		// TODO fix this
		return false;
	}

//	public int hashCode() {
//	return elemId;
//	}

	public String toString() {
		return "SE<" + Integer.toString(
				CFALocation.getNodeNumber()) 
				//+ ">(" + Integer.toString(getId()) + ")"
				; 
	}

	public CFANode getLocationNode() {
		return CFALocation;
	}

//	public Collection<CFANode> getLeaves() {
//		assert(pathFormulas != null);
//
//		return pathFormulas.keySet();
//	}

	/**
	public Stack<Pair<AbstractFormula, SymbPredAbsCFANode>> getContext() 
	{ 
		return context; 
	}

	public void setContext(Stack<Pair<AbstractFormula, SymbPredAbsCFANode>> ctx, 
			boolean owns) 
	{ 
		context = ctx;
		ownsContext = owns;
	}

	public AbstractFormula topContextAbstraction() {
		assert(context != null);
		assert(!context.empty());
		return context.peek().getFirst();
	}

	public SymbPredAbsCFANode topContextLocation() {
		assert(context != null);
		assert(!context.empty());
		return context.peek().getSecond();
	}

	private void cloneContext() {
		// copy-on-write semantics: just duplicate the context and push
		// in the copy
		Stack<Pair<AbstractFormula, SymbPredAbsCFANode>> copy = 
			new Stack<Pair<AbstractFormula, SymbPredAbsCFANode>>();
		for (Pair<AbstractFormula, SymbPredAbsCFANode> a : context) {
			copy.add(a);
		}
		context = copy;
		ownsContext = true;
	}

	public void pushContext(AbstractFormula af, SymbPredAbsCFANode returnLoc) {
		if (!ownsContext) {
			cloneContext();
		}
		context.push(new Pair<AbstractFormula, SymbPredAbsCFANode>(af, returnLoc));
	}

	public void popContext() {
		if (!ownsContext) {
			cloneContext();
		}
		context.pop();
	}

	public boolean sameContext(SymbPredAbsAbstractElement e2) {
		assert(context != null && e2.context != null);

		if (context == e2.context) {
			return true;
		} else if (context.size() != e2.context.size()) {
			return false;
		} else {
			for (int i = 0; i < context.size(); ++i) {
				if (!context.elementAt(i).equals(e2.context.elementAt(i))) {
					return false;
				}
			}
		}
		return true;
	}
**/
	public boolean isDescendant(SymbPredAbsAbstractElement c) {
		SymbPredAbsAbstractElement a = this;
		while (a != null) {
			if (a.equals(c)) return true;
			a = a.getParent();
		}
		return false;
	}

}
