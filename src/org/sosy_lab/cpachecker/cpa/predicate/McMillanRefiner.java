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
package org.sosy_lab.cpachecker.cpa.predicate;

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
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.DefaultInterpolationManager;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;

public class McMillanRefiner extends AbstractInterpolationBasedRefiner<Formula, Pair<ARTElement, CFANode>> {

  private int i = 0;

//  private final RegionManager regionManager;
//  private final PredicateAbstractionManager abstractionManager;

  public static Refiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    if (!(pCpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(McMillanRefiner.class.getSimpleName() + " could not find the PredicateCPA");
    }

    PredicateCPA predicateCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(McMillanRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    LogManager logger = predicateCpa.getLogger();

    DefaultInterpolationManager manager = new DefaultInterpolationManager(predicateCpa.getFormulaManager(),
                                          predicateCpa.getPathFormulaManager(),
                                          predicateCpa.getTheoremProver(),
                                          predicateCpa.getFormulaManagerFactory(),
                                          predicateCpa.getConfiguration(),
                                          logger);

    return new McMillanRefiner(predicateCpa.getConfiguration(), logger, pCpa, predicateCpa, manager);
  }

  protected McMillanRefiner(final Configuration config, final LogManager logger,
      final ConfigurableProgramAnalysis pCpa,
      final PredicateCPA predicateCpa, final DefaultInterpolationManager pInterpolationManager) throws CPAException, InvalidConfigurationException {

    super(config, logger, pCpa, pInterpolationManager);

//    regionManager = predicateCpa.getRegionManager();
//    abstractionManager = predicateCpa.getPredicateManager();
  }


  @Override
  protected final List<Pair<ARTElement, CFANode>> transformPath(Path pPath) {
    List<Pair<ARTElement, CFANode>> result = Lists.newArrayList();

    for (ARTElement ae : skip(transform(pPath, Pair.<ARTElement>getProjectionToFirst()), 1)) {
      PredicateAbstractElement pe = extractElementByType(ae, PredicateAbstractElement.class);
      if (pe.isAbstractionElement()) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Pair.of(ae, loc));
      }
    }

    assert pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  private static final Function<PredicateAbstractElement, Formula> GET_BLOCK_FORMULA
                = new Function<PredicateAbstractElement, Formula>() {
                    @Override
                    public Formula apply(PredicateAbstractElement e) {
                      assert e.isAbstractionElement();
                      return e.getAbstractionFormula().getBlockFormula();
                    };
                  };

  @Override
  protected List<Formula> getFormulasForPath(List<Pair<ARTElement, CFANode>> path, ARTElement initialElement) throws CPATransferException {

    List<Formula> formulas = transform(path,
        Functions.compose(
            GET_BLOCK_FORMULA,
        Functions.compose(
            AbstractElements.extractElementByTypeFunction(PredicateAbstractElement.class),
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

    // the first element on the path which was discovered to be not reachable
    ARTElement root = null;

    int i = 0;
    for (Pair<ARTElement, CFANode> interpolationPoint : interpolationPoints) {
      Formula localItp = itps.get(i++);

      if (localItp.isTrue()) {
        // do nothing
        continue;
      }

      if (localItp.isFalse()) {

        // we have reached the part of the path that is infeasible
        root = interpolationPoint.getFirst();
        pReached.replaceWithBottom(root);
        break;
      }

      ARTElement ae = interpolationPoint.getFirst();
      PredicateAbstractElement e = extractElementByType(ae, PredicateAbstractElement.class);

      assert e.isAbstractionElement();
      throw new UnsupportedOperationException();
/*
      Region oldAbs = e.getAbstractionFormula().asRegion();
      Region newAbs = regionManager.makeAnd(oldAbs, pred.getAbstractVariable());

      if (!newAbs.equals(oldAbs)) {
        Formula symbNewAbs = abstractionManager.toConcrete(newAbs, e.getPathFormula().getSsa());
        e.setAbstraction(new AbstractionFormula(newAbs, symbNewAbs, e.getAbstractionFormula().getBlockFormula()));

        pReached.removeCoverage(ae);

        // TODO not sure why this was here
//        if (pReached.checkForCoveredBy(ae)) {
//          // this element is now covered by another element
//          // the whole subtree has been removed
//
//          return;
//        }
      }
*/
    }

    ARTElement lastElement = pPath.get(pPath.size()-1).getFirst();
    assert !pReached.asReachedSet().contains(lastElement);
    if (++this.i == 10) {
      throw new RefinementFailedException(Reason.InterpolationFailed, null);
    }
  }
}
