// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * Base class for values that can be tracked by the ValueAnalysisCPA.
 *
 * <p>Traditionally, ValueAnalysisCPA would only keep track of long type values. For the future,
 * floats, symbolic values, and SMG nodes should also be supported.
 */
public interface Value extends Serializable {
  boolean isNumericValue();

  /** True if we have no idea about this value(can not track it), false otherwise. */
  boolean isUnknown();

  /** True if we deterministically know the actual value, false otherwise. */
  boolean isExplicitlyKnown();

  /**
   * Returns the NumericValue if the stored value can be explicitly represented by a numeric value,
   * null otherwise.
   */
  @Nullable NumericValue asNumericValue();

  /** Return the long value if this is a long value, null otherwise. * */
  @Nullable Long asLong(CType type);

  <T> T accept(ValueVisitor<T> pVisitor);

  /** Singleton class used to signal that the value is unknown (could be anything). * */
  public static final class UnknownValue implements Value, Serializable {

    private static final long serialVersionUID = -300842115868319184L;
    private static final UnknownValue instance = new UnknownValue();

    @Override
    public String toString() {
      return "UNKNOWN";
    }

    public static UnknownValue getInstance() {
      return instance;
    }

    @Override
    public boolean isNumericValue() {
      return false;
    }

    @Override
    public @Nullable NumericValue asNumericValue() {
      return null;
    }

    @Override
    public @Nullable Long asLong(CType type) {
      checkNotNull(type);
      return null;
    }

    @Override
    public <T> T accept(ValueVisitor<T> pVisitor) {
      return pVisitor.visit(this);
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public boolean isExplicitlyKnown() {
      return false;
    }

    Object readResolve() {
      return instance;
    }
  }
}
