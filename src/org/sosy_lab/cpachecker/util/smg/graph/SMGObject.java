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

  private static final UniqueIdGenerator U_ID_GENERATOR = new UniqueIdGenerator();
  private static final SMGObject NULL_OBJECT = new SMGObject(0, BigInteger.ZERO, BigInteger.ZERO);

  private int nestingLevel;
  private final BigInteger size;
  private final BigInteger offset;
  // ID needed for comparable implementation.
  private final int id;

  protected SMGObject(int pNestingLevel, BigInteger pSize, BigInteger pOffset) {
    nestingLevel = pNestingLevel;
    size = pSize;
    offset = pOffset;
    id = U_ID_GENERATOR.getFreshId();
  }

  protected SMGObject(int pNestingLevel, BigInteger pSize, BigInteger pOffset, int pId) {
    nestingLevel = pNestingLevel;
    size = pSize;
    offset = pOffset;
    id = pId;
  }

  public static SMGObject nullInstance() {
    return NULL_OBJECT;
  }

  public static SMGObject of(int pNestingLevel, BigInteger pSize, BigInteger pOffset) {
    return new SMGObject(pNestingLevel, pSize, pOffset);
  }

  public BigInteger getSize() {
    return size;
  }

  public BigInteger getOffset() {
    return offset;
  }

  @Override
  public int getNestingLevel() {
    return nestingLevel;
  }

  @Override
  public int compareTo(SMGObject pOther) {
    return Integer.compare(id, pOther.id);
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

  public SMGObject copyWithNewLevel(int pNewLevel) {
    return of(pNewLevel, size, offset);
  }

  public SMGObject freshCopy() {
    return of(nestingLevel, size, offset);
  }

  @Override
  public void increaseLevelBy(int pByX) {
    nestingLevel += pByX;
  }
}
