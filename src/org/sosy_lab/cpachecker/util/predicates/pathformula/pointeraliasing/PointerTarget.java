// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

record PointerTarget(
    PointerBase base, @Nullable CType containerType, long properOffset, long containerOffset)
    implements Serializable {

  /** This constructor is for fields of nested structures and arrays */
  PointerTarget {
    checkNotNull(base);
  }

  long offset() {
    return containerOffset + properOffset;
  }

  boolean isBase() {
    return containerType == null;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof PointerTarget o
        && properOffset == o.properOffset
        && containerOffset == o.containerOffset
        && base.equals(o.base)
        && (containerType != null
            ? o.containerType != null
                && containerType.getCanonicalType().equals(o.containerType.getCanonicalType())
            : o.containerType == null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(base, containerOffset, properOffset);
  }

  @Override
  public String toString() {
    return String.format(
        "(Base: %s, type: %s, prop. offset: %d, cont. offset: %d)",
        base, containerType, properOffset, containerOffset);
  }

  @Serial private static final long serialVersionUID = -1258065871533686442L;
}
