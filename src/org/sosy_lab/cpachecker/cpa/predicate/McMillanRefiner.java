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

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

public class McMillanRefiner extends AbstractInterpolationBasedRefiner {

  private final RegionManager regionManager;

  public McMillanRefiner(final ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException, CPAException {
    super(pCpa);

    regionManager = predicateCpa.getRegionManager();
  }

  @Override
  protected void performRefinement(ARTReachedSet pReached,
      List<Pair<ARTElement, CFANode>> pPath,
      CounterexampleTraceInfo pInfo) throws CPAException {

    List<Collection<AbstractionPredicate>> newPreds = pInfo.getPredicatesForRefinement();

    // target element is not really an interpolation point, exclude it
    List<Pair<ARTElement, CFANode>> interpolationPoints = pPath.subList(0, pPath.size()-1);
    assert interpolationPoints.size() == newPreds.size();

    // the first element on the path which was discovered to be not reachable
    ARTElement root = null;

    int i = 0;
    for (Pair<ARTElement, CFANode> interpolationPoint : interpolationPoints) {
      Collection<AbstractionPredicate> localPreds = newPreds.get(i++);

      if (localPreds.isEmpty()) {

        // no predicates on the beginning of the path means the interpolant is true,
        // do nothing
        continue;
      }

      if ((localPreds.size() == 1)
          && getOnlyElement(localPreds).getSymbolicAtom().isFalse()) {

        // we have reached the part of the path that is infeasible
        root = interpolationPoint.getFirst();
        break;
      }

      ARTElement ae = interpolationPoint.getFirst();
      PredicateAbstractElement e = extractElementByType(ae, PredicateAbstractElement.class);

      assert e instanceof AbstractionElement;

      Region abs = e.getAbstractionFormula().asRegion();
      Region newAbs = abs;

      boolean newPred = false;

      for (AbstractionPredicate p : localPreds) {
        Region f = p.getAbstractVariable();

        if (!regionManager.entails(abs, f)) {
          newPred = true;
          newAbs = regionManager.makeAnd(newAbs, f);
        }
      }

      if (newPred) {
//        e.setAbstraction(newAbs);
        pReached.removeCoverage(ae);

        if (pReached.checkForCoveredBy(ae)) {
          // this element is now covered by another element
          // the whole subtree has been removed

          return;
        }
        throw new UnsupportedOperationException("TODO");
      }
    }
    assert root != null : "Infeasible path without interpolant false at some time cannot exist";

    pReached.replaceWithBottom(root);
  }
}
