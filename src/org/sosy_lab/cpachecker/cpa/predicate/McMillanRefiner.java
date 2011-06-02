/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.AbstractARTBasedRefiner;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

public class McMillanRefiner extends AbstractARTBasedRefiner {

  private final RegionManager regionManager;
  private final PredicateRefinementManager<?,?> formulaManager;

  private final LogManager logger;

  public McMillanRefiner(final ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    super(pCpa);

    PredicateCPA predicateCpa = this.getArtCpa().retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(getClass().getSimpleName() + " needs a PredicateCPA");
    }

    regionManager = predicateCpa.getRegionManager();
    formulaManager = predicateCpa.getPredicateManager();
    logger = predicateCpa.getLogger();
  }

  @Override
  public boolean performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException, InterruptedException {

    logger.log(Level.FINEST, "Starting refinement for PredicateCPA");

    // create path with all abstraction location elements (excluding the initial
    // element, which is not in pPath)
    // the last element is the element corresponding to the error location
    // (which is twice in pPath)
    ArrayList<PredicateAbstractElement> path = new ArrayList<PredicateAbstractElement>();
    PredicateAbstractElement lastElement = null;
    for (Pair<ARTElement,CFAEdge> artPair : pPath) {
      PredicateAbstractElement symbElement = extractElementByType(artPair.getFirst(), PredicateAbstractElement.class);

      if (symbElement instanceof AbstractionElement && symbElement != lastElement) {
        path.add(symbElement);
      }
      lastElement = symbElement;
    }
    assert path.size() == pPath.size() - 1 : "not all elements are abstraction nodes?";


    // build the counterexample
    CounterexampleTraceInfo info = formulaManager.buildCounterexampleTrace(path);

    // if error is spurious refine
    if (info.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");
      performRefinement(pReached, pPath, info);

      return true;
    } else {
      logger.log(Level.FINEST, "Error trace is not spurious");
      // we have a real error
      return false;
    }
  }

  private void performRefinement(ARTReachedSet pReached,
      Path pArtPath, CounterexampleTraceInfo pInfo) throws CPAException {

    // the first element on the path which was discovered to be not reachable
    ARTElement root = null;

    // those elements where predicates have been added
//    Collection<ARTElement> strengthened = new ArrayList<ARTElement>();

    boolean foundInterpolant = false;
    for (Pair<ARTElement,CFAEdge> artPair : pArtPath) {
      ARTElement ae = artPair.getFirst();
      PredicateAbstractElement e = extractElementByType(ae, PredicateAbstractElement.class);

      assert e instanceof AbstractionElement;

      Collection<AbstractionPredicate> newpreds = pInfo.getPredicatesForRefinement(e);
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
        if (regionManager.isFalse(f)) {
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
