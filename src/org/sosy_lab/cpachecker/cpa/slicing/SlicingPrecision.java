/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.slicing;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;

/**
 * Precision for the {@link SlicingCPA}.
 * Contains the precision of the CPA wrapped by the SlicingCPA
 * as well a set of relevant CFA edges.
 * This set contains all CFA edges whose semantics should be considered
 * by the analysis.
 */
public class SlicingPrecision implements WrapperPrecision  {

  private final Precision wrappedPrec;
  private final Set<CFAEdge> relevantEdges;

  public SlicingPrecision(final Precision pWrappedPrec, final Set<CFAEdge> pRelevantEdges) {
    wrappedPrec = pWrappedPrec;
    relevantEdges = pRelevantEdges;
  }

  public boolean isRelevant(final CFAEdge pEdge) {
    return relevantEdges.contains(pEdge);
  }

  public Precision getWrappedPrec() {
    return wrappedPrec;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Precision> T retrieveWrappedPrecision(final Class<T> pType) {
    if (wrappedPrec.getClass().equals(pType)) {
      return (T) wrappedPrec;
    } else {
      return null;
    }
  }

  @Override
  public Precision replaceWrappedPrecision(
      final Precision pNewPrecision, Predicate<? super Precision> pReplaceType) {
    if (pReplaceType.apply(wrappedPrec)) {
      return new SlicingPrecision(pNewPrecision, relevantEdges);
    } else {
      return null;
    }
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return Collections.singleton(wrappedPrec);
  }

  public ImmutableSet<CFAEdge> getRelevantEdges() {
    return ImmutableSet.copyOf(relevantEdges);
  }
}
