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

public final class RestoreLockEffect extends LockEffect {

  private static final RestoreLockEffect instance = new RestoreLockEffect();

  private RestoreLockEffect(LockIdentifier id) {
    super(id);
  }

  private RestoreLockEffect() {
    this(null);
  }

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    Preconditions.checkArgument(target != null, "Lock identifier must be set");
    pBuilder.restore(target);
  }

  public static RestoreLockEffect getInstance() {
    return instance;
  }

  public static RestoreLockEffect createEffectForId(LockIdentifier id) {
    return new RestoreLockEffect(id);
  }

  @Override
  public RestoreLockEffect cloneWithTarget(LockIdentifier id) {
    return createEffectForId(id);
  }

  @Override
  public String getAction() {
    return "Restore";
  }
}
