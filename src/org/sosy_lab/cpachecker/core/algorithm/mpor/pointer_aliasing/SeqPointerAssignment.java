// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_checks.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * An assignment of the memory location {@code rightHandSideMemoryLocation} to the pointer {@code
 * leftHandSideMemoryLocation}.
 *
 * <p>This class is not a record so that the constructor can reject unsupported input programs (via
 * {@link UnsupportedCodeException}), and records do not allow checked exceptions in the method
 * called after construction.
 */
public final class SeqPointerAssignment {

  private final MPORThread thread;

  private final SeqPointerAssignmentType type;

  private final SeqMemoryLocation leftHandSideMemoryLocation;

  private final SeqMemoryLocation rightHandSideMemoryLocation;

  SeqPointerAssignment(
      MPORThread pThread,
      SeqPointerAssignmentType pType,
      SeqMemoryLocation pLeftHandSideMemoryLocation,
      SeqMemoryLocation pRightHandSideMemoryLocation)
      throws UnsupportedCodeException {

    InputRejection.checkPointerAssignmentLeftHandSide(pLeftHandSideMemoryLocation);
    thread = pThread;
    type = pType;
    leftHandSideMemoryLocation = pLeftHandSideMemoryLocation;
    rightHandSideMemoryLocation = pRightHandSideMemoryLocation;
  }

  public MPORThread getThread() {
    return thread;
  }

  public SeqPointerAssignmentType type() {
    return type;
  }

  public SeqMemoryLocation leftHandSideMemoryLocation() {
    return leftHandSideMemoryLocation;
  }

  public SeqMemoryLocation rightHandSideMemoryLocation() {
    return rightHandSideMemoryLocation;
  }

  public ImmutableSet<SeqMemoryLocation> getAllMemoryLocations() {
    return ImmutableSet.of(leftHandSideMemoryLocation, rightHandSideMemoryLocation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, leftHandSideMemoryLocation, rightHandSideMemoryLocation);
  }

  @Override
  public boolean equals(Object pOther) {
    return pOther instanceof SeqPointerAssignment other
        && type.equals(other.type)
        && leftHandSideMemoryLocation.equals(other.leftHandSideMemoryLocation)
        && rightHandSideMemoryLocation.equals(other.rightHandSideMemoryLocation);
  }

  @Override
  public String toString() {
    return String.format(
        "SeqPointerAssignment[type=%s, leftHandSide=%s, rightHandSide=%s]",
        type, leftHandSideMemoryLocation, rightHandSideMemoryLocation);
  }
}
