// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock.effects;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.lock.AbstractLockStateBuilder;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;

public class AcquireLockEffect extends LockEffectWithId {
  private final static AcquireLockEffect instance = new AcquireLockEffect();

  private final int maxRecursiveCounter;
  private final boolean stopAfterLimit;

  private AcquireLockEffect(LockIdentifier id, int t, boolean stop) {
    super(id);
    maxRecursiveCounter = t;
    stopAfterLimit = stop;
  }

  private AcquireLockEffect(LockIdentifier id) {
    this(id, Integer.MAX_VALUE, true);
  }

  private AcquireLockEffect() {
    this(null);
  }

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    Preconditions.checkArgument(target != null, "Lock identifier must be set");
    int previousP = pBuilder.getOldState().getCounter(target);
    if (maxRecursiveCounter > previousP) {
      pBuilder.add(target);
    } else if (stopAfterLimit) {
      pBuilder.setAsFalseState();
    }
  }

  public static AcquireLockEffect getInstance() {
    return instance;
  }

  public static AcquireLockEffect createEffectForId(LockIdentifier id, int counter, boolean stop) {
    return new AcquireLockEffect(id, counter, stop);
  }

  public static AcquireLockEffect createEffectForId(LockIdentifier id) {
    return new AcquireLockEffect(id);
  }

  @Override
  public AcquireLockEffect applyToTarget(LockIdentifier id) {
    return createEffectForId(id, this.maxRecursiveCounter, this.stopAfterLimit);
  }

  @Override
  public String getAction() {
    return "Acquire";
  }
}
