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

import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.util.ArrayList;
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
      List<Pair<ARTElement, CFANode>> pArtPath,
      CounterexampleTraceInfo pInfo) throws CPAException {

    // the first element on the path which was discovered to be not reachable
    ARTElement root = null;

    // those elements where predicates have been added
//    Collection<ARTElement> strengthened = new ArrayList<ARTElement>();

    boolean foundInterpolant = false;
    for (Pair<ARTElement, CFANode> artPair : pArtPath) {
      ARTElement ae = artPair.getFirst();
      PredicateAbstractElement e = extractElementByType(ae, PredicateAbstractElement.class);

      assert e instanceof AbstractionElement;

      // TODO get predicates from pInfo.getPredicatesForRefinement() list
      //Collection<AbstractionPredicate> newpreds = pInfo.getPredicatesForRefinement(e);
      Collection<AbstractionPredicate> newpreds = new ArrayList<AbstractionPredicate>();

      if (newpreds.size() == 0) {
        if (foundInterpolant) {
          // no predicates after some interpolants have been found means we have
          // reached that part of the path which is not reachable
          // (interpolant is false)

          root = ae;
          break;
        }

        // no predicates on the beginning of the path means the interpolant is true,
        // do nothing
        continue;

      } else {
        foundInterpolant = true;
      }

      Region abs = e.getAbstractionFormula().asRegion();

      boolean newPred = false;

      for (AbstractionPredicate p : newpreds) {
        Region f = p.getAbstractVariable();
        if (f.isFalse()) {
          assert newpreds.size() == 1;

          root = ae;

        } else if (!regionManager.entails(abs, f)) {
          newPred = true;
          abs = regionManager.makeAnd(abs, p.getAbstractVariable());
        }
      }

      if (root != null) {
        // from here on, all elements will have the interpolant false
        // they will be removed from ART and reached set
        break;
      }

      if (newPred) {
        throw new UnsupportedOperationException("TODO");
/*        e.setAbstraction(abs);
        pReached.removeCoverage(ae);
//        strengthened.add(ae);

        if (pReached.checkForCoveredBy(ae)) {
          // this element is now covered by another element
          // the whole subtree has been removed

          return;
        }
*/
      }
    }
    assert root != null : "Infeasible path without interpolant false at some time cannot exist";

//    pReached.removeCoverage(strengthened);
    pReached.replaceWithBottom(root);
  }
}
