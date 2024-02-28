// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.Optional;
import org.sosy_lab.common.UniqueIdGenerator;

public class SMGObject implements SMGNode, Comparable<SMGObject> {

  // The id generator has to be first because it needs to be initialized first!
  private static final UniqueIdGenerator U_ID_GENERATOR = new UniqueIdGenerator();

  // Static 0 instance. Always present in the SMGs
  private static final SMGObject NULL_OBJECT = new SMGObject(0, BigInteger.ZERO, BigInteger.ZERO);

  private int nestingLevel;
  private final BigInteger size;
  private final BigInteger offset;
  // ID needed for comparable implementation.
  private final int id;

  private final boolean isConstBinaryString;

  // For statically named objects, e.g. stack variables
  private final Optional<String> name;

  protected SMGObject(int pNestingLevel, BigInteger pSize, BigInteger pOffset) {
    nestingLevel = pNestingLevel;
    size = pSize;
    offset = pOffset;
    id = U_ID_GENERATOR.getFreshId();
    isConstBinaryString = false;
    name = Optional.empty();
  }

  private SMGObject(int pNestingLevel, BigInteger pSize, BigInteger pOffset, String objectName) {
    nestingLevel = pNestingLevel;
    size = pSize;
    offset = pOffset;
    id = U_ID_GENERATOR.getFreshId();
    isConstBinaryString = false;
    name = Optional.ofNullable(objectName);
  }

  protected SMGObject(int pNestingLevel, BigInteger pSize, BigInteger pOffset, int pId) {
    nestingLevel = pNestingLevel;
    size = pSize;
    offset = pOffset;
    id = pId;
    isConstBinaryString = false;
    name = Optional.empty();
  }

  protected SMGObject(
      int pNestingLevel,
      BigInteger pSize,
      BigInteger pOffset,
      int pId,
      boolean pIsConstBinaryString,
      Optional<String> maybeObjectName) {
    nestingLevel = pNestingLevel;
    size = pSize;
    offset = pOffset;
    id = pId;
    isConstBinaryString = pIsConstBinaryString;
    name = maybeObjectName;
  }

  /** Returns the static 0 {@link SMGObject} instance. */
  public static SMGObject nullInstance() {
    return NULL_OBJECT;
  }

  public static SMGObject of(int pNestingLevel, BigInteger pSize, BigInteger pOffset) {
    return new SMGObject(pNestingLevel, pSize, pOffset);
  }

  public static SMGObject of(
      int pNestingLevel, BigInteger pSize, BigInteger pOffset, String objectName) {
    return new SMGObject(pNestingLevel, pSize, pOffset, objectName);
  }

  /**
   * Copies the object, but the new object has a new id. So size etc. will match, but never the ID!
   *
   * @param objectToCopy obj to copy.
   * @return a new object with the same size etc. as the old.
   */
  public static SMGObject of(SMGObject objectToCopy) {
    return new SMGObject(objectToCopy.nestingLevel, objectToCopy.size, objectToCopy.offset);
  }

  /**
   * True for Strings allocated by the binary ("some string" in the code) that does not count
   * towards memleaks. False else.
   *
   * @return True for Strings allocated by the binary ("some string" in the code) that does not
   *     count towards memleaks.
   */
  public boolean isConstStringMemory() {
    return isConstBinaryString;
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
    return other instanceof SMGObject otherObj && id == otherObj.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public String toString() {
    if (name.isEmpty()) {
      return "SMGObject" + id + "[" + offset + ", " + size + ")";
    } else {
      return name.orElseThrow() + "[" + offset + ", " + size + ")";
    }
  }

  /** Returns true if the checked {@link SMGObject} is the null instance. */
  public boolean isZero() {
    return equals(NULL_OBJECT);
  }

  public SMGObject copyWithNewLevel(int pNewLevel) {
    Preconditions.checkArgument(pNewLevel >= 0);
    return of(pNewLevel, size, offset);
  }

  public SMGObject copyAsConstStringInBinary() {
    return new SMGObject(nestingLevel, size, offset, id, true, name);
  }

  public SMGObject freshCopy() {
    return of(nestingLevel, size, offset);
  }

  public boolean isSLL() {
    return false;
  }

  @Override
  public SMGObject withNestingLevelAndCopy(int pNewLevel) {
    return new SMGObject(pNewLevel, size, offset);
  }
}
