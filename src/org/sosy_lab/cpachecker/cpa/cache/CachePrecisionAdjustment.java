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

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.HashMap;
import java.util.Map;

/*
 * CAUTION: The cache for precision adjustment is only correct for CPAs that do
 * _NOT_ depend on the reached set when performing prec.
 */
public class CachePrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment mCachedPrecisionAdjustment;

  private final Map<Precision, Map<AbstractState, Optional<PrecisionAdjustmentResult>>> mCache;

  public CachePrecisionAdjustment(PrecisionAdjustment pCachedPrecisionAdjustment) {
    mCachedPrecisionAdjustment = pCachedPrecisionAdjustment;
    //mCache = new HashMap<AbstractState, Map<Precision, Triple<AbstractState, Precision, Action>>>();
    mCache = new HashMap<>();
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {

    Map<AbstractState, Optional<PrecisionAdjustmentResult>> lCache = mCache.get(pPrecision);

    if (lCache == null) {
      lCache = new HashMap<>();
      mCache.put(pPrecision, lCache);
    }

    Optional<PrecisionAdjustmentResult> lResult = lCache.get(pElement);

    if (lResult == null) {
      lResult = mCachedPrecisionAdjustment.prec(
              pElement, pPrecision, pElements, projection, fullState);
      lCache.put(pElement, lResult);
    }

    return lResult;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> postAdjustmentStrengthen(
      AbstractState result,
      Precision precision,
      Iterable<AbstractState> otherStates,
      Iterable<Precision> otherPrecisions,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState resultFullState) throws CPAException, InterruptedException {
    return Optional.of(PrecisionAdjustmentResult.create(result, precision, Action.CONTINUE));
  }

}
