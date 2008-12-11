package cpa.symbpredabsCPA;

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

/**
 * AbstractElement for summary cpa
 *
 * @author erkan
 */
public class SymbPredAbsAbstractElement
implements AbstractElement {
  
  private SymbPredAbsAbstractDomain domain;

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

  // TODO check this
  SSAMap maxIndex;

  // private SymbPredAbsAbstractDomain domain;
  // private BDDMathsatSymbPredAbsAbstractManager bddMathsatMan;
  // private MathsatSymbPredAbsFormulaManager mathsatFormMan;

  //private static int nextAvailableId = 1;

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

  public CFANode getAbstractionLocation(){
    return abstractionLocation;
  }

  public void setAbstractionLocation(CFANode absLoc){
    abstractionLocation = absLoc;
  }

  // TODO fix these constructors, check all callers later
  // when an element for abstraction and non-abstraction location
  // is created call different constructors
  public SymbPredAbsAbstractElement(AbstractDomain d, CFANode abstLoc,
                                    PathFormula pf, AbstractFormula a,
                                    ParentsList p, PathFormula initFormula, PredicateMap pmap) {
    //CFALocation = CFALoc;
    abstractionLocation = abstLoc;
    abstraction = a;
    pathFormula = pf;
    parents = p;
    predicates = pmap;
    initAbstractionFormula = initFormula;
    maxIndex = new SSAMap();
    domain = (SymbPredAbsAbstractDomain) d;
    //bddMathsatMan = d.getCPA().getBDDMathsatSymbPredAbsAbstractManager();
    //mathsatFormMan = d.getCPA().getMathsatSymbPredAbsFormulaManager();
//  context = null;
//  ownsContext = true;
  }

  public SymbPredAbsAbstractElement(AbstractDomain d, CFANode abstLoc) {
    this(d, abstLoc, null, null, null, null, null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    else{
      SymbPredAbsAbstractElement thisElement = this;
      SymbPredAbsAbstractElement otherElement = (SymbPredAbsAbstractElement)o;

      // TODO
//    if(e1.getLocation().equals(e2.getLocation())){
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
//        // TODO later
////      if (ok)
////      {
////      cpa.setCoveredBy(thisElement, otherElement);
////      } else {
////      LazyLogger.log(CustomLogLevel.SpecificCPALevel,
////      "NO, not covered");
////      }
//        return ok;
//        }
//        else{
//        return false;
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
//      if (ok) {
//LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//      "Element: ", element, " COVERED by: ", e2);
//      cpa.setCoveredBy(e1, e2);
//      } else {
//      LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//      "NO, not covered");
//      }
        return ok;
      }
      //}
      // TODO if locations are different
//    else{
//    return false;
//    }



    }
  }

  //public int hashCode() {
  //return elemId;
  //}

  public void updateMaxIndex(SSAMap ssa) {
    assert(maxIndex != null);
    for (String var : ssa.allVariables()) {
      int i = ssa.getIndex(var);
      int i2 = maxIndex.getIndex(var);
      maxIndex.setIndex(var, Math.max(i, i2));
    }
  }

  @Override
  public String toString() {
    BDDAbstractFormula abst = (BDDAbstractFormula)getAbstraction();
    SymbolicFormula symbReprAbst = domain.getCPA().getAbstractFormulaManager().toConcrete(domain.getCPA().getSymbolicFormulaManager(), abst);
    return
    " PF: "+ getPathFormula().getSymbolicFormula() +
    " Abstraction: " + symbReprAbst +
    " Init Formula--> " + (getInitAbstractionSet() != null ? getInitAbstractionSet().getSymbolicFormula() : "null")  +
    " Parents --> " + parents + 
    "\n \n";
    //+ ">(" + Integer.toString(getId()) + ")"
  }

  public PredicateMap getPredicates() {
    return predicates;
  }

  public void setPredicates(PredicateMap predicates) {
    this.predicates = predicates;
  }

  // TODO enable this later
//public boolean isDescendant(SymbPredAbsAbstractElement c) {
//SymbPredAbsAbstractElement a = this;
//while (a != null) {
//if (a.equals(c)) return true;
//a = a.getParent();
//}
//return false;
//}

  public void setParents(ParentsList parents2) {
    parents = parents2;
  }

  public PathFormula getInitAbstractionSet() {
    return initAbstractionFormula;
  }

  public void setInitAbstractionSet(PathFormula initFormula) {
    this.initAbstractionFormula = initFormula;
  }

  public SSAMap getMaxIndex() {
    return maxIndex;
  }

  public void setMaxIndex(SSAMap maxIndex) {
    this.maxIndex = maxIndex;
  }

  public SymbPredAbsAbstractElement getArtParent() {
    return this.artParent;
  }

  public void setArtParent(SymbPredAbsAbstractElement artParent) {
    this.artParent = artParent;
  }
}
