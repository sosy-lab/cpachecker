// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.base.Objects;

public class RWLockRelease extends LockRelease {

  private final boolean isWriteRelease;

  RWLockRelease(String pLockId, String pThreadId, int pAccessEpoch, boolean pIsWriteRelease) {
    super(pLockId, pThreadId, pAccessEpoch);
    isWriteRelease = pIsWriteRelease;
  }

  public boolean isWriteRelease() {
    return isWriteRelease;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof RWLockRelease)) {
      return false;
    }
    if (!super.equals(pO)) {
      return false;
    }
    RWLockRelease that = (RWLockRelease) pO;
    return isWriteRelease == that.isWriteRelease;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), isWriteRelease);
  }
}
