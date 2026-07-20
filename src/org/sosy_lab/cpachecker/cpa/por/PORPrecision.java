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
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

abstract class PORPrecision implements WrapperPrecision {

  private final Precision wrappedPrecision;

  PORPrecision(Precision pWrappedPrecision) {
    wrappedPrecision = pWrappedPrecision;
  }

  protected abstract PORPrecision withWrappedPrecision(Precision newWrappedPrecision);

  abstract boolean canIgnoreVariable(MemoryLocation memoryLocation);

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
      Precision newPrecision, Predicate<? super Precision> replaceType) {
    if (replaceType.apply(this)) {
      return newPrecision;
    }

    if (wrappedPrecision instanceof WrapperPrecision wrapperPrecision) {
      Precision newWrappedPrecision =
          wrapperPrecision.replaceWrappedPrecision(newPrecision, replaceType);
      if (newWrappedPrecision != null) {
        return withWrappedPrecision(newWrappedPrecision);
      }
    }
    return null;
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return ImmutableList.of(wrappedPrecision);
  }

  public Precision getWrappedPrecision() {
    return wrappedPrecision;
  }
}
