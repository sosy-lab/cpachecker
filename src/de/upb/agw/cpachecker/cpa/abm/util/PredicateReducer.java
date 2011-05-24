package de.upb.agw.cpachecker.cpa.abm.util;

import java.util.Collection;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.util.AbstractElements;
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

import de.upb.agw.cpachecker.cpa.abm.predicate.TargetPredicateAbstractElement;

/**
 * Helper class that provides methods to reduce and expand <code>PredicateAbstactElement</code>s to/from a set of relevant predicates.
 * @author dwonisch
 *
 */
public class PredicateReducer {
  public static Timer reduceTimer = new Timer();
  public static Timer expandTimer = new Timer();
    
  private FormulaManager fmgr;
  private PredicateRefinementManager<Integer, Integer> pmgr;
  private RelevantPredicatesComputer relevantComputer;
  
  public PredicateReducer(FormulaManager fmgr, PredicateRefinementManager<Integer, Integer> pmgr, RelevantPredicatesComputer relevantComputer) {
    this.fmgr = fmgr;
    this.pmgr = pmgr;
    this.relevantComputer = relevantComputer;
  } 
  
  public PredicateAbstractElement getVariableReducedElement(ARTElement expandedElement, CachedSubtree context) {
    return getVariableReducedElement(AbstractElements.extractElementByType(expandedElement, PredicateAbstractElement.class), context);
  }
  
  public PredicateAbstractElement getVariableReducedElement(PredicateAbstractElement predicateElement, CachedSubtree context) {
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
          relevantComputer.getIrrelevantPredicates(context, predicates);

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

      if (predicateElement instanceof TargetPredicateAbstractElement) {
        return new TargetPredicateAbstractElement(pathFormula,
            newAbstractionFormula);
      } else {
        return new PredicateAbstractElement.AbstractionElement(pathFormula,
            newAbstractionFormula);
      }
    } finally {
      reduceTimer.stop();
    }

  }
  
  public PredicateAbstractElement getVariableExpandedElement(ARTElement rootElement, CachedSubtree rootContext, ARTElement reducedElement) {
    return getVariableExpandedElement(AbstractElements.extractElementByType(rootElement, PredicateAbstractElement.class), rootContext, AbstractElements.extractElementByType(reducedElement, PredicateAbstractElement.class));
  }
  
  public PredicateAbstractElement getVariableExpandedElement(PredicateAbstractElement rootElement, CachedSubtree rootContext, PredicateAbstractElement reducedElement) {

    if (!(reducedElement instanceof PredicateAbstractElement.AbstractionElement)) { return reducedElement; }
    //Note: FCCP might introduce some additional abstraction if root region is not a cube 
    expandTimer.start();
    try {      

      AbstractionFormula rootElementAbstractionFormula =
          rootElement.getAbstractionFormula();

      Collection<AbstractionPredicate> rootPredicates =
          pmgr.extractPredicates(rootElementAbstractionFormula.asRegion());
      Collection<AbstractionPredicate> relevantRootPredicates =
          relevantComputer.getRelevantPredicates(rootContext, rootPredicates);
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
      if (reducedElement instanceof TargetPredicateAbstractElement) {
        return new TargetPredicateAbstractElement(pathFormula,
            newAbstractionFormula);
      } else {
        return new PredicateAbstractElement.AbstractionElement(pathFormula,
            newAbstractionFormula);
      }
    } finally {
      expandTimer.stop();
    }

  }
}
