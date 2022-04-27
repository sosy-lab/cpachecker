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

public final class ReleaseLockEffect extends LockEffect {

  private static final ReleaseLockEffect instance = new ReleaseLockEffect();

  private ReleaseLockEffect(LockIdentifier id) {
    super(id);
  }

  private ReleaseLockEffect() {
    this(null);
  }

  @Override
  public void effect(AbstractLockStateBuilder pBuilder) {
    Preconditions.checkArgument(target != null, "Lock identifier must be set");
    pBuilder.free(target);
  }

  public static ReleaseLockEffect getInstance() {
    return instance;
  }

  public static ReleaseLockEffect createEffectForId(LockIdentifier id) {
    return new ReleaseLockEffect(id);
  }

  @Override
  public ReleaseLockEffect cloneWithTarget(LockIdentifier id) {
    return createEffectForId(id);
  }

  @Override
  public String getAction() {
    return "Release";
  }
}
