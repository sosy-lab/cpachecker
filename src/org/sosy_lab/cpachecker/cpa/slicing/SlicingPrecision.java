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
import java.util.HashSet;
import java.util.Objects;
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

  /**
   * Returns a new {@link SlicingPrecision} with the given wrapped precision and the relevant edges
   * of this slicing precision.
   */
  public SlicingPrecision getNew(final Precision pWrappedPrec) {
    return new SlicingPrecision(pWrappedPrec, relevantEdges);
  }

  /**
   * Returns a new {@link SlicingPrecision} with the given wrapped precision and the relevant edges
   * of this slicing precision + the given relevant edges.
   */
  public SlicingPrecision getNew(final Precision pWrappedPrec, final Set<CFAEdge> pIncrement) {
    Set<CFAEdge> newRelevantEdges = new HashSet<>(relevantEdges);
    newRelevantEdges.addAll(pIncrement);
    return new SlicingPrecision(pWrappedPrec, newRelevantEdges);
  }

  /** Returns whether the given {@link CFAEdge} is a relevant edge. */
  public boolean isRelevant(final CFAEdge pEdge) {
    return relevantEdges.contains(pEdge);
  }

  public Set<CFAEdge> getRelevant() {
    return relevantEdges;
  }

  /** Returns the wrapped precision. */
  public Precision getWrappedPrec() {
    return wrappedPrec;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Precision> T retrieveWrappedPrecision(final Class<T> pType) {
    if (wrappedPrec.getClass().equals(pType)) {
      return (T) wrappedPrec;
    } else if (wrappedPrec instanceof WrapperPrecision) {
      return ((WrapperPrecision) wrappedPrec).retrieveWrappedPrecision(pType);
    } else {
      return null;
    }
  }

  @Override
  public Precision replaceWrappedPrecision(
      final Precision pNewPrecision, Predicate<? super Precision> pReplaceType) {

    Precision newPrecision = null;
    if (pReplaceType.apply(wrappedPrec)) {
      newPrecision = pNewPrecision;
    } else if (wrappedPrec instanceof WrapperPrecision) {
      newPrecision =
          ((WrapperPrecision) wrappedPrec).replaceWrappedPrecision(pNewPrecision, pReplaceType);
    }

    if (newPrecision == null) {
      return null;
    } else {
      return new SlicingPrecision(newPrecision, relevantEdges);
    }
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return Collections.singleton(wrappedPrec);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    SlicingPrecision that = (SlicingPrecision) pO;
    return Objects.equals(wrappedPrec, that.wrappedPrec)
        && Objects.equals(relevantEdges, that.relevantEdges);
  }

  @Override
  public int hashCode() {
    return Objects.hash(wrappedPrec, relevantEdges);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "\n\twrapped precisions: "
        + wrappedPrec
        + ",\n\trelevant edges: "
        + relevantEdges
        + '}';
  }

  public static class FullPrecision extends SlicingPrecision {

    public FullPrecision(final Precision pWrappedPrec) {
      super(pWrappedPrec, ImmutableSet.of());
    }

    @Override
    public boolean isRelevant(final CFAEdge pEdge) {
      return true;
    }

    @Override
    public SlicingPrecision getNew(final Precision pWrappedPrec) {
      return new FullPrecision(pWrappedPrec);
    }

    @Override
    public SlicingPrecision getNew(final Precision pWrappedPrec, final Set<CFAEdge> pIncrement) {
      return getNew(pWrappedPrec);
    }
  }
}
