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

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

/**
 * Implementation of {@link PathFormulaManager} that delegates to another
 * instance but caches results of some methods.
 */
public class CachingPathFormulaManager implements PathFormulaManager {

  public final Timer pathFormulaComputationTimer = new Timer();
  public int pathFormulaCacheHits = 0;

  private final PathFormulaManager delegate;

  private final Map<Pair<PathFormula, CFAEdge>, PathFormula> pathFormulaCache
            = new HashMap<Pair<PathFormula, CFAEdge>, PathFormula>();

  private final Map<Pair<PathFormula, PathFormula>, PathFormula> mergeCache
            = new HashMap<Pair<PathFormula, PathFormula>, PathFormula>();

  private final Map<PathFormula, PathFormula> emptyFormulaCache
            = new HashMap<PathFormula, PathFormula>();

  private final PathFormula emptyFormula;

  public CachingPathFormulaManager(PathFormulaManager pDelegate) {
    delegate = pDelegate;
    emptyFormula = delegate.makeEmptyPathFormula();
  }

  @Override
  public PathFormula makeAnd(PathFormula pOldFormula, CFAEdge pEdge) throws CPATransferException {

    final Pair<PathFormula, CFAEdge> formulaCacheKey = Pair.of(pOldFormula, pEdge);
    PathFormula result = pathFormulaCache.get(formulaCacheKey);
    if (result == null) {
      pathFormulaComputationTimer.start();
      // compute new pathFormula with the operation on the edge
      result = delegate.makeAnd(pOldFormula, pEdge);
      pathFormulaComputationTimer.stop();
      pathFormulaCache.put(formulaCacheKey, result);

    } else {
      pathFormulaCacheHits++;
    }
    return result;
  }

  @Override
  public PathFormula makeOr(PathFormula pF1, PathFormula pF2) {
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
      pathFormulaCacheHits++;
    }
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
      pathFormulaCacheHits++;
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
  public PathFormula makeAnd(PathFormula pPathFormula,  PathFormula pShiftedEnvPathFormula) {
    return delegate.makeAnd(pPathFormula, pShiftedEnvPathFormula);
  }

  @Override
  public PathFormula shiftFormula(PathFormula pFormula, int pOffset) {
    return delegate.shiftFormula(pFormula, pOffset);
  }
}
