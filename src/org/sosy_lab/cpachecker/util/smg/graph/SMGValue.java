// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import org.sosy_lab.common.UniqueIdGenerator;

/**
 * SMGValues are symbolic, with the exception of 0. They are only compared, so we need to use the
 * same object for the same symbolic value!
 */
public class SMGValue implements Comparable<SMGValue> {

  private static final UniqueIdGenerator U_ID_GENERATOR = new UniqueIdGenerator();

  /** The static value 0 */
  private static final SMGValue ZERO_VALUE = new SMGValue(U_ID_GENERATOR.getFreshId());

  // Floats and doubles can have all bits as 0. We need to save them as different values though to
  // not lose track of their types
  private static final SMGValue ZERO_FLOAT_VALUE = new SMGValue(U_ID_GENERATOR.getFreshId());
  private static final SMGValue ZERO_DOUBLE_VALUE = new SMGValue(U_ID_GENERATOR.getFreshId());

  /* Unique id to identify this value. This is better than hashCodes as it does not clash. */
  private final int id;

  /** Creates a new, symbolic SMGValue with the entered nesting level. */
  protected SMGValue() {
    id = U_ID_GENERATOR.getFreshId();
  }

  private SMGValue(int pId) {
    id = pId;
  }

  public static SMGValue of() {
    return new SMGValue();
  }

  /** Returns the static SMGValue = 0. */
  public static SMGValue zeroValue() {
    return ZERO_VALUE;
  }

  /** Returns the static SMGValue = 0f. */
  public static SMGValue zeroFloatValue() {
    return ZERO_FLOAT_VALUE;
  }

  /** Returns the static SMGValue = 0 as Double. */
  public static SMGValue zeroDoubleValue() {
    return ZERO_DOUBLE_VALUE;
  }

  /** Returns true if this SMGValue is equal to 0. */
  public boolean isZero() {
    return equals(ZERO_VALUE) || equals(ZERO_FLOAT_VALUE) || equals(ZERO_DOUBLE_VALUE);
  }

  @Override
  public int compareTo(SMGValue pOther) {
    return Integer.compare(id, pOther.id);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof SMGValue otherObj && id == otherObj.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public String toString() {
    if (isZero()) {
      return "ZERO";
    } else {
      return "Value" + id;
    }
  }
}
