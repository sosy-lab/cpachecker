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

public final class SetLockEffect extends LockEffect {

  private static final SetLockEffect instance = new SetLockEffect();

  final int p;

  private SetLockEffect(int t, LockIdentifier id) {
    super(id);
    p = t;
  }

  private SetLockEffect() {
    this(0, null);
  }

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    Preconditions.checkArgument(this != instance, "Temporary instance can not effect");
    Preconditions.checkArgument(target != null, "Lock identifier must be set");
    pBuilder.set(target, p);
  }

  public static SetLockEffect getInstance() {
    return instance;
  }

  public static SetLockEffect createEffectForId(int t, LockIdentifier id) {
    return new SetLockEffect(t, id);
  }

  @Override
  public SetLockEffect cloneWithTarget(LockIdentifier id) {
    return createEffectForId(p, id);
  }

  @Override
  public String getAction() {
    return "Set";
  }
}
