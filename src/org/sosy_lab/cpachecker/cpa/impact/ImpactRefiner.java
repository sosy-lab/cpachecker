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
package org.sosy_lab.cpachecker.cpa.impact;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.impact.ImpactAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.DefaultInterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;

public class ImpactRefiner extends AbstractInterpolationBasedRefiner<Formula, Pair<ARTElement, CFANode>> {

  private final FormulaManager fmgr;

  public static ImpactRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    if (!(pCpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(PredicateRefiner.class.getSimpleName() + " could not find the PredicateCPA");
    }

    ImpactCPA impactCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(ImpactCPA.class);
    if (impactCpa == null) {
      throw new InvalidConfigurationException(PredicateRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    LogManager logger = impactCpa.getLogManager();
    FormulaManager fmgr = impactCpa.getFormulaManager();

    InterpolationManager<Formula> manager = new DefaultInterpolationManager(impactCpa.getFormulaManager(),
                                                  impactCpa.getPathFormulaManager(),
                                                  impactCpa.getTheoremProver(),
                                                  impactCpa.getFormulaManagerFactory(),
                                                  impactCpa.getConfiguration(),
                                                  logger);

    ImpactRefiner refiner = new ImpactRefiner(impactCpa.getConfiguration(), logger, pCpa, fmgr, manager);
    return refiner;
  }

  private ImpactRefiner(final Configuration config, final LogManager logger,
      final ConfigurableProgramAnalysis pCpa,
      final FormulaManager pFmgr,
      final InterpolationManager<Formula> pInterpolationManager) throws InvalidConfigurationException, CPAException {

    super(config, logger, pCpa, pInterpolationManager);

    fmgr = pFmgr;
  }

  @Override
  protected List<Pair<ARTElement, CFANode>> transformPath(Path pPath) {
    List<Pair<ARTElement, CFANode>> result = Lists.newArrayList();

    for (ARTElement ae : skip(transform(pPath, Pair.<ARTElement>getProjectionToFirst()), 1)) {
      ImpactAbstractElement pe = extractElementByType(ae, ImpactAbstractElement.class);
      if (pe.isAbstractionElement()) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Pair.of(ae, loc));
      }
    }

    assert pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  private static final Function<ImpactAbstractElement, Formula> GET_BLOCK_FORMULA
                = new Function<ImpactAbstractElement, Formula>() {
                    @Override
                    public Formula apply(ImpactAbstractElement e) {
                      assert e.isAbstractionElement();
                      return ((ImpactAbstractElement.AbstractionElement)e).getBlockFormula().getFormula();
                    };
                  };

  @Override
  protected List<Formula> getFormulasForPath(
      List<Pair<ARTElement, CFANode>> pPath, ARTElement pInitialElement) {

    List<Formula> formulas = transform(pPath,
        Functions.compose(
            GET_BLOCK_FORMULA,
        Functions.compose(
            AbstractElements.extractElementByTypeFunction(ImpactAbstractElement.class),
            Pair.<ARTElement>getProjectionToFirst())));

    return formulas;
  }

  @Override
  protected void performRefinement(ARTReachedSet pReached,
      List<Pair<ARTElement, CFANode>> pPath,
      CounterexampleTraceInfo<Formula> pInfo, boolean pRepeatedCounterexample) throws CPAException {

    List<Formula> itps = pInfo.getPredicatesForRefinement();

    // target element is not really an interpolation point, exclude it
    List<Pair<ARTElement, CFANode>> interpolationPoints = pPath.subList(0, pPath.size()-1);
    assert interpolationPoints.size() == itps.size();

    final ARTElement lastElement = pPath.get(pPath.size()-1).getFirst();
    assert lastElement.isTarget();

    // the first element on the path which was discovered to be not reachable
    // default to the target element
    ARTElement infeasiblePartOfART = lastElement;

    int i = 0;
    for (Pair<ARTElement, CFANode> interpolationPoint : interpolationPoints) {
      Formula itp = itps.get(i++);

      if (itp.isTrue()) {
        // do nothing
        continue;
      }

      if (itp.isFalse()) {

        // we have reached the part of the path that is infeasible
        infeasiblePartOfART = interpolationPoint.getFirst();
        break;
      }

      ARTElement ae = interpolationPoint.getFirst();
      ImpactAbstractElement e = extractElementByType(ae, ImpactAbstractElement.class);

      assert e.isAbstractionElement();

      Formula oldAbs = e.getStateFormula();
      Formula newAbs = fmgr.makeAnd(oldAbs, itp);

      if (!newAbs.equals(oldAbs)) {
        ((AbstractionElement)e).setStateFormula(newAbs);

        pReached.removeCoverage(ae);

        // TODO not sure why this was here
//        if (pReached.checkForCoveredBy(ae)) {
//          // this element is now covered by another element
//          // the whole subtree has been removed
//
//          return;
//        }
      }
    }

    pReached.replaceWithBottom(infeasiblePartOfART);

    assert !pReached.asReachedSet().contains(lastElement);
  }
}