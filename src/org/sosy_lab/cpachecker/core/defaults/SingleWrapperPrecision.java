// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.BiFunction;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;

public class SingleWrapperPrecision implements WrapperPrecision, AdjustablePrecision, Serializable {

  private static final long serialVersionUID = 1L;

  private final Precision wrappedPrecision;

  public SingleWrapperPrecision(Precision pPrecision) {
    wrappedPrecision = checkNotNull(pPrecision);
  }

  Precision getWrappedPrecision() {
    return wrappedPrecision;
  }

  @Override
  public int hashCode() {
    return Objects.hash(wrappedPrecision);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SingleWrapperPrecision)) {
      return false;
    }
    SingleWrapperPrecision other = (SingleWrapperPrecision) obj;
    return Objects.equals(wrappedPrecision, other.wrappedPrecision);
  }

  @Override
  public String toString() {
    return wrappedPrecision.toString();
  }

  @Override
  public <T extends Precision> T retrieveWrappedPrecision(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    if (pType.isAssignableFrom(wrappedPrecision.getClass())) {
      return pType.cast(wrappedPrecision);

    } else if (wrappedPrecision instanceof WrapperPrecision) {
      T result = ((WrapperPrecision) wrappedPrecision).retrieveWrappedPrecision(pType);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public Precision replaceWrappedPrecision(
      Precision pNewPrecision, Predicate<? super Precision> pReplaceType) {

    if (pReplaceType.apply(this)) {
      return pNewPrecision;
    }

    if (pReplaceType.apply(wrappedPrecision)) {
      return new SingleWrapperPrecision(pNewPrecision);

    } else if (wrappedPrecision instanceof WrapperPrecision) {
      Precision newWrappedPrecision =
          ((WrapperPrecision) wrappedPrecision)
              .replaceWrappedPrecision(pNewPrecision, pReplaceType);
      if (newWrappedPrecision != null) {
        return new SingleWrapperPrecision(newWrappedPrecision);
      }
    }
    return null;
  }

  @Override
  public ImmutableList<Precision> getWrappedPrecisions() {
    return ImmutableList.of(wrappedPrecision);
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

    Precision adjustedPrecision = null;

    if (pOtherPrecision instanceof SingleWrapperPrecision) {
      adjustedPrecision = ((SingleWrapperPrecision) pOtherPrecision).getWrappedPrecision();
    } else if (pOtherPrecision.getClass() == wrappedPrecision.getClass()) {
      adjustedPrecision = pOtherPrecision;
    } else {
      return new SingleWrapperPrecision(wrappedPrecision);
    }

    verify(
        wrappedPrecision instanceof AdjustablePrecision,
        "Precision %s does not support adjusting precision",
        wrappedPrecision);
    verify(
        adjustedPrecision instanceof AdjustablePrecision,
        "Precision %s does not support adjusting precision",
        adjustedPrecision);

    Precision newPrecision =
        adjustFunction.apply(
            (AdjustablePrecision) wrappedPrecision, (AdjustablePrecision) adjustedPrecision);

    return new SingleWrapperPrecision(newPrecision);
  }

  @Override
  public boolean isEmpty() {
    return ((AdjustablePrecision) wrappedPrecision).isEmpty();
  }
}
