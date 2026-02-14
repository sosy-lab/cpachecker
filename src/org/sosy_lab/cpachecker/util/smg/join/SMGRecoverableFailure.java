// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import java.util.Optional;

public class SMGRecoverableFailure {
  public enum SMGRecoverableFailureType {
    LEFT_LIST_LONGER,
    RIGHT_LIST_LONGER,
    DELAYED_MERGE
  }

  // TODO: also add pointer offset, as linux style nested lists might be early abortable as well
  // with it.
  private final Optional<Integer> pointerOffset;
  private final SMGRecoverableFailureType failureType;

  private SMGRecoverableFailure(SMGRecoverableFailureType recType) {
    failureType = recType;
    pointerOffset = Optional.empty();
  }

  private SMGRecoverableFailure(SMGRecoverableFailureType recType, int pPointerOffset) {
    pointerOffset = Optional.of(pPointerOffset);
    failureType = recType;
  }

  public boolean hasPointerOffset() {
    return pointerOffset.isPresent();
  }

  public int getPointerOffset() {
    return pointerOffset.orElseThrow();
  }

  public SMGRecoverableFailureType getFailureType() {
    return failureType;
  }

  public static SMGRecoverableFailure leftListLonger() {
    return new SMGRecoverableFailure(SMGRecoverableFailureType.LEFT_LIST_LONGER);
  }

  public static SMGRecoverableFailure rightListLonger() {
    return new SMGRecoverableFailure(SMGRecoverableFailureType.RIGHT_LIST_LONGER);
  }

  public static SMGRecoverableFailure delayedMerge() {
    return new SMGRecoverableFailure(SMGRecoverableFailureType.DELAYED_MERGE);
  }

  public SMGRecoverableFailure copyWithOffset(int offset) {
    return new SMGRecoverableFailure(failureType, offset);
  }

  @Override
  public String toString() {
    return failureType.name()
        + (hasPointerOffset() ? " (source offset: " + getPointerOffset() + ")" : "");
  }
}
