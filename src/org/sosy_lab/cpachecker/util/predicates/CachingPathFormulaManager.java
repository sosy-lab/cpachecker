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
package org.sosy_lab.cpachecker.util.predicates;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

/**
 * Implementation of {@link PathFormulaManager} that delegates to another
 * instance but caches results of some methods.
 */
public class CachingPathFormulaManager implements PathFormulaManager, StatisticsProvider {



  private final PathFormulaManager delegate;

  private final Map<Pair<PathFormula, CFAEdge>, PathFormula> pathFormulaCache
            = new HashMap<Pair<PathFormula, CFAEdge>, PathFormula>();

  private final Map<Pair<PathFormula, PathFormula>, PathFormula> mergeCache
            = new HashMap<Pair<PathFormula, PathFormula>, PathFormula>();

  private final Map<PathFormula, PathFormula> emptyFormulaCache
            = new HashMap<PathFormula, PathFormula>();

  private final Map<Pair<PathFormula, CFAEdge>, PathFormula> operationCache
          = new HashMap<Pair<PathFormula, CFAEdge>, PathFormula>();

  private final Map<Triple<Formula, SSAMap, SSAMap>, PathFormula> instantiateNextValueCache
          = new HashMap<Triple<Formula, SSAMap, SSAMap>, PathFormula>();

  private final PathFormula emptyFormula;

  public final Stats stats;
  private static CachingPathFormulaManager singleton;

  public static CachingPathFormulaManager getInstance(PathFormulaManager delegate){
    if (singleton == null){
      singleton = new CachingPathFormulaManager(delegate);
    }
    return singleton;
  }

  private CachingPathFormulaManager(PathFormulaManager pDelegate) {
    delegate = pDelegate;
    stats    = new Stats();
    emptyFormula = delegate.makeEmptyPathFormula();
  }

  @Override
  public PathFormula makeAnd(PathFormula pOldFormula, CFAEdge pEdge) throws CPATransferException {
    stats.makeAndTimer.start();
    final Pair<PathFormula, CFAEdge> formulaCacheKey = Pair.of(pOldFormula, pEdge);
    PathFormula result = pathFormulaCache.get(formulaCacheKey);
    if (result == null) {
      // compute new pathFormula with the operation on the edge
      result = delegate.makeAnd(pOldFormula, pEdge);
      pathFormulaCache.put(formulaCacheKey, result);

    } else {
      stats.makeAndCH++;
    }
    stats.makeAndTimer.stop();
    return result;
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, PathFormula pOtherFormula) {
    return delegate.makeAnd(pPathFormula, pOtherFormula);
  }

  @Override
  public PathFormula makeOr(PathFormula pF1, PathFormula pF2) {
    stats.makeOrTimer.start();
    final Pair<PathFormula, PathFormula> formulaCacheKey = Pair.of(pF1, pF2);

    PathFormula result = mergeCache.get(formulaCacheKey);
    if (result == null) {
      // try again with other order
      result = mergeCache.get(Pair.of(pF2, pF1));
    }

    if (result == null) {
      result = delegate.makeOr(pF1, pF2);
      mergeCache.put(formulaCacheKey, result);
    } else {
      stats.makeOrCH++;
    }

    stats.makeAndTimer.stop();
    return result;
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return emptyFormula;
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula pOldFormula) {
    PathFormula result = emptyFormulaCache.get(pOldFormula);
    if (result == null) {
      result = delegate.makeEmptyPathFormula(pOldFormula);
      emptyFormulaCache.put(pOldFormula, result);
    } else {
    }
    return result;
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, Formula pOtherFormula) {
    return delegate.makeAnd(pPathFormula, pOtherFormula);
  }

  @Override
  public PathFormula makeNewPathFormula(PathFormula pOldFormula, SSAMap pM) {
    return delegate.makeNewPathFormula(pOldFormula, pM);
  }

  @Override
  public PathFormula operationPathFormula(PathFormula pf, CFAEdge edge) throws CPATransferException {
    stats.operationPathFormulaTimer.start();
    Pair<PathFormula, CFAEdge> key = Pair.of(pf, edge);
    PathFormula result = operationCache.get(key);
    if (result == null){
      result = delegate.operationPathFormula(pf, edge);
      operationCache.put(key, result);
    } else {
      stats.operationPathFormulaCH++;
    }

    stats.operationPathFormulaTimer.stop();
    return result;
  }

  @Override
  public PathFormula makePrimedEqualities(SSAMap ssa1, int i, SSAMap ssa2, int j) {
    stats.makePrimedEqualitiesTimer.start();
    PathFormula result = delegate.makePrimedEqualities(ssa1, i, ssa2, j);
    stats.makePrimedEqualitiesTimer.stop();
    return result;
  }

  @Override
  public PathFormula changePrimedNo(PathFormula pf,Map<Integer, Integer> map) {
    // note: caching better for formula and ssa seperatly
    stats.changePrimedNoTimer.start();
    PathFormula result = delegate.changePrimedNo(pf, map);
    stats.changePrimedNoTimer.stop();
    return result;
  }

  @Override
  public PathFormula instantiateNextValue(Formula f, SSAMap low, SSAMap high) {
    stats.instantiateNextValueTimer.start();
    PathFormula result = delegate.instantiateNextValue(f, low, high);
    stats.instantiateNextValueTimer.stop();
    return result;
  }

  @Override
  public PathFormula makeFalsePathFormula() {
    return delegate.makeFalsePathFormula();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  public static class Stats implements Statistics{

    public final Timer pathFormulaComputationTimer = new Timer();
    public int pathFormulaCacheHits = 0;

    public final Timer makeAndTimer               = new Timer();
    public int   makeAndCH                  = 0;
    public final Timer makeOrTimer                = new Timer();
    public int   makeOrCH                   = 0;
    public final Timer operationPathFormulaTimer  = new Timer();
    public int   operationPathFormulaCH     = 0;
    public final Timer makePrimedEqualitiesTimer  = new Timer();
    public final Timer changePrimedNoTimer        = new Timer();
    public final Timer instantiateNextValueTimer  = new Timer();

    @Override
    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached) {
      long totalTimer = makeAndTimer.getSumTime() + makeOrTimer.getSumTime() + operationPathFormulaTimer.getSumTime()
      + makePrimedEqualitiesTimer.getSumTime() + changePrimedNoTimer.getSumTime()
      + instantiateNextValueTimer.getSumTime();

      out.println("makeAnd time:                    " + makeAndTimer);
      out.println("makeAnd cache hits:              " + formatInt(makeAndCH));
      out.println("makeOr time:                     " + makeOrTimer);
      out.println("makeOr cache hits:               " + formatInt(makeOrCH));
      out.println("operationPathFormula time:       " + operationPathFormulaTimer);
      out.println("operationPathFormula cache hits: " + formatInt(operationPathFormulaCH));
      out.println("makePrimedEqualities time:       " + makePrimedEqualitiesTimer  );
      out.println("changePrimedNo time:             " + changePrimedNoTimer );
      out.println("instantiateNextValue time:       " + instantiateNextValueTimer);
      out.println("total time on those:             " + Timer.formatTime(totalTimer));
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }

    @Override
    public String getName() {
      return "CachingPathFormulaManager";
    }

  }




}
