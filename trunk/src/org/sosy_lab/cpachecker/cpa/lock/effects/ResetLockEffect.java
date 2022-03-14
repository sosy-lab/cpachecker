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

public final class ResetLockEffect extends LockEffect {

  private static final ResetLockEffect instance = new ResetLockEffect();

  private ResetLockEffect(LockIdentifier id) {
    super(id);
  }

  private ResetLockEffect() {
    this(null);
  }

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    Preconditions.checkArgument(target != null, "Lock identifier must be set");
    pBuilder.reset(target);
  }

  public static ResetLockEffect getInstance() {
    return instance;
  }

  public static ResetLockEffect createEffectForId(LockIdentifier id) {
    return new ResetLockEffect(id);
  }

  @Override
  public ResetLockEffect cloneWithTarget(LockIdentifier id) {
    return createEffectForId(id);
  }

  @Override
  public String getAction() {
    return "Reset";
  }
}
