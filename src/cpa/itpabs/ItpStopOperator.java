/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.itpabs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.interfaces.TheoremProver;

import cmdline.CPAMain;

import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

/**
 * Coverage check for interpolation-based lazy abstraction
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN 
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpStopOperator implements StopOperator {

  private final ItpAbstractDomain domain;
  private final TheoremProver thmProver;
  // cache for checking entailement. Can be disabled
  private final boolean entailsUseCache;
  private Map<Pair<SymbolicFormula, SymbolicFormula>, Boolean> entailsCache;

  // statistics
  public long coverageCheckTime;
  public int numCoveredStates;
  public int numCoverageChecks;
  public int numCachedCoverageChecks;

  public ItpStopOperator(ItpAbstractDomain d, TheoremProver prover) {
    domain = d;
    coverageCheckTime = 0;
    numCoveredStates = 0;
    numCoverageChecks = 0;
    numCachedCoverageChecks = 0;
    thmProver = prover;

    entailsUseCache = CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.mathsat.useCache");
    if (entailsUseCache) {
      entailsCache =
        new HashMap<Pair<SymbolicFormula, SymbolicFormula>, Boolean>();
    }
  }

  public <AE extends AbstractElement> boolean stop(AE element,
                                                   Collection<AE> reached, Precision prec) throws CPAException {
    ItpCPA cpa = domain.getCPA();
    ItpAbstractElement ie = (ItpAbstractElement)element;
    if (cpa.isCovered(ie)) {
      return true;
    }
    for (AbstractElement e : reached) {
      if (stop(element, e)) {
        return true;
      }
    }
    return false;
  }

  public boolean stop(AbstractElement element, AbstractElement reachedElement)
  throws CPAException {
    long start = System.currentTimeMillis();
    boolean res = stopPriv(element, reachedElement);
    long end = System.currentTimeMillis();
    coverageCheckTime += end - start;
    return res;
  }

  public boolean stopPriv(AbstractElement element,
                          AbstractElement reachedElement)
  throws CPAException {

    ItpAbstractElement e1 = (ItpAbstractElement)element;
    ItpAbstractElement e2 =
      (ItpAbstractElement)reachedElement;

    if (e1.getLocation().equals(e2.getLocation()) &&
        e2.getId() < e1.getId() && !e2.isCovered()) {
      CPAMain.logManager.log(Level.ALL, "DEBUG_1",
          "Checking Coverage of element: ", element);

      if (!e1.sameContext(e2)) {
        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
            "NO, not covered: context differs");
        return false;
      }

      assert(e1.getAbstraction() != null);
      assert(e2.getAbstraction() != null);

      ItpCPA cpa = domain.getCPA();

      ++numCoverageChecks;
      SymbolicFormulaManager mgr = cpa.getFormulaManager();
      int res = -1;
      boolean ok;

      Pair<SymbolicFormula, SymbolicFormula> key = null;
      if (entailsUseCache) {
        key = new Pair<SymbolicFormula, SymbolicFormula>(
            e1.getAbstraction(), e2.getAbstraction());
        if (entailsCache.containsKey(key)) {
          res = entailsCache.get(key) ? 1 : 0;
          ++numCachedCoverageChecks;
        }
      }
      if (res != -1) {
        ok = (res == 1);
      } else {
        ok = mgr.entails(e1.getAbstraction(), e2.getAbstraction(),
            thmProver);
        if (entailsUseCache) {
          assert(key != null);
          entailsCache.put(key, ok);
        }
      }

      if (ok) {
        ++numCoveredStates;
        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
            "Element: ", e1, " COVERED by: ", e2);
        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
            "Abstraction for ", e1, ": ", e1.getAbstraction());
        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
            "Abstraction for ", e2, ": ", e2.getAbstraction());
        cpa.setCoveredBy(e1, e2);
        ItpTransferRelation trans =
          (ItpTransferRelation)cpa.getTransferRelation();
        trans.addToProcess(cpa.removeDescendantsFromCovering(e1));
      } else {
        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
            "NO, not covered");
      }

      return ok;
    } else {
      return false;
    }
  }

}
