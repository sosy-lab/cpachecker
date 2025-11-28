// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import java.util.OptionalLong;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * Base class for values that can be tracked by the ValueAnalysisCPA.
 *
 * <p>Traditionally, ValueAnalysisCPA would only keep track of long type values. For the future,
 * floats, symbolic values, and SMG nodes should also be supported.
 */
public interface Value extends Serializable {

  /**
   * Returns true for values who's numeric value is concretely known, i.e. of the type {@link
   * NumericValue}. It is safe to interpret them (and only them!) as numeric values using {@link
   * #asNumericValue()}. False else. (Note: {@link JNullValue}, as well as others are Java types and
   * do not return a numeric value!)
   */
  default boolean isNumericValue() {
    return false;
  }

  /**
   * True if we do not have any information about the value, and we do not track it currently (as
   * tracked unknown values are {@link
   * org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue}s), false otherwise. Values that
   * return true are always of the {@link UnknownValue} type!
   */
  default boolean isUnknown() {
    return false;
  }

  /**
   * True if we deterministically know the actual value, false otherwise. Explicitly known are (at
   * least) the following types: {@link NumericValue}s, {@link JArrayValue}s, {@link
   * JBooleanValue}s, {@link JEnumConstantValue}s, and {@link JNullValue}s.
   */
  default boolean isExplicitlyKnown() {
    return false;
  }

  /**
   * Returns the NumericValue if the stored value can be explicitly represented by a {@link
   * NumericValue}, empty otherwise.
   */
  default Optional<NumericValue> asNumericValue() {
    // TODO: this should be COMPLETELY removed (together with isNumericValue())! And replaced by
    //  instanceof checks!
    return Optional.empty();
  }

  /** Return the long value if this is a long value. */
  default OptionalLong asLong(CType type) {
    checkNotNull(type);
    return OptionalLong.empty();
  }

  <T> T accept(ValueVisitor<T> pVisitor);

  /** Singleton class used to signal that the value is unknown (could be anything). */
  final class UnknownValue implements Value {

    @Serial private static final long serialVersionUID = -300842115868319184L;
    private static final UnknownValue instance = new UnknownValue();

    @Override
    public String toString() {
      return "UNKNOWN";
    }

    public static UnknownValue getInstance() {
      return instance;
    }

    @Override
    public <T> T accept(ValueVisitor<T> pVisitor) {
      return pVisitor.visit(this);
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    // Used as part of the deserialization (usage might not appear in some IDEs/tools) in
    // proof-carrying code for example.
    @Serial
    private Object readResolve() {
      return instance;
    }
  }
}
