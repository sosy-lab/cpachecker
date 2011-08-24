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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;


public class ABMPredicateReducer implements Reducer {

  static final Timer reduceTimer = new Timer();
  static final Timer expandTimer = new Timer();

  private final RegionManager rmgr;
  private final FormulaManager fmgr;
  private final PredicateAbstractionManager pmgr;
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
  public Object getHashCodeForElement(AbstractElement pElementKey, Precision pPrecisionKey) {

    PredicateAbstractElement element = (PredicateAbstractElement)pElementKey;
    PredicatePrecision precision = (PredicatePrecision)pPrecisionKey;

    return Pair.of(element.getAbstractionFormula().asRegion(), precision);
  }

  private Map<Pair<Integer, Block>, Precision> reduceCache = new HashMap<Pair<Integer, Block>, Precision>();

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision,
      Block pContext) {
    PredicatePrecision precision = (PredicatePrecision)pPrecision;
    Pair<Integer, Block> key = Pair.of(precision.getId(), pContext);
    Precision result = reduceCache.get(key);
    if(result != null) {
      return result;
    }

    result = new ReducedPredicatePrecision(precision, pContext);
    reduceCache.put(key, result);
    return result;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext, Precision pReducedPrecision) {
    PredicatePrecision rootPrecision = (PredicatePrecision)pRootPrecision;
    PredicatePrecision toplevelPrecision = rootPrecision;
    if(rootPrecision instanceof ReducedPredicatePrecision) {
      toplevelPrecision = ((ReducedPredicatePrecision)rootPrecision).getRootPredicatePrecision();
    }

    PredicatePrecision derivedToplevelPrecision = ((ReducedPredicatePrecision)pReducedPrecision).getRootPredicatePrecision();

    if(derivedToplevelPrecision == toplevelPrecision) {
      return pRootPrecision;
    }

    PredicatePrecision mergedToplevelPrecision = mergePrecisions(toplevelPrecision, derivedToplevelPrecision);

    return getVariableReducedPrecision(mergedToplevelPrecision, pRootContext);
  }


  private PredicatePrecision mergePrecisions(PredicatePrecision lhs, PredicatePrecision rhs) {
    Set<AbstractionPredicate> globalPredicates = new HashSet<AbstractionPredicate>();
    globalPredicates.addAll(rhs.getGlobalPredicates());
    globalPredicates.addAll(lhs.getGlobalPredicates());

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
    pmapBuilder.putAll(rhs.getPredicateMap());
    pmapBuilder.putAll(lhs.getPredicateMap());

    return new PredicatePrecision(pmapBuilder.build(), globalPredicates);
  }

  private class ReducedPredicatePrecision extends PredicatePrecision {
    private final PredicatePrecision rootPredicatePrecision;

    private final PredicatePrecision expandedPredicatePrecision;
    private final Block context;

    private ImmutableSetMultimap<CFANode, AbstractionPredicate> evaluatedPredicateMap;
    private ImmutableSet<AbstractionPredicate> evaluatedGlobalPredicates;


    public ReducedPredicatePrecision(PredicatePrecision expandedPredicatePrecision, Block context) {
      super(null);

      this.expandedPredicatePrecision = expandedPredicatePrecision;
      this.context = context;

      if(expandedPredicatePrecision instanceof ReducedPredicatePrecision) {
        this.rootPredicatePrecision = ((ReducedPredicatePrecision) expandedPredicatePrecision).getRootPredicatePrecision();
      }
      else {
        this.rootPredicatePrecision = expandedPredicatePrecision;
      }
      assert !(rootPredicatePrecision instanceof ReducedPredicatePrecision);

      this.evaluatedPredicateMap = null;
      this.evaluatedGlobalPredicates = null;
    }

    public PredicatePrecision getRootPredicatePrecision() {
      return rootPredicatePrecision;
    }

    private void computeView() {
      if(evaluatedPredicateMap == null) {
        ReducedPredicatePrecision lExpandedPredicatePrecision = null;
        if(expandedPredicatePrecision instanceof ReducedPredicatePrecision) {
          lExpandedPredicatePrecision = (ReducedPredicatePrecision)expandedPredicatePrecision;
        }

        evaluatedGlobalPredicates = ImmutableSet.copyOf(relevantComputer.getRelevantPredicates(context, rootPredicatePrecision.getGlobalPredicates()));

        ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
        Set<CFANode> keySet = lExpandedPredicatePrecision==null?rootPredicatePrecision.getPredicateMap().keySet():lExpandedPredicatePrecision.approximatePredicateMap().keySet();
        for(CFANode node : keySet) {
          if(context.getNodes().contains(node)) {
            Collection<AbstractionPredicate> set = relevantComputer.getRelevantPredicates(context, rootPredicatePrecision.getPredicates(node));
            pmapBuilder.putAll(node, set);
          }
        }

        evaluatedPredicateMap = pmapBuilder.build();
      }
    }

    private SetMultimap<CFANode, AbstractionPredicate> approximatePredicateMap() {
      if(evaluatedPredicateMap == null) {
        return rootPredicatePrecision.getPredicateMap();
      } else {
        return evaluatedPredicateMap;
      }
    }

    @Override
    public SetMultimap<CFANode, AbstractionPredicate> getPredicateMap() {
      computeView();
      return evaluatedPredicateMap;
    }

    @Override
    public Set<AbstractionPredicate> getGlobalPredicates() {
      if(evaluatedGlobalPredicates != null) {
        return evaluatedGlobalPredicates;
      } else {
        return relevantComputer.getRelevantPredicates(context, rootPredicatePrecision.getGlobalPredicates());
      }
    }

    @Override
    public Set<AbstractionPredicate> getPredicates(CFANode loc) {
      assert context.getNodes().contains(loc);

      if(evaluatedPredicateMap != null) {
        Set<AbstractionPredicate> result = evaluatedPredicateMap.get(loc);
        if (result.isEmpty()) {
          result = evaluatedGlobalPredicates;
        }
        return result;
      }
      else {
        Set<AbstractionPredicate> result = relevantComputer.getRelevantPredicates(context, rootPredicatePrecision.getPredicates(loc));
        if (result.isEmpty()) {
          result = relevantComputer.getRelevantPredicates(context, rootPredicatePrecision.getGlobalPredicates());
        }
        return result;
      }
    }

    @Override
    public boolean equals(Object pObj) {
      if (pObj == this) {
        return true;
      } else if (!(pObj instanceof ReducedPredicatePrecision)) {
        return false;
      } else {
        computeView();
        return evaluatedPredicateMap.equals(((ReducedPredicatePrecision)pObj).evaluatedPredicateMap);
      }
    }

    @Override
    public int hashCode() {
      computeView();
      return evaluatedPredicateMap.hashCode();
    }

  }
}
