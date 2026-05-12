// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;

/**
 * Simple flags are not enough, this class contains more about the nature of the error.
 *
 * <p>This class is immutable.
 */
public class SMGErrorInfo {

  enum Property {
    INVALID_READ,
    INVALID_WRITE,
    INVALID_FREE,
    INVALID_HEAP,
    CORRECTNESS_WITNESS_REJECTED,
    UNDEFINED_BEHAVIOR
  }

  private final boolean invalidWrite;
  private final boolean invalidRead;
  private final boolean invalidFree;
  private final boolean hasMemoryLeak;
  private final boolean rejectedCorrectnessWitness;
  private final String errorDescription;
  private final PersistentList<Object> invalidChain;
  private final PersistentList<Object> currentChain;

  private SMGErrorInfo(
      boolean pInvalidWrite,
      boolean pInvalidRead,
      boolean pInvalidFree,
      boolean pHasMemoryLeak,
      boolean pRejectedCorrectnessWitness,
      String pErrorDescription,
      PersistentList<Object> pInvalidChain,
      PersistentList<Object> pCurrentChain) {
    invalidWrite = pInvalidWrite;
    invalidRead = pInvalidRead;
    invalidFree = pInvalidFree;
    hasMemoryLeak = pHasMemoryLeak;
    errorDescription = pErrorDescription;
    invalidChain = pInvalidChain;
    currentChain = pCurrentChain;
    rejectedCorrectnessWitness = pRejectedCorrectnessWitness;
  }

  static SMGErrorInfo of() {
    return new SMGErrorInfo(
        false,
        false,
        false,
        false,
        false,
        "",
        PersistentLinkedList.of(),
        PersistentLinkedList.of());
  }

  SMGErrorInfo withErrorMessage(String pErrorDescription) {
    return new SMGErrorInfo(
        invalidWrite,
        invalidRead,
        invalidFree,
        hasMemoryLeak,
        rejectedCorrectnessWitness,
        pErrorDescription,
        invalidChain,
        currentChain);
  }

  static SMGErrorInfo ofRejectedCorrectnessWitness() {
    return new SMGErrorInfo(
        false, false, false, false, true, "", PersistentLinkedList.of(), PersistentLinkedList.of());
  }

  SMGErrorInfo withProperty(Property pProperty) {
    boolean pInvalidWrite = invalidWrite;
    boolean pInvalidRead = invalidRead;
    boolean pInvalidFree = invalidFree;
    boolean pHasLeaks = hasMemoryLeak;

    switch (pProperty) {
      case INVALID_WRITE -> pInvalidWrite = true;
      case INVALID_READ -> pInvalidRead = true;
      case INVALID_FREE -> pInvalidFree = true;
      case INVALID_HEAP -> pHasLeaks = true;
      default -> throw new AssertionError("Unknown property " + pProperty);
    }

    return new SMGErrorInfo(
        pInvalidWrite,
        pInvalidRead,
        pInvalidFree,
        pHasLeaks,
        false,
        errorDescription,
        invalidChain,
        currentChain);
  }

  boolean hasMemoryErrors() {
    return invalidWrite
        || invalidRead
        || invalidFree
        || hasMemoryLeak
        || rejectedCorrectnessWitness;
  }

  public SMGErrorInfo withInvalidObjects(Collection<?> pObjects) {
    return new SMGErrorInfo(
        invalidWrite,
        invalidRead,
        invalidFree,
        hasMemoryLeak,
        rejectedCorrectnessWitness,
        errorDescription,
        invalidChain.withAll(new ArrayList<>(pObjects)),
        currentChain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        invalidWrite,
        invalidRead,
        invalidFree,
        hasMemoryLeak,
        rejectedCorrectnessWitness,
        invalidChain,
        currentChain);
  }

  @Override
  public boolean equals(Object pOther) {
    return pOther instanceof SMGErrorInfo o
        && invalidWrite == o.invalidWrite
        && invalidRead == o.invalidRead
        && invalidFree == o.invalidFree
        && hasMemoryLeak == o.hasMemoryLeak
        && rejectedCorrectnessWitness == o.rejectedCorrectnessWitness
        && invalidChain.equals(o.invalidChain)
        && currentChain.equals(o.currentChain);
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder("ErrorInfo {");
    str.append(errorDescription.isEmpty() ? "<>" : errorDescription).append("; Errors: ");
    if (invalidWrite) {
      str.append("invalid write").append(", ");
    }
    if (invalidRead) {
      str.append("invalid read").append(", ");
    }
    if (isInvalidFree()) {
      str.append("invalid free").append(", ");
    }
    if (hasMemoryLeak()) {
      str.append("has memory leak").append(", ");
    }
    if (rejectedCorrectnessWitness) {
      str.append("correctness witness rejected").append(", ");
    }
    return str.append("}").toString();
  }

  String getErrorDescription() {
    return errorDescription;
  }

  boolean isInvalidWrite() {
    return invalidWrite;
  }

  boolean isInvalidRead() {
    return invalidRead;
  }

  boolean isInvalidFree() {
    return invalidFree;
  }

  boolean hasMemoryLeak() {
    return hasMemoryLeak;
  }

  boolean isRejectedCorrectnessWitness() {
    return rejectedCorrectnessWitness;
  }

  public Property getPropertyViolated() {
    if (invalidFree) {
      return Property.INVALID_FREE;
    } else if (invalidWrite) {
      return Property.INVALID_WRITE;
    } else if (invalidRead) {
      return Property.INVALID_READ;
    } else if (hasMemoryLeak) {
      return Property.INVALID_HEAP;
    } else if (rejectedCorrectnessWitness) {
      return Property.CORRECTNESS_WITNESS_REJECTED;
    }
    // Will not happen
    throw new RuntimeException("Undefined memory error");
  }

  public PersistentList<Object> getInvalidChain() {
    if (invalidChain == null) {
      return PersistentLinkedList.of();
    }
    return invalidChain;
  }

  @SuppressWarnings("unused")
  public PersistentList<Object> getCurrentChain() {
    if (currentChain == null) {
      return PersistentLinkedList.of();
    }
    return currentChain;
  }
}
