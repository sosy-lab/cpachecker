package cpaplugin.cpa.cpas.symbpredabsCPA;

import symbpredabstraction.PathFormula;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;
import symbpredabstraction.*;

/**
 * AbstractElement for symbolic lazy abstraction with summaries
 *
 * @author erkan
 */
public class SymbPredAbsAbstractElement 
implements AbstractElement, AbstractElementWithLocation {

	/** The location on CFA */
	private CFANode CFALocation;
	/** The abstraction location for this node */
	private CFANode abstractionLocation;
	/** the path formula from the abstraction location to this node */
	private PathFormula pathFormula;
	/** the abstraction which is updated only on abstraction locations */
	private AbstractFormula abstraction;
	/** parent of this element on ART */
	private SymbPredAbsAbstractElement parent;
	/** predicate list for this element*/
    private PredicateMap predicates;
	
	// context is used to deal with function calls/returns
//	private Stack<Pair<AbstractFormula, SymbPredAbsCFANode>> context;
//	private boolean ownsContext;

//	private static int nextAvailableId = 1;

//	public int getId() { return elemId; }
	public CFANode getLocation() { 
		return CFALocation; 
	}

	public PathFormula getPathFormula() { 
		return pathFormula;
	}
	
	public void setLocation(CFANode loc){
		CFALocation = loc;
	}

	public AbstractFormula getAbstraction() { 
		return abstraction; 
	}

	public void setAbstraction(AbstractFormula a) { 
		abstraction = a;
	}
	public void setPathFormula(PathFormula pf){
		pathFormula = pf;
	}

	public SymbPredAbsAbstractElement getParent() { 
		return parent; 
	}
	
	public void setParent(SymbPredAbsAbstractElement p) { 
		parent = p;
	}
	
	public CFANode getAbstractionLocation(){
		return abstractionLocation;
	}
	
	public void setAbstractionLocation(CFANode absLoc){
		abstractionLocation = absLoc;
	}

	public SymbPredAbsAbstractElement(CFANode CFALoc, CFANode abstLoc, 
			PathFormula pf, AbstractFormula a, 
			SymbPredAbsAbstractElement p, PredicateMap pmap) {
		CFALocation = CFALoc;
		abstractionLocation = abstLoc;
		abstraction = a;
		pathFormula = pf;
		parent = p;
		predicates = pmap;
//		context = null;
//		ownsContext = true;
	}

	public SymbPredAbsAbstractElement(CFANode loc, CFANode abstLoc) {
		this(loc, abstLoc, null, null, null, null);
	}
	
	public SymbPredAbsAbstractElement() {
		this(null, null, null, null, null, null);
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

	public PredicateMap getPredicates() {
		return predicates;
	}

	public void setPredicates(PredicateMap predicates) {
		this.predicates = predicates;
	}

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
