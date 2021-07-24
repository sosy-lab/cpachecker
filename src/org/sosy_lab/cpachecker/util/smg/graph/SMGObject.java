// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;
import org.sosy_lab.common.UniqueIdGenerator;

public class SMGObject implements SMGNode, Comparable<SMGObject> {

  private static final SMGObject NULL_OBJECT =
      new SMGObject(0, BigInteger.ZERO, BigInteger.ZERO, false, 0);
  private static final UniqueIdGenerator U_ID_GENERATOR = new UniqueIdGenerator();

  private final int nestingLevel;
  private final BigInteger size;
  private final BigInteger offset;
  private boolean valid;
  // ID needed for comparable implementation.
  private final int id;

  protected SMGObject(int pNestingLevel, BigInteger pSize, BigInteger pOffset, boolean pValid) {
    nestingLevel = pNestingLevel;
    size = pSize;
    offset = pOffset;
    valid = pValid;
    id = U_ID_GENERATOR.getFreshId() + 1;
  }

  protected SMGObject(
      int pNestingLevel,
      BigInteger pSize,
      BigInteger pOffset,
      boolean pValid,
      int pId) {
    nestingLevel = pNestingLevel;
    size = pSize;
    offset = pOffset;
    valid = pValid;
    id = pId;
  }

  public static SMGObject nullInstance() {
    return NULL_OBJECT;
  }

  public static SMGObject
      of(int pNestingLevel, BigInteger pSize, BigInteger pOffset, boolean pValid) {
    return new SMGObject(pNestingLevel, pSize, pOffset, pValid);
  }

  public BigInteger getSize() {
    return size;
  }

  public BigInteger getOffset() {
    return offset;
  }

  public boolean isValid() {
    return valid;
  }

  public void invalidate() {
    valid = false;
  }

  @Override
  public int getNestingLevel() {
    return nestingLevel;
  }

  @Override
  public int compareTo(SMGObject pArg0) {
    return Integer.compare(id, pArg0.id);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof SMGObject)) {
      return false;
    }
    SMGObject otherObj = (SMGObject) other;
    return id == otherObj.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  public boolean isZero() {
    return equals(NULL_OBJECT);
  }

}
