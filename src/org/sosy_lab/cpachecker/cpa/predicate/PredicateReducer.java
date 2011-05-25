package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Collection;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementHash;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;
import de.upb.agw.cpachecker.cpa.abm.util.PrecisionUtils;
import de.upb.agw.cpachecker.cpa.abm.util.RelevantPredicatesComputer;

public class PredicateReducer implements Reducer {

  public static Timer reduceTimer = new Timer();
  public static Timer expandTimer = new Timer();
    
  private final FormulaManager fmgr;
  private final PredicateRefinementManager<?, ?> pmgr;
  private final RelevantPredicatesComputer relevantComputer;
  
  public PredicateReducer(FormulaManager fmgr, PredicateRefinementManager<?, ?> pmgr, RelevantPredicatesComputer relevantComputer) {
    this.fmgr = fmgr;
    this.pmgr = pmgr;
    this.relevantComputer = relevantComputer;
  } 
  
  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, CachedSubtree pContext,
      CFANode pLocation) {
    
    PredicateAbstractElement predicateElement = (PredicateAbstractElement)pExpandedElement;
    
    if (!(predicateElement instanceof PredicateAbstractElement.AbstractionElement)) {
      return predicateElement;
    }    
    
    reduceTimer.start();
    try {  
      AbstractionFormula abstractionFormula =
          predicateElement.getAbstractionFormula();

      Region oldRegion = predicateElement.getAbstractionFormula().asRegion();

      Collection<AbstractionPredicate> predicates =
          pmgr.extractPredicates(abstractionFormula.asRegion());
      Collection<AbstractionPredicate> removePredicates =
          relevantComputer.getIrrelevantPredicates(pContext, predicates);

      //System.out.println("=> Removing the following predicates: " + removePredicates);

      RegionManager bddRegionManager = BDDRegionManager.getInstance();
      Region newRegion = oldRegion;
      for (AbstractionPredicate predicate : removePredicates) {
        newRegion =
            bddRegionManager.makeExists(newRegion,
                predicate.getAbstractVariable());
      }

      //System.out.println("Resulting region: " + newRegion);

      PathFormula pathFormula = predicateElement.getPathFormula();
      Formula newFormula =
          fmgr.instantiate(pmgr.toConcrete(newRegion), pathFormula.getSsa());

      //System.out.println("New formula: " + newFormula);
      AbstractionFormula newAbstractionFormula =
            new AbstractionFormula(newRegion, newFormula, predicateElement
                .getAbstractionFormula().getBlockFormula());

      return new PredicateAbstractElement.AbstractionElement(pathFormula,
            newAbstractionFormula);
    } finally {
      reduceTimer.stop();
    }
  }

  @Override
  public AbstractElement getVariableExpandedElement(
      AbstractElement pRootElement, CachedSubtree pRootContext,
      AbstractElement pReducedElement) {
    
    PredicateAbstractElement rootElement = (PredicateAbstractElement)pRootElement;
    PredicateAbstractElement reducedElement = (PredicateAbstractElement)pReducedElement;

    if (!(reducedElement instanceof PredicateAbstractElement.AbstractionElement)) { return reducedElement; }
    //Note: FCCP might introduce some additional abstraction if root region is not a cube 
    expandTimer.start();
    try {      

      AbstractionFormula rootElementAbstractionFormula =
          rootElement.getAbstractionFormula();

      Collection<AbstractionPredicate> rootPredicates =
          pmgr.extractPredicates(rootElementAbstractionFormula.asRegion());
      Collection<AbstractionPredicate> relevantRootPredicates =
          relevantComputer.getRelevantPredicates(pRootContext, rootPredicates);
      //for each removed predicate, we have to lookup the old (expanded) value and insert it to the reducedElements region

      Region reducedRegion = reducedElement.getAbstractionFormula().asRegion();
      Region rootRegion = rootElement.getAbstractionFormula().asRegion();

      RegionManager bddRegionManager = BDDRegionManager.getInstance();
      Region removedInformationRegion = rootRegion;
      for (AbstractionPredicate predicate : relevantRootPredicates) {
        removedInformationRegion =
            bddRegionManager.makeExists(removedInformationRegion,
                predicate.getAbstractVariable());
      }
  
      //System.out.println("Removed information region: " + removedInformationRegion);

      Region expandedRegion =
          bddRegionManager.makeAnd(reducedRegion, removedInformationRegion);

      PathFormula pathFormula = reducedElement.getPathFormula();

      //pathFormula.getSSa() might not contain index for the newly added variables in predicates; while the actual index is not really important at this point,
      //there still should be at least _some_ index for each variable of the abstraction formula.
      SSAMapBuilder builder = pathFormula.getSsa().builder();
      for (String var : rootElement.getPathFormula().getSsa().allVariables()) {
        //if we do not have the index in the reduced map..
        if (pathFormula.getSsa().getIndex(var) == -1) {
          //add an index (with the value of rootSSA)
          builder.setIndex(var,
              rootElement.getPathFormula().getSsa().getIndex(var));
        }
      }
      SSAMap newSSA = builder.build();
      pathFormula = pmgr.getPathFormulaManager().makeNewPathFormula(pathFormula, newSSA);

      Formula newFormula =
          fmgr.instantiate(pmgr.toConcrete(expandedRegion),
              pathFormula.getSsa());
      Formula blockFormula =
          reducedElement.getAbstractionFormula().getBlockFormula();

      AbstractionFormula newAbstractionFormula =
          new AbstractionFormula(expandedRegion, newFormula, blockFormula);
      
      return new PredicateAbstractElement.AbstractionElement(pathFormula,
          newAbstractionFormula);
    } finally {
      expandTimer.stop();
    }
  }

  @Override
  public boolean isEqual(AbstractElement pReducedTargetElement,
      AbstractElement pCandidateElement) {

    PredicateAbstractElement reducedTargetElement = (PredicateAbstractElement)pReducedTargetElement;
    PredicateAbstractElement candidateElement = (PredicateAbstractElement)pCandidateElement;
    
    return candidateElement.getAbstractionFormula().asRegion().equals(reducedTargetElement.getAbstractionFormula().asRegion());
  }

  @Override
  public AbstractElementHash getHashCodeForElement(AbstractElement pElementKey,
      Precision pPrecisionKey, CachedSubtree pContext, CachedSubtreeManager pCsmgr) {
    
    PredicateAbstractElement element = (PredicateAbstractElement)pElementKey;
    PredicatePrecision precision = (PredicatePrecision)pPrecisionKey;
    
    return new PredicateElementHash(element, precision, pContext, pCsmgr);
  }

  private class PredicateElementHash implements AbstractElementHash {
    private final CachedSubtree context;
    private final Region region;
    private final PredicatePrecision precision;
    private final CachedSubtreeManager csmgr;
    
    PredicateElementHash(PredicateAbstractElement element, PredicatePrecision precision, CachedSubtree context, CachedSubtreeManager csmgr) {      
      this.precision = precision;
      this.context = context;
      this.region = element.getAbstractionFormula().asRegion();
      this.csmgr = csmgr;
    }
    
    @Override
    public boolean equals(Object other) {
      if(!(other instanceof PredicateElementHash)) {
       return false; 
      }
      PredicateElementHash hOther = (PredicateElementHash)other;
      if(!region.equals(hOther.region)) {
        return false;
      }        
      return PrecisionUtils.relevantComparePrecisions(precision, context, hOther.precision, hOther.context, relevantComputer, csmgr);
    }
    
    @Override
    public int hashCode() {
      return region.hashCode() * 17 + PrecisionUtils.relevantComputeHashCode(precision, context, relevantComputer, csmgr);
    }
    
    @Override
    public String toString() {
      return region.toString();
    }
  }
}
