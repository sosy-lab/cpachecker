/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit.refiner;

import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.PredicateMap;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class PredicatingExplicitRefiner implements IExplicitRefiner {

  protected List<Pair<ARTElement, CFAEdge>> currentErrorPath  = null;

  private int numberOfPredicateRefinements                    = 0;
  private int numberOfPredicateRefinementsDone                = 0;

  @Override
  public final List<Pair<ARTElement, CFANode>> transformPath(Path errorPath) {
    numberOfPredicateRefinements++;

    List<Pair<ARTElement, CFANode>> result = Lists.newArrayList();

    for (ARTElement ae : transform(errorPath, Pair.<ARTElement>getProjectionToFirst())) {
      PredicateAbstractElement pe = extractElementByType(ae, PredicateAbstractElement.class);
      if (pe.isAbstractionElement()) {
        CFANode location = AbstractElements.extractLocation(ae);
        result.add(Pair.of(ae, location));
      }
    }

    assert errorPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  @Override
  public List<Formula> getFormulasForPath(List<Pair<ARTElement, CFANode>> errorPath, ARTElement initialElement) throws CPATransferException {
    List<Formula> formulas = transform(errorPath,
        Functions.compose(
            GET_BLOCK_FORMULA,
        Functions.compose(
            AbstractElements.extractElementByTypeFunction(PredicateAbstractElement.class),
            Pair.<ARTElement>getProjectionToFirst())));

    return formulas;
  }

  @Override
  public Pair<ARTElement, Precision> performRefinement(Precision oldPrecision,
      List<Pair<ARTElement, CFANode>> errorPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> pInfo) throws CPAException {
    numberOfPredicateRefinementsDone++;
    // create the mapping of CFA nodes to predicates, based on the counter example trace info
    PredicateMap predicateMap = new PredicateMap(pInfo.getPredicatesForRefinement(), errorPath);

    Precision precision = createPredicatePrecision(extractPredicatePrecision(oldPrecision), predicateMap);
    ARTElement interpolationPoint = predicateMap.firstInterpolationPoint.getFirst();

    return Pair.of(interpolationPoint, precision);
  }

  /**
   * This helper function is used to extract the block formula from an abstraction node.
   */
  private static final Function<PredicateAbstractElement, Formula> GET_BLOCK_FORMULA
                = new Function<PredicateAbstractElement, Formula>() {
                    @Override
                    public Formula apply(PredicateAbstractElement e) {
                      assert e.isAbstractionElement();
                      return e.getAbstractionFormula().getBlockFormula();
                    };
                  };

  /**
   * This method extracts the predicate precision.
   *
   * @param precision the current precision
   * @return the predicate precision, or null, if the PredicateCPA is not in use
   */
  private PredicatePrecision extractPredicatePrecision(Precision precision) {
    PredicatePrecision predicatePrecision = Precisions.extractPrecisionByType(precision, PredicatePrecision.class);
    if(predicatePrecision == null) {
      throw new IllegalStateException("Could not find the PredicatePrecision for the error element");
    }
    return predicatePrecision;
  }

  /**
   * This method creates the new predicate precision based on the old precision and the increment.
   *
   * @param oldPredicatePrecision the old predicate precision to build on
   * @param predicateMap the precision increment
   * @return the new predicate precision
   */
  private PredicatePrecision createPredicatePrecision(PredicatePrecision oldPredicatePrecision,
                                                    PredicateMap predicateMap) {
    Multimap<CFANode, AbstractionPredicate> oldPredicateMap = oldPredicatePrecision.getPredicateMap();
    Set<AbstractionPredicate> globalPredicates = oldPredicatePrecision.getGlobalPredicates();

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
    pmapBuilder.putAll(oldPredicateMap);

    for(Map.Entry<CFANode, AbstractionPredicate> predicateAtLocation : predicateMap.getPredicateMapping().entries()) {
      pmapBuilder.putAll(predicateAtLocation.getKey(), predicateAtLocation.getValue());
    }

    return new PredicatePrecision(pmapBuilder.build(), globalPredicates);
  }

  @Override
  public boolean hasMadeProgress(List<Pair<ARTElement, CFAEdge>> currentErrorPath, Precision currentPrecision) {
    return true;
  }

  @Override
  public void setCurrentErrorPath(List<Pair<ARTElement, CFAEdge>> currentErrorPath) {
    this.currentErrorPath = currentErrorPath;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println(this.getClass().getSimpleName() + ":");
    out.println("  number of predicate refinements:           " + numberOfPredicateRefinements);
    out.println("  number of predicate refinements done:      " + numberOfPredicateRefinementsDone);
  }
}
