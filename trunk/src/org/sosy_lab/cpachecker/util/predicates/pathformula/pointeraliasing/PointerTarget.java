// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import java.io.Serializable;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

@javax.annotation.concurrent.Immutable // cannot prove deep immutability
public final class PointerTarget implements Serializable {

  /** This constructor is for fields of nested structures and arrays */
  PointerTarget(
      String base, @Nullable CType containerType, long properOffset, long containerOffset) {
    this.base = base;
    this.containerType = containerType;
    this.properOffset = properOffset;
    this.containerOffset = containerOffset;
  }

  String getBase() {
    return base;
  }

  public String getBaseName() {
    return PointerTargetSet.getBaseName(base);
  }

  public long getOffset() {
    return containerOffset + properOffset;
  }

  long getProperOffset() {
    assert containerType != null : "The target's offset is ill-defined";
    return properOffset;
  }

  boolean isBase() {
    return containerType == null;
  }

  @Nullable CType getContainerType() {
    return containerType;
  }

  long getContainerOffset() {
    assert containerType != null : "The target's container offset is ill-defined";
    return containerOffset;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof PointerTarget)) {
      return false;
    } else {
      final PointerTarget o = (PointerTarget) other;
      return properOffset == o.properOffset
          && containerOffset == o.containerOffset
          && base.equals(o.base)
          && (containerType != null
              ? o.containerType != null
                  && containerType.getCanonicalType().equals(o.containerType.getCanonicalType())
              : o.containerType == null);
    }
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

  final String base;
  final @Nullable CType containerType;
  final long properOffset;
  final long containerOffset;

  private static final long serialVersionUID = -1258065871533686442L;
}
