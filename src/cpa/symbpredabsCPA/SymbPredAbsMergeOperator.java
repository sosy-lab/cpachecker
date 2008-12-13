package cpa.symbpredabsCPA;

import symbpredabstraction.PathFormula;

import common.Pair;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormula;
import exceptions.CPAException;

/**
 * trivial merge operation for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsMergeOperator implements MergeOperator {

  private final SymbPredAbsAbstractDomain domain;

  private SymbolicFormulaManager mgr;

  public SymbPredAbsMergeOperator(SymbPredAbsAbstractDomain d) {
    domain = d;
    mgr = d.getCPA().getSymbolicFormulaManager();
  }


  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2) {

    SymbPredAbsAbstractElement elem1 = (SymbPredAbsAbstractElement)element1;
    SymbPredAbsAbstractElement elem2 = (SymbPredAbsAbstractElement)element2;

    // TODO removed loc information
//  CFANode loc1 = elem1.getLocation();
//  CFANode loc2 = elem2.getLocation();

//  if(loc1.getNodeNumber() != loc2.getNodeNumber() ||
//  !(elem1.getParents().equals(elem2.getParents()))){
//  return element2;
//  }
//  else{
    //TODO check
    boolean b = elem1.isAbstractionNode();
    SymbPredAbsAbstractElement merged;
    if(!b){
      if(!elem1.getParents().equals(elem2.getParents())){
        merged = elem2;
      }
      else{
     // we set parent to abstract element 2's parent
        merged = new SymbPredAbsAbstractElement(domain, false, elem1.getAbstractionLocation(), 
            null, elem1.getInitAbstractionSet(), elem1.getAbstraction(), 
            elem1.getParents(), elem1.getArtParent(), elem1.getPredicates());
        // TODO check
        MathsatSymbolicFormula form1 =
          (MathsatSymbolicFormula)elem1.getPathFormula().getSymbolicFormula();
        elem1.updateMaxIndex(elem1.getPathFormula().getSsa());
        MathsatSymbolicFormula form2 =
          (MathsatSymbolicFormula)elem2.getPathFormula().getSymbolicFormula();
        SSAMap ssa2 = elem2.getPathFormula().getSsa();
        SSAMap ssa1 = elem1.getPathFormula().getSsa();
        Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = mgr.mergeSSAMaps(ssa2, ssa1, false);
        MathsatSymbolicFormula old = (MathsatSymbolicFormula)mgr.makeAnd(
            form2, pm.getFirst().getFirst());
        SymbolicFormula newFormula = mgr.makeAnd(form1, pm.getFirst().getSecond());
        newFormula = mgr.makeOr(old, newFormula);
        ssa1 = pm.getSecond();

        merged.setPathFormula(new PathFormula(newFormula, ssa1));

        // TODO check, what is that???
        // merged.setMaxIndex(maxIndex)
        merged.updateMaxIndex(ssa1);
      }
    }
    else{
//    // set path formula - it is true
//    PathFormula pf = elem1.getPathFormula();
//    merged.setPathFormula(pf);

//    // update initial formula
//    // TODO check
//    MathsatSymbolicFormula form1 =
//    (MathsatSymbolicFormula)elem1.getInitAbstractionSet().getSymbolicFormula();
//    MathsatSymbolicFormula form2 =
//    (MathsatSymbolicFormula)elem2.getInitAbstractionSet().getSymbolicFormula();
//    SSAMap ssa2 = elem2.getInitAbstractionSet().getSsa();
//    SSAMap ssa1 = elem1.getInitAbstractionSet().getSsa();
//    Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = mgr.mergeSSAMaps(ssa2, ssa1, false);
//    MathsatSymbolicFormula old = (MathsatSymbolicFormula)mgr.makeAnd(
//    form2, pm.getFirst().getFirst());
//    SymbolicFormula newFormula = mgr.makeAnd(form1, pm.getFirst().getSecond());
//    newFormula = mgr.makeOr(old, newFormula);
//    ssa1 = pm.getSecond();

//    // TODO these parameters should be cloned (really?)
//    merged.setParents(elem1.getParents());
//    merged.setPredicates(elem1.getPredicates());
//    merged.setPathFormula(new PathFormula(newFormula, ssa1));

//    // TODO compute abstraction here
//    merged.setAbstraction(elem1.getAbstraction());

//    // TODO check, what is that???
//    // merged.setMaxIndex(maxIndex)
//    merged.updateMaxIndex(ssa1);
      merged = elem2;
    }
    return merged;
    //}
  }

  public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                           AbstractElementWithLocation pElement2) throws CPAException {
    throw new CPAException ("Cannot return element with location information");
  }
}
