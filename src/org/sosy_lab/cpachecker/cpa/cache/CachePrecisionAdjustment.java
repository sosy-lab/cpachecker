/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cache;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/*
 * CAUTION: The cache for precision adjustment is only correct for CPAs that do
 * _NOT_ depend on the reached set when performing prec.
 */
public class CachePrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment mCachedPrecisionAdjustment;
  //private final Map<AbstractState, Map<Precision, Triple<AbstractState, Precision, Action>>> mCache;
  private final Map<Precision, Map<AbstractState, PrecisionAdjustmentResult>> mCache;

  public CachePrecisionAdjustment(PrecisionAdjustment pCachedPrecisionAdjustment) {
    mCachedPrecisionAdjustment = pCachedPrecisionAdjustment;
    //mCache = new HashMap<AbstractState, Map<Precision, Triple<AbstractState, Precision, Action>>>();
    mCache = new HashMap<>();
  }

  @Override
  public PrecisionAdjustmentResult prec(
      AbstractState pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements) throws CPAException, InterruptedException {
    /*Map<Precision, Triple<AbstractState, Precision, Action>> lCache = mCache.get(pElement);

    if (lCache == null) {
      lCache = new HashMap<>();
      mCache.put(pElement, lCache);
    }

    Triple<AbstractState, Precision, Action> lResult = lCache.get(pPrecision);

    if (lResult == null) {
      lResult = mCachedPrecisionAdjustment.prec(pElement, pPrecision, pElements);
      lCache.put(pPrecision, lResult);
    }

    return lResult;*/

    Map<AbstractState, PrecisionAdjustmentResult> lCache = mCache.get(pPrecision);

    if (lCache == null) {
      lCache = new HashMap<>();
      mCache.put(pPrecision, lCache);
    }

    PrecisionAdjustmentResult lResult = lCache.get(pElement);

    if (lResult == null) {
      lResult = mCachedPrecisionAdjustment.prec(pElement, pPrecision, pElements);
      lCache.put(pElement, lResult);
    }

    return lResult;
  }

}
