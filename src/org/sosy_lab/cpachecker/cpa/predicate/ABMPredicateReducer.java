/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RelevantPredicatesComputer;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

import com.google.common.collect.ImmutableSetMultimap;


public class ABMPredicateReducer implements Reducer {

  static final Timer reduceTimer = new Timer();
  static final Timer expandTimer = new Timer();

  private final RegionManager rmgr;
  private final FormulaManager fmgr;
  private final PredicateRefinementManager<?, ?> pmgr;
  private final RelevantPredicatesComputer relevantComputer;

  public ABMPredicateReducer(ABMPredicateCPA cpa) {
    this.rmgr = cpa.getRegionManager();
    this.fmgr = cpa.getFormulaManager();
    this.pmgr = cpa.getPredicateManager();
    this.relevantComputer = cpa.getRelevantPredicatesComputer();
  }

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, Block pContext,
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

      Region newRegion = oldRegion;
      for (AbstractionPredicate predicate : removePredicates) {
        newRegion = rmgr.makeExists(newRegion, predicate.getAbstractVariable());
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
      AbstractElement pRootElement, Block pReducedContext,
      AbstractElement pReducedElement) {

    PredicateAbstractElement rootElement = (PredicateAbstractElement)pRootElement;
    PredicateAbstractElement reducedElement = (PredicateAbstractElement)pReducedElement;

    if (!(reducedElement instanceof PredicateAbstractElement.AbstractionElement)) { return reducedElement; }
    //Note: ABM might introduce some additional abstraction if root region is not a cube
    expandTimer.start();
    try {

      AbstractionFormula rootElementAbstractionFormula =
          rootElement.getAbstractionFormula();

      Collection<AbstractionPredicate> rootPredicates =
          pmgr.extractPredicates(rootElementAbstractionFormula.asRegion());
      Collection<AbstractionPredicate> relevantRootPredicates =
          relevantComputer.getRelevantPredicates(pReducedContext, rootPredicates);
      //for each removed predicate, we have to lookup the old (expanded) value and insert it to the reducedElements region

      Region reducedRegion = reducedElement.getAbstractionFormula().asRegion();
      Region rootRegion = rootElement.getAbstractionFormula().asRegion();

      Region removedInformationRegion = rootRegion;
      for (AbstractionPredicate predicate : relevantRootPredicates) {
        removedInformationRegion = rmgr.makeExists(removedInformationRegion,
                                                   predicate.getAbstractVariable());
      }

      //System.out.println("Removed information region: " + removedInformationRegion);

      Region expandedRegion = rmgr.makeAnd(reducedRegion, removedInformationRegion);

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
  public Object getHashCodeForElement(AbstractElement pElementKey, Precision pPrecisionKey) {

    PredicateAbstractElement element = (PredicateAbstractElement)pElementKey;
    PredicatePrecision precision = (PredicatePrecision)pPrecisionKey;

    return Pair.of(element.getAbstractionFormula().asRegion(), precision);
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision,
      Block pContext) {
    PredicatePrecision precision = (PredicatePrecision)pPrecision;

    Collection<AbstractionPredicate> globalPredicates = relevantComputer.getRelevantPredicates(pContext, precision.getGlobalPredicates());

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
    for(CFANode node : precision.getPredicateMap().keySet()) {
      if(pContext.getNodes().contains(node)) {
        Collection<AbstractionPredicate> set = relevantComputer.getRelevantPredicates(pContext, precision.getPredicates(node));
        pmapBuilder.putAll(node, set);
      }
    }

    return new ReducedPredicatePrecision(pmapBuilder.build(), globalPredicates, precision);
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext, Precision pReducedPrecision) {
    PredicatePrecision derivedToplevelPrecision = ((ReducedPredicatePrecision)pReducedPrecision).getRootPredicatePrecision();
    PredicatePrecision derivedRootPrecision = (PredicatePrecision)getVariableReducedPrecision(derivedToplevelPrecision, pRootContext);

    PredicatePrecision rootPrecision = (PredicatePrecision)pRootPrecision;
    PredicatePrecision toplevelPrecision = rootPrecision;
    if(rootPrecision instanceof ReducedPredicatePrecision) {
      toplevelPrecision = ((ReducedPredicatePrecision)rootPrecision).getRootPredicatePrecision();
    }

    Set<AbstractionPredicate> globalPredicates = new HashSet<AbstractionPredicate>();
    globalPredicates.addAll(derivedRootPrecision.getGlobalPredicates());
    globalPredicates.addAll(rootPrecision.getGlobalPredicates());

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
    pmapBuilder.putAll(derivedRootPrecision.getPredicateMap());
    pmapBuilder.putAll(rootPrecision.getPredicateMap());

    return new ReducedPredicatePrecision(pmapBuilder.build(), globalPredicates, mergePrecisions(toplevelPrecision, derivedToplevelPrecision));
  }

  private PredicatePrecision mergePrecisions(PredicatePrecision lhs, PredicatePrecision rhs) {
    Set<AbstractionPredicate> globalPredicates = new HashSet<AbstractionPredicate>();
    globalPredicates.addAll(lhs.getGlobalPredicates());
    globalPredicates.addAll(rhs.getGlobalPredicates());

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
    pmapBuilder.putAll(lhs.getPredicateMap());
    pmapBuilder.putAll(rhs.getPredicateMap());

    return new PredicatePrecision(pmapBuilder.build(), globalPredicates);
  }

  private static class ReducedPredicatePrecision extends PredicatePrecision {
    private final PredicatePrecision rootPredicatePrecision;

    public ReducedPredicatePrecision(ImmutableSetMultimap<CFANode, AbstractionPredicate> pPredicateMap, Collection<AbstractionPredicate> pGlobalPredicates, PredicatePrecision expandedPredicatePrecision) {
      super(pPredicateMap, pGlobalPredicates);
      if(expandedPredicatePrecision instanceof ReducedPredicatePrecision) {
        this.rootPredicatePrecision = ((ReducedPredicatePrecision) expandedPredicatePrecision).getRootPredicatePrecision();
      }
      else {
        this.rootPredicatePrecision = expandedPredicatePrecision;
      }
      assert !(rootPredicatePrecision instanceof ReducedPredicatePrecision);
    }

    public PredicatePrecision getRootPredicatePrecision() {
      return rootPredicatePrecision;
    }

  }
}
