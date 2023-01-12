// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.cache;

import com.google.common.base.Function;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/*
 * CAUTION: The cache for precision adjustment is only correct for CPAs that do
 * _NOT_ depend on the reached set when performing prec.
 */
public class CachePrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment mCachedPrecisionAdjustment;

  private final Map<Precision, Map<AbstractState, Optional<PrecisionAdjustmentResult>>> mCache;

  public CachePrecisionAdjustment(PrecisionAdjustment pCachedPrecisionAdjustment) {
    mCachedPrecisionAdjustment = pCachedPrecisionAdjustment;
    // mCache = new HashMap<AbstractState, Map<Precision, Triple<AbstractState, Precision,
    // Action>>>();
    mCache = new HashMap<>();
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    Map<AbstractState, Optional<PrecisionAdjustmentResult>> lCache = mCache.get(pPrecision);

    if (lCache == null) {
      lCache = new HashMap<>();
      mCache.put(pPrecision, lCache);
    }

    Optional<PrecisionAdjustmentResult> lResult = lCache.get(pElement);

    if (lResult == null) {
      lResult =
          mCachedPrecisionAdjustment.prec(pElement, pPrecision, pElements, projection, fullState);
      lCache.put(pElement, lResult);
    }

    return lResult;
  }
}
