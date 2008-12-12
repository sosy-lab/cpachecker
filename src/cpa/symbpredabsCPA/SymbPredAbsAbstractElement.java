package cpa.symbpredabsCPA;

import common.Pair;

import cfa.objectmodel.CFANode;
import symbpredabstraction.ParentsList;
import symbpredabstraction.PathFormula;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.PredicateMap;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.mathsat.BDDAbstractFormula;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormulaManager;

/**
 * AbstractElement for summary cpa
 *
 * @author erkan
 */
public class SymbPredAbsAbstractElement
implements AbstractElement {

	private SymbPredAbsAbstractDomain domain;

	/** Unique state id*/
	private int elementId;
	/** If the element is on an abstraction location */
	private boolean isAbstractionNode = false;
	/** The abstraction location for this node */
	private CFANode abstractionLocation;
	/** the path formula from the abstraction location to this node */
	private PathFormula pathFormula;
	/** initial abstraction values*/
	private PathFormula initAbstractionFormula;
	/** the abstraction which is updated only on abstraction locations */
	private AbstractFormula abstraction;
	/** parents of this element */
	private ParentsList parents;
	/** parent of this element on ART*/
	private SymbPredAbsAbstractElement artParent;
	/** predicate list for this element*/
	private PredicateMap predicates;

	private SSAMap maxIndex;

	public boolean isBottomElement = false;
	private static int nextAvailableId = 1;

	public PathFormula getPathFormula() {
		return pathFormula;
	}

	public void setAbstractionNode(){
		isAbstractionNode = true;
	}

	public boolean isAbstractionNode(){
		return isAbstractionNode;
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

	public ParentsList getParents() {
		return parents;
	}

	public void addParent(Integer i) {
		parents.addToList(i);
	}

	public boolean isDescendant(SymbPredAbsAbstractElement c) {
		SymbPredAbsAbstractElement a = this;
		while (a != null) {
			if (a.equals(c)) return true;
			a = a.getArtParent();
		}
		return false;
	}

	public CFANode getAbstractionLocation(){
		return abstractionLocation;
	}

	public void setAbstractionLocation(CFANode absLoc){
		abstractionLocation = absLoc;
	}

	public SymbPredAbsAbstractElement(AbstractDomain d, boolean isAbstractionElement, CFANode abstLoc,
			PathFormula pf, PathFormula initFormula, AbstractFormula a, 
			ParentsList pl, SymbPredAbsAbstractElement artParent, PredicateMap pmap){
		this.elementId = nextAvailableId++;
		this.domain = (SymbPredAbsAbstractDomain)d;
		this.isAbstractionNode = isAbstractionElement;
		this.abstractionLocation = abstLoc;
		this.pathFormula = pf;
		this.initAbstractionFormula = initFormula;
		this.abstraction = a;
		this.parents = pl;
		this.artParent = artParent;
		this.predicates = pmap;
		this.maxIndex = new SSAMap();
	}


//	// TODO fix these constructors, check all callers later
//// when an element for abstraction and non-abstraction location
//// is created call different constructors
//	private SymbPredAbsAbstractElement(AbstractDomain d, CFANode abstLoc, SymbPredAbsAbstractElement artParent,
//	PathFormula pf, AbstractFormula a,
//	ParentsList p, PathFormula initFormula, PredicateMap pmap) {
//	//CFALocation = CFALoc;
//	abstractionLocation = abstLoc;
//	abstraction = a;
//	pathFormula = pf;
//	parents = p;
//	predicates = pmap;
//	initAbstractionFormula = initFormula;
//	this.artParent = artParent;
//	maxIndex = new SSAMap();
//	domain = (SymbPredAbsAbstractDomain) d;
//	elementId = nextAvailableId++;
//	//bddMathsatMan = d.getCPA().getBDDMathsatSymbPredAbsAbstractManager();
//	//mathsatFormMan = d.getCPA().getMathsatSymbPredAbsFormulaManager();
////	context = null;
////	ownsContext = true;
//	}

//	public SymbPredAbsAbstractElement(AbstractDomain d, CFANode abstLoc, SymbPredAbsAbstractElement artParent) {
//	this(d, abstLoc, artParent, null, null, null, null, null);
//	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		else if(elementId == ((SymbPredAbsAbstractElement)o).elementId){
			return true;
		}

		else{
			SymbPredAbsAbstractElement thisElement = this;
			SymbPredAbsAbstractElement otherElement = (SymbPredAbsAbstractElement)o;

			// TODO
//			if(e1.getLocation().equals(e2.getLocation())){
			// TODO check
			//	boolean b = cpa.isAbstractionLocation(e1.getLocation());
			boolean b = thisElement.isAbstractionNode();
			// if not an abstraction location
			if(!b){
				if(thisElement.getParents().equals(otherElement.getParents())){
					SymbolicFormulaManager mgr = domain.getCPA().getFormulaManager();
					boolean ok = mgr.entails(thisElement.getPathFormula().getSymbolicFormula(),
							otherElement.getPathFormula().getSymbolicFormula()) && 
							mgr.entails(otherElement.getPathFormula().getSymbolicFormula(),
									thisElement.getPathFormula().getSymbolicFormula());
//					// TODO later
////if (ok)
////	{
////					cpa.setCoveredBy(thisElement, otherElement);
////					} else {
////					LazyLogger.log(CustomLogLevel.SpecificCPALevel,
////					"NO, not covered");
////					}
//					return ok;
//					}
//					else{
//					return false;
					return ok;
				}
				return false;
			}
			// if abstraction location
			else{

				// SymbPredAbsCPA cpa = domain.getCPA();

				assert(thisElement.getAbstraction() != null);
				assert(otherElement.getAbstraction() != null);
				if(!thisElement.getParents().equals(otherElement.getParents())){
					return false;
				}
				// TODO check -- we are calling the equals method of the abstract formula
				boolean ok = thisElement.getAbstraction().equals(otherElement.getAbstraction());

				// TODO
//				if (ok) {
//				LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//				"Element: ", element, " COVERED by: ", e2);
//				cpa.setCoveredBy(e1, e2);
//				} else {
//				LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//				"NO, not covered");
//				}
				return ok;
			}
			//}
			// TODO if locations are different
//			else{
//			return false;
//			}
		}
	}

	@Override
	public String toString() {
		BDDAbstractFormula abst = (BDDAbstractFormula)getAbstraction();
		SymbolicFormula symbReprAbst = domain.getCPA().getAbstractFormulaManager().toConcrete(domain.getCPA().getSymbolicFormulaManager(), abst);
		return
		" Abstraction LOCATION: " + getAbstractionLocation() +
		" PF: "+ getPathFormula().getSymbolicFormula() +
		" Abstraction: " + symbReprAbst +
		" Init Formula--> " + (getInitAbstractionSet() != null ? getInitAbstractionSet().getSymbolicFormula() : "null")  +
		" Parents --> " + parents + 
		//" ART Parent --> " + getArtParent() + 
		"\n \n";
		//+ ">(" + Integer.toString(getId()) + ")"
	}

	@Override
	public int hashCode() {
		return elementId;
	}

	public PredicateMap getPredicates() {
		return predicates;
	}

	public void setPredicates(PredicateMap predicates) {
		this.predicates = predicates;
	}

	public void setParents(ParentsList parents2) {
		parents = parents2;
	}

	public PathFormula getInitAbstractionSet() {
		return initAbstractionFormula;
	}

	public void setInitAbstractionSet(PathFormula initFormula) {
		SymbolicFormulaManager mgr = domain.getCPA().getSymbolicFormulaManager();
		SSAMap ssa = new SSAMap();
		SymbolicFormula f = mgr.makeFalse();
		Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mp =
			mgr.mergeSSAMaps(ssa, initFormula.getSsa(), false);
		SymbolicFormula curf = initFormula.getSymbolicFormula();
		// TODO modified if
		if (true) {
			curf = ((MathsatSymbolicFormulaManager)mgr).replaceAssignments((MathsatSymbolicFormula)curf);
		}
		f = mgr.makeAnd(f, mp.getFirst().getFirst());
		curf = mgr.makeAnd(curf, mp.getFirst().getSecond());
		f = mgr.makeOr(f, curf);
		ssa = mp.getSecond();
		initFormula = new PathFormula(f,ssa);
		this.initAbstractionFormula = initFormula;
	}

	public SymbPredAbsAbstractElement getArtParent() {
		return this.artParent;
	}

	public void setArtParent(SymbPredAbsAbstractElement artParent) {
		this.artParent = artParent;
	}

	public void updateMaxIndex(SSAMap ssa) {
		assert(maxIndex != null);
		for (String var : ssa.allVariables()) {
			int i = ssa.getIndex(var);
			int i2 = maxIndex.getIndex(var);
			maxIndex.setIndex(var, Math.max(i, i2));
		}
	}

	public SSAMap getMaxIndex() {
		return maxIndex;
	}

	public void setMaxIndex(SSAMap maxIndex) {
		this.maxIndex = maxIndex;
	}

}
