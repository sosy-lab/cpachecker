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
public class SMGValue implements SMGNode, Comparable<SMGValue> {

  private static final UniqueIdGenerator U_ID_GENERATOR = new UniqueIdGenerator();

  /** The static value 0 */
  private static final SMGValue ZERO_VALUE = new SMGValue(0);
  // Floats and doubles can have all bits as 0. We need to save them as different values though to
  // not lose track of their types
  private static final SMGValue ZERO_FLOAT_VALUE = new SMGValue(0);
  private static final SMGValue ZERO_DOUBLE_VALUE = new SMGValue(0);

  /* Unique id to idendify this value. This is better than hashCodes as it does not clash. */
  private final int id;

  /**
   * Values can be nested inside SMGs. Basicly a value in a list is nesting level 0, while a value
   * in a list that is in a list is level 1 etc.
   */
  private int nestingLevel;

  /**
   * Creates a new, symbolic SMGValue with the entered nesting level.
   *
   * @param pNestingLevel The nesting level of this value node.
   */
  protected SMGValue(int pNestingLevel) {
    nestingLevel = pNestingLevel;
    id = U_ID_GENERATOR.getFreshId();
  }

  private SMGValue(int pId, int pNestingLevel) {
    nestingLevel = pNestingLevel;
    id = pId;
  }

  public static SMGValue of(int pNestingLevel) {
    return new SMGValue(pNestingLevel);
  }

  public static SMGValue of() {
    return of(0);
  }

  @Override
  public int getNestingLevel() {
    return nestingLevel;
  }

  /**
   * @return The static SMGValue = 0.
   */
  public static SMGValue zeroValue() {
    return ZERO_VALUE;
  }

  /**
   * @return The static SMGValue = 0f
   */
  public static SMGValue zeroFloatValue() {
    return ZERO_FLOAT_VALUE;
  }

  /**
   * @return The static SMGValue = 0 as Double
   */
  public static SMGValue zeroDoubleValue() {
    return ZERO_DOUBLE_VALUE;
  }

  /**
   * @return True if this SMGValue is equal to 0.
   */
  public boolean isZero() {
    return equals(ZERO_VALUE) || equals(ZERO_FLOAT_VALUE) || equals(ZERO_DOUBLE_VALUE);
  }

  @Override
  public int compareTo(SMGValue pOther) {
    return Integer.compare(id, pOther.id);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (!(other instanceof SMGValue)) {
      return false;
    }
    SMGValue otherObj = (SMGValue) other;
    return id == otherObj.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public SMGValue withNestingLevelAndCopy(int newLevel) {
    return new SMGValue(id, newLevel);
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
