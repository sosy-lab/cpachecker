// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class AnnotationInfo {
  private final ImmutableSet<LockIdentifier> freeLocks;
  private final ImmutableSet<LockIdentifier> restoreLocks;
  private final ImmutableSet<LockIdentifier> resetLocks;
  private final ImmutableSet<LockIdentifier> captureLocks;

  public AnnotationInfo(
      Set<LockIdentifier> free,
      Set<LockIdentifier> restore,
      Set<LockIdentifier> reset,
      Set<LockIdentifier> capture) {
    freeLocks = ImmutableSet.copyOf(free);
    restoreLocks = ImmutableSet.copyOf(restore);
    resetLocks = ImmutableSet.copyOf(reset);
    captureLocks = ImmutableSet.copyOf(capture);
  }

  public ImmutableSet<LockIdentifier> getFreeLocks() {
    return freeLocks;
  }

  public ImmutableSet<LockIdentifier> getRestoreLocks() {
    return restoreLocks;
  }

  public ImmutableSet<LockIdentifier> getResetLocks() {
    return resetLocks;
  }

  public ImmutableSet<LockIdentifier> getCaptureLocks() {
    return captureLocks;
  }
}
