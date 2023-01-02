// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.BiFunction;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;

class CompositePrecision implements WrapperPrecision, AdjustablePrecision {

  private final ImmutableList<Precision> precisions;

  CompositePrecision(List<Precision> precisions) {
    this.precisions = ImmutableList.copyOf(precisions);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    } else if (!(other instanceof CompositePrecision)) {
      return false;
    }

    return precisions.equals(((CompositePrecision) other).precisions);
  }

  @Override
  public int hashCode() {
    return precisions.hashCode();
  }

  Precision get(int idx) {
    return precisions.get(idx);
  }

  @Override
  public String toString() {
    return precisions.toString();
  }

  @Override
  public <T extends Precision> T retrieveWrappedPrecision(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    for (Precision precision : precisions) {
      if (pType.isAssignableFrom(precision.getClass())) {
        return pType.cast(precision);

      } else if (precision instanceof WrapperPrecision) {
        T result = ((WrapperPrecision) precision).retrieveWrappedPrecision(pType);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  @Override
  public Precision replaceWrappedPrecision(
      Precision newPrecision, Predicate<? super Precision> replaceType) {

    if (replaceType.apply(this)) {
      return newPrecision;
    }

    ImmutableList.Builder<Precision> newPrecisions = ImmutableList.builder();
    boolean changed = false;
    for (Precision precision : precisions) {
      if (replaceType.apply(precision)) {
        newPrecisions.add(newPrecision);
        changed = true;

      } else if (precision instanceof WrapperPrecision) {
        Precision newWrappedPrecision =
            ((WrapperPrecision) precision).replaceWrappedPrecision(newPrecision, replaceType);
        if (newWrappedPrecision != null) {
          newPrecisions.add(newWrappedPrecision);
          changed = true;

        } else {
          newPrecisions.add(precision);
        }
      } else {
        newPrecisions.add(precision);
      }
    }
    return changed ? new CompositePrecision(newPrecisions.build()) : null;
  }

  @Override
  public ImmutableList<Precision> getWrappedPrecisions() {
    return precisions;
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision pOtherPrecision) {
    return adjustPrecisionWith(pOtherPrecision, (a, b) -> a.add(b));
  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision pOtherPrecision) {
    return adjustPrecisionWith(pOtherPrecision, (a, b) -> a.subtract(b));
  }

  private AdjustablePrecision adjustPrecisionWith(
      AdjustablePrecision pOtherPrecision,
      BiFunction<AdjustablePrecision, AdjustablePrecision, AdjustablePrecision> adjustFunction) {

    ImmutableList.Builder<Precision> newPrecisions = ImmutableList.builder();

    for (int i = 0; i < precisions.size(); i++) {
      Precision currentPrecision = get(i);
      Precision adjustedPrecision;

      if (pOtherPrecision instanceof CompositePrecision) {
        CompositePrecision precisionToAdjust = (CompositePrecision) pOtherPrecision;
        adjustedPrecision = precisionToAdjust.get(i);
      } else if (pOtherPrecision.getClass() == currentPrecision.getClass()) {
        adjustedPrecision = pOtherPrecision;
      } else {
        newPrecisions.add(currentPrecision);
        continue;
      }

      checkArgument(
          currentPrecision instanceof AdjustablePrecision,
          "Precision %s does not support adjusting precision",
          currentPrecision);
      checkArgument(
          adjustedPrecision instanceof AdjustablePrecision,
          "Precision %s does not support adjusting precision",
          adjustedPrecision);

      Precision newPrecision =
          adjustFunction.apply(
              (AdjustablePrecision) currentPrecision, (AdjustablePrecision) adjustedPrecision);
      newPrecisions.add(newPrecision);
    }
    return new CompositePrecision(newPrecisions.build());
  }

  @Override
  public boolean isEmpty() {
    return from(precisions)
        .transform(p -> (AdjustablePrecision) p)
        .allMatch(AdjustablePrecision::isEmpty);
  }
}
