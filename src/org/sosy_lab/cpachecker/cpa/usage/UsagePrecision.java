// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;
import org.sosy_lab.cpachecker.util.identifiers.GeneralIdentifier;

public final class UsagePrecision implements WrapperPrecision, AdjustablePrecision {
  private final Map<CFANode, Map<GeneralIdentifier, DataType>> localStatistics;
  private final Precision wrappedPrecision;

  private UsagePrecision(
      Precision pWrappedPrecision, Map<CFANode, Map<GeneralIdentifier, DataType>> pMap) {
    localStatistics = pMap;
    wrappedPrecision = pWrappedPrecision;
  }

  static UsagePrecision create(
      Precision pWrappedPrecision, Map<CFANode, Map<GeneralIdentifier, DataType>> pMap) {
    return new UsagePrecision(pWrappedPrecision, ImmutableMap.copyOf(pMap));
  }

  Map<GeneralIdentifier, DataType> get(CFANode node) {
    return localStatistics.get(node);
  }

  public Precision getWrappedPrecision() {
    return wrappedPrecision;
  }

  public UsagePrecision copy(Precision pWrappedPrecision) {
    return new UsagePrecision(pWrappedPrecision, localStatistics);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(localStatistics);
    result = prime * result + Objects.hashCode(wrappedPrecision);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    UsagePrecision other = (UsagePrecision) obj;
    return Objects.equals(localStatistics, other.localStatistics)
        && Objects.equals(wrappedPrecision, other.wrappedPrecision);
  }

  @Override
  public String toString() {
    return "Size = " + localStatistics.size() + ";" + wrappedPrecision;
  }

  public int getTotalRecords() {
    int sum = 0;
    for (Map<GeneralIdentifier, DataType> val : localStatistics.values()) {
      sum += val.size();
    }
    return sum;
  }

  @Override
  public <T extends Precision> T retrieveWrappedPrecision(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(wrappedPrecision.getClass())) {
      return pType.cast(wrappedPrecision);
    } else if (wrappedPrecision instanceof WrapperPrecision) {
      return ((WrapperPrecision) wrappedPrecision).retrieveWrappedPrecision(pType);
    } else {
      return null;
    }
  }

  @Override
  public Precision replaceWrappedPrecision(
      Precision pNewPrecision, Predicate<? super Precision> pReplaceType) {
    if (pReplaceType.apply(this)) {
      return pNewPrecision;
    } else if (pReplaceType.apply(wrappedPrecision)) {
      return copy(pNewPrecision);
    } else if (wrappedPrecision instanceof WrapperPrecision) {
      return copy(
          ((WrapperPrecision) wrappedPrecision)
              .replaceWrappedPrecision(pNewPrecision, pReplaceType));

    } else {
      return null;
    }
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return Collections.singleton(wrappedPrecision);
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision pOtherPrecision) {
    return adjust(pOtherPrecision, (a, b) -> a.add(b));
  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision pOtherPrecision) {
    return adjust(pOtherPrecision, (a, b) -> a.subtract(b));
  }

  private AdjustablePrecision adjust(
      AdjustablePrecision pOtherPrecision,
      BiFunction<AdjustablePrecision, AdjustablePrecision, AdjustablePrecision> adjustFunction) {

    AdjustablePrecision thisWrappedPrecision = (AdjustablePrecision) wrappedPrecision;
    AdjustablePrecision wrappedOtherPrecision;
    if (pOtherPrecision instanceof UsagePrecision) {
      UsagePrecision otherPrecision = (UsagePrecision) pOtherPrecision;
      wrappedOtherPrecision = (AdjustablePrecision) otherPrecision.wrappedPrecision;
      // The precision is not modified
      assert localStatistics.equals(otherPrecision.localStatistics);
    } else {
      wrappedOtherPrecision = pOtherPrecision;
    }
    AdjustablePrecision newWrappedPrecision =
        adjustFunction.apply(thisWrappedPrecision, wrappedOtherPrecision);

    return copy(newWrappedPrecision);
  }

  @Override
  public boolean isEmpty() {
    // local statistics map is not relevant to comparison as it is not modified and is the same in
    // other instances
    return ((AdjustablePrecision) wrappedPrecision).isEmpty();
  }
}
