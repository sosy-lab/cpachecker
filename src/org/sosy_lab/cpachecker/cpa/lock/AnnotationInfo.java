/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
