// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class AbstractionAwarePORPrecision implements PORPrecision, WrapperPrecision {

  private final Precision wrappedPrecision;

  AbstractionAwarePORPrecision(Precision pWrappedPrecision) {
    wrappedPrecision = pWrappedPrecision;
  }

  @Override
  public boolean hasInformationAboutVariable(MemoryLocation memoryLocation) {
    if (wrappedPrecision instanceof PredicatePrecision predicatePrecision) {
      if (!predicatePrecision.getLocalPredicates().isEmpty() ||
          !predicatePrecision.getFunctionPredicates().isEmpty() ||
          !predicatePrecision.getLocationInstancePredicates().isEmpty()) {
        return true;
      }

      var globalPredicates = predicatePrecision.getGlobalPredicates();
      for (var predicate : globalPredicates) {

      }

      return false;
    }
    return true;
  }

  @Override
  public @Nullable <T extends Precision> T retrieveWrappedPrecision(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }

    if (pType.isAssignableFrom(wrappedPrecision.getClass())) {
      return pType.cast(wrappedPrecision);
    }

    if (wrappedPrecision instanceof WrapperPrecision wrapperPrecision) {
      return wrapperPrecision.retrieveWrappedPrecision(pType);
    }

    return null;
  }

  @Override
  public @Nullable Precision replaceWrappedPrecision(
      Precision newPrecision,
      Predicate<? super Precision> replaceType) {
    if (replaceType.apply(this)) {
      return newPrecision;
    }

    if (replaceType.apply(wrappedPrecision)) {
      return new AbstractionAwarePORPrecision(newPrecision);
    }

    if (wrappedPrecision instanceof WrapperPrecision wrapperPrecision) {
      Precision newWrappedPrecision =
          wrapperPrecision.replaceWrappedPrecision(newPrecision, replaceType);
      if (newWrappedPrecision != null) {
        return new AbstractionAwarePORPrecision(newWrappedPrecision);
      }
    }
    return null;
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return ImmutableList.of(wrappedPrecision);
  }
}
